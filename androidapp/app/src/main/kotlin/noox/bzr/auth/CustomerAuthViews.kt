package noox.bzr.auth

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.FieldVisualState
import noox.bzr.design.R
import noox.bzr.design.views.ErrorStateView
import noox.bzr.design.views.FieldShellView
import noox.bzr.design.views.InfoBannerView
import noox.bzr.gallery.databinding.ScreenC10LoginBinding
import noox.bzr.gallery.databinding.ScreenC11RegisterBinding
import noox.bzr.gallery.databinding.ScreenC12VerifyBinding
import noox.bzr.gallery.databinding.ScreenC13RecoveryBinding

// SCR-C10..C13 as XML Views (DEC-047). They only render AuthUiState; AuthLogic / AuthViewModel decide.

/** Callbacks shared by the four auth screens; the fragment wires them to [AuthViewModel]. */
class AuthActions(
    val onFieldChange: ((String, String) -> Unit)? = null,
    val onSubmit: () -> Unit = {},
    val onCheck: () -> Unit = {},
    val onResend: () -> Unit = {},
    val onOpen: (String) -> Unit = {},
    val onLogout: () -> Unit = {},
)

abstract class AuthScreenView(context: Context) : FrameLayout(context) {
    protected val inflater: LayoutInflater = LayoutInflater.from(context)
    var actions: AuthActions = AuthActions()
        set(value) {
            field = value
            bindActions()
        }

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        // RTL is mandatory (43 §13), whatever the device locale — as BzrTheme did for Compose.
        layoutDirection = LAYOUT_DIRECTION_RTL
    }

    abstract fun render(state: AuthUiState)

    protected abstract fun bindActions()

    protected fun message(state: AuthUiState, error: ErrorStateView, info: InfoBannerView) {
        val key = state.messageKey
        error.isVisible = key != null && state.phase == AuthPhase.Error
        info.isVisible = key != null && state.phase != AuthPhase.Error
        if (key != null) {
            error.body = context.getString(authTextRes(key))
            info.text = context.getString(authTextRes(key))
        }
    }

    protected fun field(view: FieldShellView, value: String, error: String?, name: String) {
        val text = error?.let { context.getString(authTextRes(it)) }
        val state = authFieldState(value, error)
        when (view) {
            is noox.bzr.design.views.AppTextFieldView -> view.also { it.value = value; it.state = state; it.error = text }
            is noox.bzr.design.views.SecureTextFieldView -> view.also { it.value = value; it.state = state; it.error = text }
        }
        val handler = actions.onFieldChange?.let { change -> { input: String -> change(name, input) } }
        when (view) {
            is noox.bzr.design.views.AppTextFieldView -> view.onValueChange = handler
            is noox.bzr.design.views.SecureTextFieldView -> view.onValueChange = handler
        }
    }
}

class CustomerLoginScreen(context: Context) : AuthScreenView(context) {
    private val binding = ScreenC10LoginBinding.inflate(inflater, this, true)
    private var state: AuthUiState? = null

    override fun render(state: AuthUiState) {
        this.state = state
        message(state, binding.messageError, binding.messageInfo)
        field(binding.email, state.email, state.emailError, "email")
        field(binding.password, state.password, state.passwordError, "password")
        binding.submit.state = authButtonStateOf(state)
        binding.register.enabled = state.phase != AuthPhase.Loading
    }

    override fun bindActions() {
        binding.submit.onClick = actions.onSubmit
        binding.register.onClick = { actions.onOpen("SCR-C11") }
        binding.forgot.onClick = { actions.onOpen("SCR-C13") }
        state?.let(::render)
    }
}

class CustomerRegisterScreen(context: Context) : AuthScreenView(context) {
    private val binding = ScreenC11RegisterBinding.inflate(inflater, this, true)
    private var state: AuthUiState? = null

