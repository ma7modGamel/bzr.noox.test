package noox.bzr.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import noox.bzr.design.AppTextField
import noox.bzr.design.AppTopBar
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.BzrText
import noox.bzr.design.BzrTheme
import noox.bzr.design.EmptyState
import noox.bzr.design.ErrorState
import noox.bzr.design.FieldVisualState
import noox.bzr.design.InfoBanner
import noox.bzr.design.LinkButton
import noox.bzr.design.PrimaryButton
import noox.bzr.design.R
import noox.bzr.design.SecondaryButton
import noox.bzr.design.SecureTextField
import noox.bzr.design.generated.DesignColors
import noox.bzr.design.generated.DesignSpace
import noox.bzr.design.generated.DesignType

@Composable
fun CustomerAuthFlow(viewModel: AuthViewModel, onAuthenticated: () -> Unit = {}) {
    val state = viewModel.state
    LaunchedEffect(state.route) {
        when (state.route) {
            "SCR-C12" -> viewModel.open("SCR-C12")
            "SCR-C10" -> viewModel.open("SCR-C10")
            "SCR-C01" -> onAuthenticated()
        }
    }
    BzrTheme {
        when (state.screen) {
            "SCR-C11" -> CustomerRegisterScreen(state, viewModel::update, viewModel::submit, { viewModel.open("SCR-C10") }, { viewModel.open("SCR-C10") })
            "SCR-C12" -> CustomerEmailVerificationScreen(state, viewModel::checkVerification, viewModel::resendVerification, viewModel::logout)
            "SCR-C13" -> CustomerPasswordRecoveryScreen(state, viewModel::update, viewModel::submit, { viewModel.open("SCR-C10") }, { viewModel.open("SCR-C10") })
            else -> CustomerLoginScreen(state, viewModel::update, viewModel::submit, { viewModel.open("SCR-C11") }, { viewModel.open("SCR-C13") })
        }
    }
}

@Composable
fun CustomerLoginScreen(
    state: AuthUiState,
    onFieldChange: ((String, String) -> Unit)? = null,
    onSubmit: () -> Unit = {},
    onRegister: () -> Unit = {},
    onForgotPassword: () -> Unit = {},
) {
    AuthBody {
        Spacer(Modifier.height(DesignSpace.xxl))
        BzrText(stringResource(R.string.app_name), DesignType.screenTitle)
        BzrText(stringResource(R.string.auth_login_title), DesignType.sectionTitle)
        BzrText(stringResource(R.string.auth_login_body), DesignType.secondary)
        AuthMessage(state)
        AppTextField(
            stringResource(R.string.auth_email_label), stringResource(R.string.auth_email_placeholder), state.email,
            fieldState(state.email, state.emailError), state.emailError?.let { authText(it) },
            onValueChange = onFieldChange?.let { handler -> { handler("email", it) } },
        )
        SecureTextField(
            stringResource(R.string.auth_password_label), stringResource(R.string.auth_password_placeholder), state.password,
            fieldState(state.password, state.passwordError), state.passwordError?.let { authText(it) },
            onValueChange = onFieldChange?.let { handler -> { handler("password", it) } },
        )
        PrimaryButton(
            stringResource(R.string.auth_login_action),
            buttonState(state),
            onClick = onSubmit,
        )
        SecondaryButton(stringResource(R.string.auth_register_open), state.phase != AuthPhase.Loading, onClick = onRegister)
        LinkButton(stringResource(R.string.auth_password_forgot_open), onForgotPassword)
    }
}

