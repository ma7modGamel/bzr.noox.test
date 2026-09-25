package noox.bzr.auth

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AuthSessionStore {
    var token: String?
    var email: String?
    fun clear()
}

class AuthViewModel(private val api: AuthApi, private val session: AuthSessionStore) : ViewModel() {
    // DEC-047: only the state holder changed (Compose mutableStateOf → StateFlow); fragments collect [stateFlow].
    private val mutableState = MutableStateFlow(AuthUiState("SCR-C10", AuthPhase.Editing))
    val stateFlow: StateFlow<AuthUiState> = mutableState.asStateFlow()
    var state: AuthUiState
        get() = mutableState.value
        private set(value) {
            mutableState.value = value
        }

    fun update(field: String, value: String) {
        val input = values() + (field to value) + ("event" to "validate")
        state = AuthLogic.reduce(state.screen, input)
    }

    fun open(screen: String) {
        state = AuthLogic.reduce(screen, mapOf("event" to "show", "email" to (session.email ?: "")))
    }

    fun submit() {
        val input = values() + ("event" to "submit")
        val loading = AuthLogic.reduce(state.screen, input)
        state = loading
        if (loading.phase != AuthPhase.Loading) return

        runRequest {
            when (loading.screen) {
                "SCR-C10" -> acceptSession(api.login(loading.email, loading.password), "login_succeeded")
                "SCR-C11" -> acceptSession(api.register(loading.name, loading.email, loading.phone, loading.password), "register_succeeded")
                "SCR-C13" -> {
                    api.forgotPassword(loading.email)
                    transition("forgot_succeeded")
                }
            }
        }
    }

    fun checkVerification() {
        state = AuthLogic.reduce("SCR-C12", values() + ("event" to "check"))
        runRequest {
            val verified = api.me(requireNotNull(session.token))
            transition("verification_checked", mapOf("is_verified" to verified))
        }
    }

    fun resendVerification() {
        state = AuthLogic.reduce("SCR-C12", values() + ("event" to "resend"))
        Thread {
            try {
                api.resendVerification(requireNotNull(session.token))
                transition("resend_succeeded")
            } catch (exception: AuthApiException) {
                if (exception.code == "EMAIL_ALREADY_VERIFIED") {
                    checkVerificationAfterConflict()
                } else {
                    handleApiException(exception)
                }
            } catch (_: Exception) {
                transition("network_error")
            }
        }.start()
    }

    fun logout() {
        session.token?.let { token -> runRequest { api.logout(token) } }
        session.clear()
        open("SCR-C10")
    }

    private fun acceptSession(authSession: AuthSession, event: String) {
        session.token = authSession.token
        session.email = authSession.email
        transition(event, mapOf("is_verified" to authSession.isVerified))
    }

    private fun transition(event: String, extra: Map<String, Any?> = emptyMap()) {
        Handler(Looper.getMainLooper()).post {
            state = AuthLogic.reduce(state.screen, values() + extra + ("event" to event))
        }
    }

    private fun runRequest(block: () -> Unit) {
        Thread {
            try {
                block()
            } catch (exception: AuthApiException) {
                handleApiException(exception)
            } catch (_: Exception) {
                transition("network_error")
            }
        }.start()
    }

    private fun checkVerificationAfterConflict() {
        try {
            transition("verification_checked", mapOf("is_verified" to api.me(requireNotNull(session.token))))
        } catch (exception: AuthApiException) {
            handleApiException(exception)
        } catch (_: Exception) {
            transition("network_error")
        }
    }

    private fun handleApiException(exception: AuthApiException) {
        if (state.screen == "SCR-C12" && exception.code in setOf("UNAUTHENTICATED", "HTTP_401")) {
            session.clear()
            transition("session_expired")
            return
        }
        transition(
            "api_error",
            mapOf(
                "error_code" to exception.code,
                "field" to (exception.fields.keys.firstOrNull() ?: if (state.screen == "SCR-C11" && exception.code == "VALIDATION_FAILED") "email" else null),
            ),
        )
    }

    private fun values(): Map<String, Any?> = mapOf(
        "name" to state.name,
        "email" to state.email,
        "phone" to state.phone,
        "password" to state.password,
    )
}
