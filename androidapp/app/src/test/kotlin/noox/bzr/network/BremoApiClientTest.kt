package noox.bzr.network

import java.net.ServerSocket
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class BremoApiClientTest {
    private lateinit var server: MockWebServer

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun stop() {
        server.shutdown()
    }

    private fun client(mode: String? = "CUSTOMER") = BremoApiClient(server.url("/api/v1/").toString(), mode)

    @Test
    fun sendsTheSameHeadersAndBodyAsBefore() {
        server.enqueue(MockResponse().setBody("""{"data":{"id":7}}"""))

        val response = client().send("orders?scope=current", "POST", JSONObject().put("a", 1), token = "T", idempotencyKey = "K")

        val request = server.takeRequest()
        assertEquals("/api/v1/orders?scope=current", request.path)
        assertEquals("POST", request.method)
        assertEquals("application/json", request.getHeader("Accept"))
        assertEquals("CUSTOMER", request.getHeader("X-App-Mode"))
        assertEquals("Bearer T", request.getHeader("Authorization"))
        assertEquals("K", request.getHeader("Idempotency-Key"))
        assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
        assertEquals(1, JSONObject(request.body.readUtf8()).getInt("a"))
        assertEquals(7, response.json().getJSONObject("data").getInt("id"))
    }

    @Test
    fun getAndPatchUseTheirMethods() {
        server.enqueue(MockResponse().setBody("{}"))
        server.enqueue(MockResponse().setBody("{}"))

        client(null).send("me", "GET", token = "T")
        client(null).send("orders/1", "PATCH", JSONObject())

        val get = server.takeRequest()
        assertEquals("GET", get.method)
        assertEquals(null, get.getHeader("X-App-Mode"))
        assertEquals("PATCH", server.takeRequest().method)
    }

    @Test
    fun errorStatusKeepsTheServerBody() {
        server.enqueue(MockResponse().setResponseCode(422).setBody("""{"error":{"code":"VALIDATION_FAILED","message":"x"}}"""))

        val response = client().send("orders", "POST")

        assertFalse(response.isSuccessful)
        assertEquals(422, response.status)
        assertEquals("VALIDATION_FAILED", response.json().getJSONObject("error").getString("code"))
    }

    @Test
    fun uploadSendsMultipartFile() {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"media":{"id":3}}"""))

        val response = client("PROVIDER").upload("media", "T", "a.jpg", "image/jpeg", byteArrayOf(1, 2, 3))

        val request = server.takeRequest()
        assertTrue(request.getHeader("Content-Type")!!.startsWith("multipart/form-data"))
        assertTrue(request.body.readUtf8().contains("name=\"file\"; filename=\"a.jpg\""))
        assertEquals(3, response.json().getJSONObject("media").getInt("id"))
    }

    @Test
    fun connectionFailureNamesTheCause() {
        val port = ServerSocket(0).use { it.localPort }

        try {
            BremoApiClient("http://127.0.0.1:$port/api/v1/").send("config", "GET")
            fail("expected a network failure")
        } catch (failure: NetworkException) {
            assertEquals(NetworkFailureKind.REFUSED, failure.kind)
            assertTrue(failure.message!!.contains("http://127.0.0.1:$port/api/v1/config"))
        }
    }
}
