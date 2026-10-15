package noox.bzr.network

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** DEC-065: only a 401 on a call that carried a token ends the session, and only once. */
class SessionExpiryTest {
    private lateinit var server: MockWebServer
    private var expired = 0

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        expired = 0
        SessionExpiry.reset()
        SessionExpiry.dispatch = { it() }
        SessionExpiry.onExpired = { expired++ }
    }

    @After
    fun stop() {
        SessionExpiry.onExpired = null
        SessionExpiry.reset()
        server.shutdown()
    }

    private val service get() = BremoHttp.typed(server.url("/api/v1/").toString(), ProbeService::class.java, "CUSTOMER")

    private fun unauthorized() = MockResponse().setResponseCode(401).setBody("""{"error":{"code":"UNAUTHENTICATED","message":"x"}}""")

    private fun call(block: () -> Unit) {
        runCatching(block)
    }

    @Test
    fun refusedTokenExpiresTheSessionOnce() {
        repeat(3) { server.enqueue(unauthorized()) }

        repeat(3) { call { bremoExecute(service.get("Bearer old")) } }

        assertEquals(1, expired)
    }

    @Test
    fun loginWithoutTokenIsNotAnExpiry() {
        server.enqueue(unauthorized())

        call { bremoExecute(service.get(null)) }

        assertEquals(0, expired)
    }

    @Test
    fun logoutWithRefusedTokenIsNotAnExpiry() {
        server.enqueue(unauthorized())

        call { bremoExecute(service.logout("Bearer old")) }

        assertEquals(0, expired)
    }

    @Test
    fun aNewSessionCanExpireAgain() {
        repeat(2) { server.enqueue(unauthorized()) }

        call { bremoExecute(service.get("Bearer old")) }
        SessionExpiry.reset()
        call { bremoExecute(service.get("Bearer new")) }

        assertEquals(2, expired)
    }

    @Test
    fun otherErrorsKeepTheSession() {
        server.enqueue(MockResponse().setResponseCode(403).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))

        call { bremoExecute(service.get("Bearer t")) }
        call { bremoExecute(service.get("Bearer t")) }

        assertEquals(0, expired)
    }
}
