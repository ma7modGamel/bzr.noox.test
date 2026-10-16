package noox.bzr.network

import android.util.Log
import android.os.Build
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import noox.bzr.gallery.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.LoggingEventListener
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * The only path from the app to the API: one OkHttp client behind Retrofit.
 *
 * Debug builds log every request and response body under the `BremoHttp` Logcat tag, plus
 * the connection steps (DNS answer, IP, TLS, connect failure) under `BremoNet`. Android Studio's
 * Network Inspector reads the same OkHttp traffic. Release builds log failures only, never bodies.
 */
object BremoHttp {
    const val TAG = "BremoHttp"
    private const val NET_TAG = "BremoNet"

    /** callStart prints the request with its headers; the token never reaches Logcat. */
    private val BEARER = Regex("Authorization[:=]\\s*Bearer [^,\\]}]+")

    @Volatile
    var language: () -> String = { AppLanguage.resolve(listOf(java.util.Locale.getDefault().toLanguageTag())) }

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val requestLanguage = language()
                val response = chain.proceed(chain.request().newBuilder()
                    .header("X-App-Platform", "Android")
                    .header("X-App-Version", BuildConfig.VERSION_NAME)
                    .header("X-App-Build", BuildConfig.VERSION_CODE.toString())
                    .header("X-Device-Model", "${Build.MANUFACTURER} ${Build.MODEL}".filter { it.code in 32..126 }.take(80))
                    .header("X-OS-Version", "Android ${Build.VERSION.RELEASE}".filter { it.code in 32..126 }.take(60))
                    .header("Accept", "application/json").header("Accept-Language", requestLanguage).build())
                if (requestLanguage != language()) {
                    response.close()
                    throw IOException("The application language changed during this request")
                }
                response
            }
            .addInterceptor { chain ->
                val request = chain.request()
                if (request.url.encodedPath.startsWith("/api/v1/") && request.method in setOf("PUT", "PATCH", "DELETE")) {
                    chain.proceed(request.newBuilder()
                        .header("X-HTTP-Method-Override", request.method)
                        .method("POST", request.body ?: ByteArray(0).toRequestBody())
                        .build())
                } else chain.proceed(request)
            }
            .addInterceptor(SessionExpiry.interceptor)
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(
                        HttpLoggingInterceptor { log(Log.DEBUG, TAG, it) }.apply {
                            level = HttpLoggingInterceptor.Level.BODY
                            redactHeader("Authorization")
                        },
                    )
                    eventListenerFactory(LoggingEventListener.Factory { log(Log.VERBOSE, NET_TAG, it.replace(BEARER, "Authorization:██")) })
                }
            }
            .build()
    }

    /** DEC-061: one decoder for every typed model. Extra server fields are ignored; a missing required field fails the decode. */
    val json = Json { ignoreUnknownKeys = true }

    private val typedServices = mutableMapOf<Triple<String, Class<*>, String?>, Any>()

    /**
     * A typed Retrofit interface (DEC-061) on the shared client, decoding with [json]. [appMode] is sent as
     * `X-App-Mode` on every call.
     */
    @Synchronized
    fun <T : Any> typed(baseUrl: String, service: Class<T>, appMode: String? = null): T {
        val normalized = baseUrl.trimEnd('/') + "/"
        return service.cast(
            typedServices.getOrPut(Triple(normalized, service, appMode)) {
                val modeClient = appMode?.let { mode ->
                    client.newBuilder()
                        .addInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("X-App-Mode", mode).build()) }
                        .build()
                } ?: client
                Retrofit.Builder()
                    .baseUrl(normalized)
                    .client(modeClient)
                    .addConverterFactory(json.asConverterFactory("application/json; charset=utf-8".toMediaType()))
                    .build()
                    .create(service)
            },
        )!!
    }
}

/** The server's error envelope (`Error` in openapi.json). `fields` comes with VALIDATION_FAILED. */
@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val rule: String? = null,
    val fields: Map<String, List<String>> = emptyMap(),
)

@Serializable
private data class ApiErrorEnvelope(val error: ApiError)

/** A non-2xx answer from a typed call. [error] is null when the body is not the error envelope. */
class ApiException(val status: Int, val error: ApiError?) :
    RuntimeException("HTTP $status ${error?.code.orEmpty()} ${error?.message.orEmpty()}".trim()) {
    /** The server's code, or `HTTP_<status>` when it sent none (the same fallback the JSONObject code used). */
    val code: String get() = error?.code ?: "HTTP_$status"
}

/** [bremoCall] for code that still runs on worker threads: executes the call and returns the decoded body. */
fun <T> bremoExecute(call: Call<T>): T {
    val request = call.request()
    val method = request.method
    val path = request.url.encodedPath.substringAfter("/api/v1/") + (request.url.encodedQuery?.let { "?$it" } ?: "")
    val response = try {
        call.execute()
    } catch (exception: IOException) {
        val failure = NetworkException(exception.failureKind(), request.url.toString(), exception)
        log(Log.ERROR, BremoHttp.TAG, "$method $path ✗ ${failure.message}", exception)
        throw failure
    } catch (exception: Exception) {
        BremoDiagnostics.record(exception)
        throw exception
    }
    return bodyOrThrow(method, path, response)
}