    override fun render(state: AuthUiState) {
        this.state = state
        message(state, binding.messageError, binding.messageInfo)
        field(binding.name, state.name, state.nameError, "name")
        field(binding.email, state.email, state.emailError, "email")
        field(binding.phone, state.phone, state.phoneError, "phone")
        field(binding.password, state.password, state.passwordError, "password")
        binding.submit.state = authButtonStateOf(state)
    }

    override fun bindActions() {
        binding.topBar.onBack = { actions.onOpen("SCR-C10") }
        binding.submit.onClick = actions.onSubmit
        binding.login.onClick = { actions.onOpen("SCR-C10") }
        state?.let(::render)
    }
}

class CustomerEmailVerificationScreen(context: Context) : AuthScreenView(context) {
    private val binding = ScreenC12VerifyBinding.inflate(inflater, this, true)

    override fun render(state: AuthUiState) {
        binding.waiting.body = context.getString(R.string.auth_verify_waiting_body) + "\n" + state.email
        message(state, binding.messageError, binding.messageInfo)
        binding.check.state = authButtonStateOf(state)
        binding.resend.enabled = state.canSubmit
    }

    override fun bindActions() {
        binding.topBar.onBack = actions.onLogout
        binding.check.onClick = actions.onCheck
        binding.resend.onClick = actions.onResend
        binding.logout.onClick = actions.onLogout
    }
}

class CustomerPasswordRecoveryScreen(context: Context) : AuthScreenView(context) {
    private val binding = ScreenC13RecoveryBinding.inflate(inflater, this, true)
    private var state: AuthUiState? = null

    override fun render(state: AuthUiState) {
        this.state = state
        message(state, binding.messageError, binding.messageInfo)
        field(binding.email, state.email, state.emailError, "email")
        binding.submit.state = authButtonStateOf(state)
    }

    override fun bindActions() {
        binding.topBar.onBack = { actions.onOpen("SCR-C10") }
        binding.submit.onClick = actions.onSubmit
        binding.login.onClick = { actions.onOpen("SCR-C10") }
        state?.let(::render)
    }
}

/** The view for an auth screen code (SCR-C10 is the default, as in the Compose flow). */
fun authScreenView(context: Context, screen: String): AuthScreenView = when (screen) {
    "SCR-C11" -> CustomerRegisterScreen(context)
    "SCR-C12" -> CustomerEmailVerificationScreen(context)
    "SCR-C13" -> CustomerPasswordRecoveryScreen(context)
    else -> CustomerLoginScreen(context)
}

private fun authFieldState(value: String, error: String?): FieldVisualState = when {
    error != null -> FieldVisualState.Error
    value.isEmpty() -> FieldVisualState.Empty
    else -> FieldVisualState.Filled
}

private fun authButtonStateOf(state: AuthUiState): ButtonVisualState = when {
    state.phase == AuthPhase.Loading -> ButtonVisualState.Loading
    !state.canSubmit -> ButtonVisualState.Disabled
    else -> ButtonVisualState.Normal
}

internal fun authTextRes(key: String): Int = when (key) {
    "auth.validation.name_required" -> R.string.auth_validation_name_required
    "auth.validation.email_required" -> R.string.auth_validation_email_required
    "auth.validation.email_invalid" -> R.string.auth_validation_email_invalid
    "auth.validation.email_used" -> R.string.auth_validation_email_used
    "auth.validation.phone_invalid" -> R.string.auth_validation_phone_invalid
    "auth.validation.password_required" -> R.string.auth_validation_password_required
    "auth.validation.password_short" -> R.string.auth_validation_password_short
    "auth.error.credentials" -> R.string.auth_error_credentials
    "auth.error.blocked" -> R.string.auth_error_blocked
    "auth.error.rate_limited" -> R.string.auth_error_rate_limited
    "auth.verify.resent" -> R.string.auth_verify_resent
    "auth.verify.not_yet" -> R.string.auth_verify_not_yet
    "auth.password.forgot.sent" -> R.string.auth_password_forgot_sent
    else -> R.string.error_body
}
