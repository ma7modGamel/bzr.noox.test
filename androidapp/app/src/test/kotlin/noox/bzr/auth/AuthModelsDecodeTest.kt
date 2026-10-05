package noox.bzr.auth

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import noox.bzr.network.ApiError
import noox.bzr.network.BremoHttp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DEC-061 / 9أ stage 1: every identity model decodes the real server answers recorded by
 * tests/Feature/Api/MobileAuthResponsesTest.php (RECORD_API_RESPONSES=1).
 */
class AuthModelsDecodeTest {
    @Test
    fun loginDecodesACustomer() {
        val response = decode("login_200", AuthResponse.serializer())
        assertEquals(AuthSession("1|recorded-token", "customer@test.local", true), response.session())
        assertNull(response.user.provider)
        assertEquals("4.50", response.user.ratingAvg)
    }

    @Test
    fun loginDecodesAProviderProfile() {
        val provider = decode("login_200_provider", AuthResponse.serializer()).user.provider!!
        assertEquals("ACTIVE", provider.status)
        assertTrue(provider.availableNow)
        assertTrue(provider.isVerified)
    }

    @Test
    fun registerDecodesAnUnverifiedSession() {
        val response = decode("register_201", AuthResponse.serializer())
        assertFalse(response.user.isVerified)
        assertTrue(response.emailVerificationRequired)
        assertEquals("01012345678", response.user.phone)
    }

    @Test
    fun meDecodesTheAccount() {
        val user = decode("me_200_unverified", AccountResponse.serializer()).user
        assertFalse(user.isVerified)
        assertEquals(1, user.terms?.currentVersion)
        assertTrue(user.terms!!.acceptanceRequired)
        assertTrue("logout" in user.availableActions)
    }

    @Test
    fun errorEnvelopesDecode() {
        val validation = error("error_422_validation")
        assertEquals("VALIDATION_FAILED", validation.code)
        assertEquals(listOf("email"), validation.fields.keys.toList())
        assertEquals("ACCOUNT_BLOCKED", error("error_403_blocked").code)
        assertEquals("EMAIL_ALREADY_VERIFIED", error("error_409_already_verified").code)
        assertEquals("UNAUTHENTICATED", error("error_401").code)
    }

    @Serializable
    private data class Envelope(val error: ApiError)

    private fun error(name: String): ApiError = decode(name, Envelope.serializer()).error

    private fun <T> decode(name: String, serializer: KSerializer<T>): T = BremoHttp.json.decodeFromString(serializer, recorded(name))

    companion object {
        fun recorded(name: String): String =
            requireNotNull(AuthModelsDecodeTest::class.java.classLoader!!.getResource("api/auth/$name.json")) { "missing recorded answer $name" }.readText()
    }
}
