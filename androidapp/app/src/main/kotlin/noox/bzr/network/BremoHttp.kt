package noox.bzr.network

import android.util.Log
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
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
import retrofit2.Retrofit
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

    private val services = mutableMapOf<String, BremoService>()

    @Synchronized
    fun service(baseUrl: String): BremoService {
        val normalized = baseUrl.trimEnd('/') + "/"
        return services.getOrPut(normalized) {
            Retrofit.Builder().baseUrl(normalized).client(client).build().create(BremoService::class.java)
        }
    }
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

/** What went wrong before any HTTP status existed. The message names the cause in Logcat. */
enum class NetworkFailureKind(val description: String) {
    DNS("تعذّر تحويل اسم الخادم إلى عنوان (DNS)"),
    UNREACHABLE("الخادم غير قابل للوصول من هذه الشبكة (لا يوجد مسار)"),
    REFUSED("الخادم رفض الاتصال أو لا يعمل على هذا المنفذ"),
    TIMEOUT("انتهت مهلة الاتصال بالخادم"),
    TLS("فشل الاتصال الآمن (شهادة SSL)"),
    OTHER("خطأ شبكة غير متوقع"),
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
