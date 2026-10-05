package noox.bzr.auth

import kotlinx.coroutines.test.runTest
import noox.bzr.auth.AuthModelsDecodeTest.Companion.recorded
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/** The typed identity calls send the same requests as the JSONObject code did, and map errors to the same codes. */
class RetrofitAuthApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: RetrofitAuthApi

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        api = RetrofitAuthApi(server.url("/api/v1/").toString())
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun loginPostsCredentialsAndReturnsTheSession() = runTest {
        server.enqueue(MockResponse().setBody(recorded("login_200")))

        val session = api.login("customer@test.local", "secret-password")

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/auth/login", request.path)
        assertEquals("application/json", request.getHeader("Accept"))
        val body = JSONObject(request.body.readUtf8())
        assertEquals("customer@test.local", body.getString("email"))
        assertEquals("secret-password", body.getString("password"))
        assertEquals(AuthSession("1|recorded-token", "customer@test.local", true), session)
    }

    @Test
    fun registerSendsAllFields() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody(recorded("register_201")))

        assertFalse(api.register("أحمد", "new@test.local", "01012345678", "secret-password").isVerified)

        val body = JSONObject(server.takeRequest().body.readUtf8())
        assertEquals(setOf("name", "email", "phone", "password"), body.keys().asSequence().toSet())
    }

    @Test
    fun meSendsTheTokenAndReadsVerification() = runTest {
        server.enqueue(MockResponse().setBody(recorded("me_200_unverified")))

        assertFalse(api.me("T"))

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v1/me", request.path)
        assertEquals("Bearer T", request.getHeader("Authorization"))
    }

    @Test
    fun noContentCallsSucceed() = runTest {
        repeat(3) { server.enqueue(MockResponse().setResponseCode(204)) }

        api.resendVerification("T")
        api.forgotPassword("a@b.co")
        api.logout("T")

        assertEquals("/api/v1/auth/email/resend", server.takeRequest().path)
        assertEquals("a@b.co", JSONObject(server.takeRequest().body.readUtf8()).getString("email"))
        assertEquals("Bearer T", server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun serverErrorsKeepTheirCode() = runTest {
        assertCode("VALIDATION_FAILED", 422, recorded("error_422_validation")) { api.login("a@b.co", "x") }
        assertCode("ACCOUNT_BLOCKED", 403, recorded("error_403_blocked")) { api.login("a@b.co", "x") }
        assertCode("EMAIL_ALREADY_VERIFIED", 409, recorded("error_409_already_verified")) { api.resendVerification("T") }
        assertCode("UNAUTHENTICATED", 401, recorded("error_401")) { api.me("T") }
        assertCode("HTTP_500", 500, "<html>oops</html>") { api.me("T") }
    }

    private suspend fun assertCode(code: String, status: Int, body: String, block: suspend () -> Unit) {
        server.enqueue(MockResponse().setResponseCode(status).setBody(body))
        try {
            block()
            fail("expected $code")
        } catch (exception: AuthApiException) {
            assertEquals(code, exception.code)
        }
    }
}