/**
 * Runs one typed call: a transport failure becomes [NetworkException], a non-2xx
 * answer becomes [ApiException], and both are logged under [BremoHttp.TAG].
 */
suspend fun <T> bremoCall(method: String, path: String, call: suspend () -> Response<T>): T {
    val response = try {
        call()
    } catch (exception: IOException) {
        val failure = NetworkException(exception.failureKind(), path, exception)
        log(Log.ERROR, BremoHttp.TAG, "$method $path ✗ ${failure.message}", exception)
        throw failure
    } catch (exception: Exception) {
        BremoDiagnostics.record(exception)
        throw exception
    }
    return bodyOrThrow(method, path, response)
}

private fun <T> bodyOrThrow(method: String, path: String, response: Response<T>): T {
    if (!response.isSuccessful) {
        val text = response.errorBody()?.use { it.string() }.orEmpty()
        val error = runCatching { BremoHttp.json.decodeFromString(ApiErrorEnvelope.serializer(), text).error }.getOrNull()
        log(Log.WARN, BremoHttp.TAG, "$method $path ← HTTP ${response.code()} ${error?.let { "${it.code}: ${it.message}" } ?: text.take(300)}")
        throw ApiException(response.code(), error).also { BremoDiagnostics.record(it) }
    }
    @Suppress("UNCHECKED_CAST")
    return response.body() ?: Unit as T
}

/** What went wrong before any HTTP status existed. The message names the cause in Logcat (logs only, never shown). */
enum class NetworkFailureKind(val description: String) {
    DNS("Could not resolve the server name (DNS)"),
    UNREACHABLE("Server unreachable from this network (no route)"),
    REFUSED("Server refused the connection or is not listening on this port"),
    TIMEOUT("Connection to the server timed out"),
    TLS("Secure connection failed (SSL certificate)"),
    OTHER("Unexpected network error"),
}

class NetworkException(val kind: NetworkFailureKind, val url: String, cause: IOException) :
    IOException("${kind.description} — $url — ${cause.javaClass.simpleName}: ${cause.message}", cause)

internal fun IOException.failureKind(): NetworkFailureKind = when (this) {
    is UnknownHostException -> NetworkFailureKind.DNS
    is NoRouteToHostException -> NetworkFailureKind.UNREACHABLE
    is SocketTimeoutException -> NetworkFailureKind.TIMEOUT
    is SSLException -> NetworkFailureKind.TLS
    is ConnectException -> if (message.orEmpty().contains("ENETUNREACH") || message.orEmpty().contains("EHOSTUNREACH")) {
        NetworkFailureKind.UNREACHABLE
    } else {
        NetworkFailureKind.REFUSED
    }
    else -> NetworkFailureKind.OTHER
}

/** Logs a failure that a screen turns into its generic error state, so the cause is never lost. */
fun logFailure(where: String, error: Throwable) {
    BremoDiagnostics.record(error)
    log(Log.ERROR, BremoHttp.TAG, "$where failed: ${error.javaClass.simpleName}: ${error.message}", error)
}

/** Logcat write that never fails the caller (JVM unit tests run without the native logger). */
internal fun log(priority: Int, tag: String, message: String, error: Throwable? = null) {
    try {
        Log.println(priority, tag, if (error == null) message else message + "\n" + Log.getStackTraceString(error))
    } catch (_: Throwable) {
        // No Logcat on the JVM.
    }
}

/**
 * DEC-065: a 401 on a request that carried a token means the session is over (revoked, blocked, server reset).
 * The app registers [onExpired] once; it runs on the main thread, once per session, however many calls fail together.
 */
object SessionExpiry {
    @Volatile
    var onExpired: (() -> Unit)? = null

    @Volatile
    private var notified = false

    /** Called when a new token is saved, so the next expiry is reported again. */
    fun reset() {
        notified = false
    }

    internal val interceptor = okhttp3.Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)
        if (response.code == 401 && request.header("Authorization") != null && !request.url.encodedPath.endsWith("/auth/logout")) {
            expire()
        }
        response
    }

    @Synchronized
    private fun expire() {
        if (notified) return
        notified = true
        log(Log.WARN, BremoHttp.TAG, "401 on an authenticated request: session expired (DEC-065)")
        val callback = onExpired ?: return
        dispatch(callback)
    }

    /** Main-thread delivery; JVM tests run it inline. */
    internal var dispatch: (() -> Unit) -> Unit = { callback ->
        android.os.Handler(android.os.Looper.getMainLooper()).post(callback)
    }
}