@Composable
fun CustomerRegisterScreen(
    state: AuthUiState,
    onFieldChange: ((String, String) -> Unit)? = null,
    onSubmit: () -> Unit = {},
    onBack: () -> Unit = {},
    onLogin: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(DesignColors.surface)) {
        AppTopBar(stringResource(R.string.auth_register_title), onBack = onBack)
        AuthBody {
            AuthMessage(state)
            AppTextField(stringResource(R.string.auth_name_label), stringResource(R.string.auth_name_placeholder), state.name, fieldState(state.name, state.nameError), state.nameError?.let { authText(it) }, onValueChange = onFieldChange?.let { handler -> { handler("name", it) } })
            AppTextField(stringResource(R.string.auth_email_label), stringResource(R.string.auth_email_placeholder), state.email, fieldState(state.email, state.emailError), state.emailError?.let { authText(it) }, onValueChange = onFieldChange?.let { handler -> { handler("email", it) } })
            AppTextField(stringResource(R.string.auth_phone_label), stringResource(R.string.auth_phone_placeholder), state.phone, fieldState(state.phone, state.phoneError), state.phoneError?.let { authText(it) }, onValueChange = onFieldChange?.let { handler -> { handler("phone", it) } })
            SecureTextField(stringResource(R.string.auth_password_label), stringResource(R.string.auth_password_hint), state.password, fieldState(state.password, state.passwordError), state.passwordError?.let { authText(it) }, onValueChange = onFieldChange?.let { handler -> { handler("password", it) } })
            PrimaryButton(stringResource(R.string.auth_register_action), buttonState(state), onClick = onSubmit)
            LinkButton(stringResource(R.string.auth_login_open), onLogin)
        }
    }
}

@Composable
fun CustomerEmailVerificationScreen(
    state: AuthUiState,
    onCheck: () -> Unit = {},
    onResend: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(DesignColors.surface)) {
        AppTopBar(stringResource(R.string.auth_verify_title), onBack = onLogout)
        AuthBody {
            EmptyState(
                stringResource(R.string.auth_verify_waiting_title),
                stringResource(R.string.auth_verify_waiting_body) + "\n" + state.email,
            )
            AuthMessage(state)
            PrimaryButton(stringResource(R.string.auth_verify_check), buttonState(state), onClick = onCheck)
            SecondaryButton(stringResource(R.string.auth_verify_resend), state.canSubmit, onClick = onResend)
            LinkButton(stringResource(R.string.auth_logout_action), onLogout)
        }
    }
}

@Composable
fun CustomerPasswordRecoveryScreen(
    state: AuthUiState,
    onFieldChange: ((String, String) -> Unit)? = null,
    onSubmit: () -> Unit = {},
    onBack: () -> Unit = {},
    onLogin: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(DesignColors.surface)) {
        AppTopBar(stringResource(R.string.auth_password_forgot_title), onBack = onBack)
        AuthBody {
            BzrText(stringResource(R.string.auth_password_forgot_body), DesignType.secondary)
            AuthMessage(state)
            AppTextField(
                stringResource(R.string.auth_email_label), stringResource(R.string.auth_email_placeholder), state.email,
                fieldState(state.email, state.emailError), state.emailError?.let { authText(it) },
                onValueChange = onFieldChange?.let { handler -> { handler("email", it) } },
            )
            PrimaryButton(stringResource(R.string.auth_password_forgot_action), buttonState(state), onClick = onSubmit)
            LinkButton(stringResource(R.string.auth_login_open), onLogin)
        }
    }
}

@Composable
private fun AuthBody(content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(DesignSpace.m),
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = DesignSpace.screenHorizontal, vertical = DesignSpace.l),
    ) { content() }
}

@Composable
private fun AuthMessage(state: AuthUiState) {
    state.messageKey?.let { key ->
        if (state.phase == AuthPhase.Error) {
            ErrorState(stringResource(R.string.error_title), authText(key))
        } else {
            InfoBanner(authText(key))
        }
    }
}

private fun fieldState(value: String, error: String?): FieldVisualState = when {
    error != null -> FieldVisualState.Error
    value.isEmpty() -> FieldVisualState.Empty
    else -> FieldVisualState.Filled
}

private fun buttonState(state: AuthUiState): ButtonVisualState = when {
    state.phase == AuthPhase.Loading -> ButtonVisualState.Loading
    !state.canSubmit -> ButtonVisualState.Disabled
    else -> ButtonVisualState.Normal
}

@Composable
private fun authText(key: String): String = stringResource(
    when (key) {
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
    },
)
