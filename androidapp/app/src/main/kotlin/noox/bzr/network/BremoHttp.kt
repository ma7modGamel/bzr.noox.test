package noox.bzr.network

import android.util.Log
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
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.LoggingEventListener
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.HeaderMap
import retrofit2.http.Url

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

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain -> chain.proceed(chain.request().newBuilder().header("Accept", "application/json").build()) }
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

    private val services = mutableMapOf<String, BremoService>()
    private val typedServices = mutableMapOf<Triple<String, Class<*>, String?>, Any>()

    @Synchronized
    fun service(baseUrl: String): BremoService {
        val normalized = baseUrl.trimEnd('/') + "/"
        return services.getOrPut(normalized) {
            Retrofit.Builder().baseUrl(normalized).client(client).build().create(BremoService::class.java)
        }
    }

    /**
     * A typed Retrofit interface (DEC-061) on the shared client, decoding with [json]. [appMode] is sent as
     * `X-App-Mode` on every call, as [BremoApiClient] does for its surface.
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
    }
    return bodyOrThrow(method, path, response)
}

/**
 * Runs one typed call: a transport failure becomes [NetworkException] (as [BremoApiClient] does), a non-2xx
 * answer becomes [ApiException], and both are logged under [BremoHttp.TAG].
 */
suspend fun <T> bremoCall(method: String, path: String, call: suspend () -> Response<T>): T {
    val response = try {
        call()
    } catch (exception: IOException) {
        val failure = NetworkException(exception.failureKind(), path, exception)
        log(Log.ERROR, BremoHttp.TAG, "$method $path ✗ ${failure.message}", exception)
        throw failure
    }
    return bodyOrThrow(method, path, response)
}

private fun <T> bodyOrThrow(method: String, path: String, response: Response<T>): T {
    if (!response.isSuccessful) {
        val text = response.errorBody()?.use { it.string() }.orEmpty()
        val error = runCatching { BremoHttp.json.decodeFromString(ApiErrorEnvelope.serializer(), text).error }.getOrNull()
        log(Log.WARN, BremoHttp.TAG, "$method $path ← HTTP ${response.code()} ${error?.let { "${it.code}: ${it.message}" } ?: text.take(300)}")
        throw ApiException(response.code(), error)
    }
    @Suppress("UNCHECKED_CAST")
    return response.body() ?: Unit as T
}

/** Untyped Retrofit endpoints: each API keeps parsing its own JSON (org.json), the transport is shared. */
interface BremoService {
    @HTTP(method = "GET")
    fun get(@Url path: String, @HeaderMap headers: Map<String, String>): Call<ResponseBody>

    @HTTP(method = "DELETE")
    fun delete(@Url path: String, @HeaderMap headers: Map<String, String>): Call<ResponseBody>

    @HTTP(method = "POST", hasBody = true)
    fun post(@Url path: String, @HeaderMap headers: Map<String, String>, @Body body: RequestBody): Call<ResponseBody>

    @HTTP(method = "PATCH", hasBody = true)
    fun patch(@Url path: String, @HeaderMap headers: Map<String, String>, @Body body: RequestBody): Call<ResponseBody>

    @HTTP(method = "PUT", hasBody = true)
    fun put(@Url path: String, @HeaderMap headers: Map<String, String>, @Body body: RequestBody): Call<ResponseBody>
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

data class ApiResponse(val status: Int, val text: String) {
    val isSuccessful: Boolean get() = status in 200..299

    fun json(): JSONObject = if (text.isBlank()) JSONObject() else JSONObject(text)
}

/** One API surface (customer, provider, auth) on top of the shared client. */
class BremoApiClient(private val baseUrl: String, private val appMode: String? = null) {
    private val service get() = BremoHttp.service(baseUrl)

    fun send(
        path: String,
        method: String,
        body: JSONObject? = null,
        token: String? = null,
        idempotencyKey: String? = null,
    ): ApiResponse {
        val headers = headers(token, idempotencyKey)
        val payload = (body ?: JSONObject()).toString().toRequestBody(JSON)
        val call = when (method) {
            "GET" -> service.get(path, headers)
            "DELETE" -> service.delete(path, headers)
            "PATCH" -> service.patch(path, headers, payload)
            "PUT" -> service.put(path, headers, payload)
            else -> service.post(path, headers, payload)
        }
        return execute(method, path, call)
    }

    fun upload(path: String, token: String, fileName: String, mimeType: String, bytes: ByteArray): ApiResponse {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, bytes.toRequestBody(mimeType.toMediaType()))
            .build()
        return execute("POST", path, service.post(path, headers(token, null), body))
    }

    private fun headers(token: String?, idempotencyKey: String?): Map<String, String> = buildMap {
        appMode?.let { put("X-App-Mode", it) }
        token?.let { put("Authorization", "Bearer $it") }
        idempotencyKey?.let { put("Idempotency-Key", it) }
    }

    private fun execute(method: String, path: String, call: Call<ResponseBody>): ApiResponse {
        val url = call.request().url.toString()
        val response = try {
            call.execute()
        } catch (exception: IOException) {
            val failure = NetworkException(exception.failureKind(), url, exception)
            log(Log.ERROR, BremoHttp.TAG, "$method $url ✗ ${failure.message}", exception)
            throw failure
        }
        val text = (response.body() ?: response.errorBody())?.use { it.string() }.orEmpty()
        if (!response.isSuccessful) {
            log(Log.WARN, BremoHttp.TAG, "$method $url ← HTTP ${response.code()} ${errorSummary(text)}")
        }
        return ApiResponse(response.code(), text)
    }

    private fun errorSummary(text: String): String = runCatching {
        JSONObject(text).optJSONObject("error")?.let { "${it.optString("code")}: ${it.optString("message")}" }
    }.getOrNull() ?: text.take(300)

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

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
