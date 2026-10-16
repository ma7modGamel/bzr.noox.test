package noox.bzr.network

import java.net.ServerSocket
import kotlinx.serialization.Serializable
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.POST

@Serializable
data class Probe(val id: Int = 0)

@Serializable
data class ProbeBody(val token: String)

/** A minimal typed surface for testing the shared client (DEC-061 transport, DEC-065 expiry). */
interface ProbeService {
    @GET("orders")
    fun get(@Header("Authorization") bearer: String?): Call<Probe>

    @POST("auth/logout")
    fun logout(@Header("Authorization") bearer: String): Call<Unit>

    @HTTP(method = "PUT", path = "provider/availability", hasBody = true)
    fun putWithBody(@Header("Authorization") bearer: String, @Body body: ProbeBody): Call<Unit>

    @HTTP(method = "DELETE", path = "me/devices", hasBody = true)
    fun deleteWithBody(@Header("Authorization") bearer: String, @Body body: ProbeBody): Call<Unit>
}

/** The shared typed client: headers, error envelope, empty answers, and named transport failures. */
class BremoTypedCallsTest {
    private lateinit var server: MockWebServer

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun stop() {
        server.shutdown()
    }

    private fun service(mode: String? = "CUSTOMER") = BremoHttp.typed(server.url("/api/v1/").toString(), ProbeService::class.java, mode)

    @Test
    fun sendsAcceptModeAndBearer() {
        server.enqueue(MockResponse().setBody("""{"id":7}"""))

        assertEquals(7, bremoExecute(service().get("Bearer T")).id)

        val request = server.takeRequest()
        assertEquals("/api/v1/orders", request.path)
        assertEquals("application/json", request.getHeader("Accept"))
        assertEquals("CUSTOMER", request.getHeader("X-App-Mode"))
        assertEquals("Bearer T", request.getHeader("Authorization"))
    }

    @Test
    fun errorEnvelopeBecomesApiException() {
        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"error":{"code":"VALIDATION_FAILED","message":"x","fields":{"price":["required"]}}}"""))

        try {
            bremoExecute(service().get("Bearer T"))
            fail("expected ApiException")
        } catch (exception: ApiException) {
            assertEquals(422, exception.status)
            assertEquals("VALIDATION_FAILED", exception.code)
            assertEquals(listOf("required"), exception.error!!.fields.getValue("price"))
        }
    }

    @Test
    fun emptyAnswerIsUnitAndDeleteKeepsItsBody() {
        server.enqueue(MockResponse().setResponseCode(204))

        bremoExecute(service().deleteWithBody("Bearer T", ProbeBody("push")))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("DELETE", request.getHeader("X-HTTP-Method-Override"))
        assertEquals("push", JSONObject(request.body.readUtf8()).getString("token"))
    }

    @Test
    fun putUsesAllowedPostAndPreservesBodyAndAuthorization() {
        server.enqueue(MockResponse().setResponseCode(204))
        bremoExecute(service().putWithBody("Bearer T", ProbeBody("payload")))
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("PUT", request.getHeader("X-HTTP-Method-Override"))
        assertEquals("Bearer T", request.getHeader("Authorization"))
        assertEquals("payload", JSONObject(request.body.readUtf8()).getString("token"))
    }

    @Test
    fun methodsOutsideTheVersionedApiAreNotChanged() {
        server.enqueue(MockResponse().setResponseCode(204))
        val outside = BremoHttp.typed(server.url("/other/").toString(), ProbeService::class.java)
        bremoExecute(outside.deleteWithBody("Bearer T", ProbeBody("payload")))
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals(null, request.getHeader("X-HTTP-Method-Override"))
    }

    @Test
    fun connectionFailureNamesTheCause() {
        val port = ServerSocket(0).use { it.localPort }
        val closed = BremoHttp.typed("http://127.0.0.1:$port/api/v1/", ProbeService::class.java)

        try {
            bremoExecute(closed.get(null))
            fail("expected a network failure")
        } catch (failure: NetworkException) {
            assertEquals(NetworkFailureKind.REFUSED, failure.kind)
            assertTrue(failure.message!!.contains("http://127.0.0.1:$port/api/v1/orders"))
        }
    }
}
