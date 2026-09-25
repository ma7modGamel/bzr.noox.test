package noox.bzr.auth

enum class AuthPhase { Editing, Loading, Waiting, Success, Error }

data class AuthUiState(
    val screen: String,
    val phase: AuthPhase,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val password: String = "",
    val canSubmit: Boolean = true,
    val nameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val passwordError: String? = null,
    val messageKey: String? = null,
    val route: String? = null,
)

object AuthLogic {
    private val emailPattern = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
    private val phonePattern = Regex("^01[0-9]{9}$")

    fun reduce(screen: String, input: Map<String, Any?>): AuthUiState = when (screen) {
        "SCR-C10" -> login(input)
        "SCR-C11" -> register(input)
        "SCR-C12" -> verify(input)
        "SCR-C13" -> forgot(input)
        else -> error("Unknown auth screen: $screen")
    }

    private fun login(input: Map<String, Any?>): AuthUiState {
        val event = input.text("event")
        val email = input.text("email")
        val password = input.text("password")
        val errors = loginErrors(email, password)
        val valid = errors.first == null && errors.second == null
        val base = AuthUiState("SCR-C10", AuthPhase.Editing, email = email, password = password, canSubmit = valid, emailError = errors.first, passwordError = errors.second)

        return when (event) {
            "submit" -> if (valid) base.copy(phase = AuthPhase.Loading, canSubmit = false) else base
            "api_error" -> base.copy(
                phase = AuthPhase.Error,
                messageKey = when (input.text("error_code")) {
                    "ACCOUNT_BLOCKED" -> "auth.error.blocked"
                    "RATE_LIMITED" -> "auth.error.rate_limited"
                    else -> "auth.error.credentials"
                },
            )
            "network_error" -> base.copy(phase = AuthPhase.Error, messageKey = "error.body")
            "login_succeeded" -> base.copy(phase = AuthPhase.Success, canSubmit = false, route = if (input.boolean("is_verified")) "SCR-C01" else "SCR-C12")
            else -> base
        }
    }

    private fun register(input: Map<String, Any?>): AuthUiState {
        val name = input.text("name")
        val email = input.text("email")
        val phone = input.text("phone")
        val password = input.text("password")
        val nameError = if (name.isBlank()) "auth.validation.name_required" else null
        val emailError = emailError(email)
        val phoneError = if (!phonePattern.matches(phone)) "auth.validation.phone_invalid" else null
        val passwordError = if (password.length < 8) "auth.validation.password_short" else null
        val valid = listOf(nameError, emailError, phoneError, passwordError).all { it == null }
        val base = AuthUiState(
            "SCR-C11", AuthPhase.Editing, name, email, phone, password, valid,
            nameError, emailError, phoneError, passwordError,
        )

        return when (input.text("event")) {
            "submit" -> if (valid) base.copy(phase = AuthPhase.Loading, canSubmit = false) else base
            "api_error" -> base.copy(
                phase = AuthPhase.Error,
                emailError = if (input.text("field") == "email") "auth.validation.email_used" else emailError,
                messageKey = if (input.text("error_code") == "RATE_LIMITED") "auth.error.rate_limited" else null,
            )
            "network_error" -> base.copy(phase = AuthPhase.Error, messageKey = "error.body")
            "register_succeeded" -> base.copy(phase = AuthPhase.Success, canSubmit = false, route = "SCR-C12")
            else -> base
        }
    }

    private fun verify(input: Map<String, Any?>): AuthUiState {
        val base = AuthUiState("SCR-C12", AuthPhase.Waiting, email = input.text("email"))

        return when (input.text("event")) {
            "check", "resend" -> base.copy(phase = AuthPhase.Loading, canSubmit = false)
            "resend_succeeded" -> base.copy(phase = AuthPhase.Success, messageKey = "auth.verify.resent")
            "verification_checked" -> if (input.boolean("is_verified")) {
                base.copy(phase = AuthPhase.Success, canSubmit = false, route = "SCR-C01")
            } else {
                base.copy(messageKey = "auth.verify.not_yet")
            }
            "network_error" -> base.copy(phase = AuthPhase.Error, messageKey = "error.body")
            "api_error" -> base.copy(phase = AuthPhase.Error, messageKey = if (input.text("error_code") == "RATE_LIMITED") "auth.error.rate_limited" else "error.body")
            "session_expired" -> base.copy(phase = AuthPhase.Success, canSubmit = false, route = "SCR-C10")
            else -> base
        }
    }

    private fun forgot(input: Map<String, Any?>): AuthUiState {
        val email = input.text("email")
        val emailError = emailError(email)
        val base = AuthUiState("SCR-C13", AuthPhase.Editing, email = email, canSubmit = emailError == null, emailError = emailError)

        return when (input.text("event")) {
            "submit" -> if (emailError == null) base.copy(phase = AuthPhase.Loading, canSubmit = false) else base
            "forgot_succeeded" -> base.copy(phase = AuthPhase.Success, messageKey = "auth.password.forgot.sent")
            "network_error" -> base.copy(phase = AuthPhase.Error, messageKey = "error.body")
            "api_error" -> base.copy(phase = AuthPhase.Error, messageKey = if (input.text("error_code") == "RATE_LIMITED") "auth.error.rate_limited" else "error.body")
            else -> base
        }
    }

    private fun loginErrors(email: String, password: String): Pair<String?, String?> = emailError(email) to if (password.isBlank()) "auth.validation.password_required" else null

    private fun emailError(email: String): String? = when {
        email.isBlank() -> "auth.validation.email_required"
        !emailPattern.matches(email) -> "auth.validation.email_invalid"
        else -> null
    }

    private fun Map<String, Any?>.text(key: String): String = this[key] as? String ?: ""
    private fun Map<String, Any?>.boolean(key: String): Boolean = this[key] as? Boolean ?: false
}
