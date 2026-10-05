package noox.bzr.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/** The coroutine ViewModel keeps the flow the Thread-based one had: session saved, then the verified route. */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private val session = object : AuthSessionStore {
        override var token: String? = null
        override var email: String? = null
        override fun clear() {
            token = null
            email = null
        }
    }

    @Before
    fun main() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun reset() = Dispatchers.resetMain()

    @Test
    fun loginStoresTheSession() {
        val viewModel = AuthViewModel(FakeAuthApi(login = { AuthSession("T", "a@b.co", true) }), session)
        viewModel.update("email", "a@b.co")
        viewModel.update("password", "secret-password")

        viewModel.submit()

        assertEquals("T", session.token)
        assertEquals("a@b.co", session.email)
        assertEquals(AuthLogic.reduce("SCR-C10", mapOf("email" to "a@b.co", "password" to "secret-password", "event" to "login_succeeded", "is_verified" to true)).route, viewModel.state.route)
    }

    @Test
    fun serverErrorReachesTheScreen() {
        val viewModel = AuthViewModel(FakeAuthApi(login = { throw AuthApiException("ACCOUNT_BLOCKED") }), session)
        viewModel.update("email", "a@b.co")
        viewModel.update("password", "secret-password")

        viewModel.submit()

        assertNull(session.token)
        assertEquals(AuthPhase.Error, viewModel.state.phase)
    }

    @Test
    fun networkFailureShowsTheNetworkError() {
        val viewModel = AuthViewModel(FakeAuthApi(login = { throw java.io.IOException("offline") }), session)
        viewModel.update("email", "a@b.co")
        viewModel.update("password", "secret-password")

        viewModel.submit()

        assertEquals(
            AuthLogic.reduce("SCR-C10", mapOf("email" to "a@b.co", "password" to "secret-password", "event" to "network_error")),
            viewModel.state,
        )
    }

    private class FakeAuthApi(private val login: suspend () -> AuthSession) : AuthApi {
        override suspend fun login(email: String, password: String) = login()
        override suspend fun register(name: String, email: String, phone: String, password: String) = login()
        override suspend fun me(token: String) = true
        override suspend fun resendVerification(token: String) = Unit
        override suspend fun forgotPassword(email: String) = Unit
        override suspend fun logout(token: String) = Unit
    }
}
