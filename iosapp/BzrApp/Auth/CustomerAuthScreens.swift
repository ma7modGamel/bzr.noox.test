import BzrCore
import DesignSystem
import SwiftUI

public struct CustomerAuthFlow: View {
    @Bindable private var viewModel: AuthViewModel
    private let onAuthenticated: () -> Void

    public init(viewModel: AuthViewModel, onAuthenticated: @escaping () -> Void = {}) {
        self.viewModel = viewModel
        self.onAuthenticated = onAuthenticated
    }

    public var body: some View {
        BzrTheme {
            Group {
                switch viewModel.state.screen {
                case "SCR-C11":
                    CustomerRegisterScreen(
                        state: viewModel.state, onFieldChange: viewModel.update,
                        onSubmit: { Task { await viewModel.submit() } },
                        onBack: { viewModel.open("SCR-C10") }, onLogin: { viewModel.open("SCR-C10") }
                    )
                case "SCR-C12":
                    CustomerEmailVerificationScreen(
                        state: viewModel.state,
                        onCheck: { Task { await viewModel.checkVerification() } },
                        onResend: { Task { await viewModel.resendVerification() } },
                        onLogout: { Task { await viewModel.logout() } }
                    )
                case "SCR-C13":
                    CustomerPasswordRecoveryScreen(
                        state: viewModel.state, onFieldChange: viewModel.update,
                        onSubmit: { Task { await viewModel.submit() } },
                        onBack: { viewModel.open("SCR-C10") }, onLogin: { viewModel.open("SCR-C10") }
                    )
                default:
                    CustomerLoginScreen(
                        state: viewModel.state, onFieldChange: viewModel.update,
                        onSubmit: { Task { await viewModel.submit() } },
                        onRegister: { viewModel.open("SCR-C11") },
                        onForgotPassword: { viewModel.open("SCR-C13") }
                    )
                }
            }
            .onChange(of: viewModel.state.route) { route in
                if route == "SCR-C12" { viewModel.open("SCR-C12") }
                if route == "SCR-C10" { viewModel.open("SCR-C10") }
                if route == "SCR-C01" { onAuthenticated() }
            }
        }
    }
}

public struct CustomerLoginScreen: View {
    private let state: AuthUIState
    private let onFieldChange: ((String, String) -> Void)?
    private let onSubmit: () -> Void
    private let onRegister: () -> Void
    private let onForgotPassword: () -> Void

    public init(
        state: AuthUIState,
        onFieldChange: ((String, String) -> Void)? = nil,
        onSubmit: @escaping () -> Void = {},
        onRegister: @escaping () -> Void = {},
        onForgotPassword: @escaping () -> Void = {}
    ) {
        self.state = state
        self.onFieldChange = onFieldChange
        self.onSubmit = onSubmit
        self.onRegister = onRegister
        self.onForgotPassword = onForgotPassword
    }

    public var body: some View {
        AuthBody {
            Color.clear.frame(height: DesignSpace.xxl)
            BzrText(bzrString("app.name"), style: DesignType.screenTitle)
            BzrText(bzrString("auth.login.title"), style: DesignType.sectionTitle)
            BzrText(bzrString("auth.login.body"), style: DesignType.secondary)
            authMessage(state: state)
            AppTextField(
                label: bzrString("auth.email.label"), placeholder: bzrString("auth.email.placeholder"),
                value: state.email,
                state: fieldState(state.email, error: state.emailError), error: localized(state.emailError),
                onValueChange: fieldHandler("email", onFieldChange)
            )
            SecureTextField(
                label: bzrString("auth.password.label"),
                placeholder: bzrString("auth.password.placeholder"), value: state.password,
                state: fieldState(state.password, error: state.passwordError),
                error: localized(state.passwordError),
                onValueChange: fieldHandler("password", onFieldChange)
            )
            PrimaryButton(
                text: bzrString("auth.login.action"), state: buttonState(state), onClick: onSubmit)
            SecondaryButton(
                text: bzrString("auth.register.open"), enabled: state.phase != .loading, onClick: onRegister
            )
            LinkButton(text: bzrString("auth.password.forgot.open"), onClick: onForgotPassword)
        }
    }
}

public struct CustomerRegisterScreen: View {
    private let state: AuthUIState
    private let onFieldChange: ((String, String) -> Void)?
    private let onSubmit: () -> Void
    private let onBack: () -> Void
    private let onLogin: () -> Void

    public init(
        state: AuthUIState,
        onFieldChange: ((String, String) -> Void)? = nil,
        onSubmit: @escaping () -> Void = {},
        onBack: @escaping () -> Void = {},
        onLogin: @escaping () -> Void = {}
    ) {
        self.state = state
        self.onFieldChange = onFieldChange
        self.onSubmit = onSubmit
        self.onBack = onBack
        self.onLogin = onLogin
    }

    public var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: bzrString("auth.register.title"), onBack: onBack)
            AuthBody {
                authMessage(state: state)
                AppTextField(
                    label: bzrString("auth.name.label"), placeholder: bzrString("auth.name.placeholder"),
                    value: state.name, state: fieldState(state.name, error: state.nameError),
                    error: localized(state.nameError), onValueChange: fieldHandler("name", onFieldChange))
                AppTextField(
                    label: bzrString("auth.email.label"), placeholder: bzrString("auth.email.placeholder"),
                    value: state.email,
                    state: fieldState(state.email, error: state.emailError),
                    error: localized(state.emailError), onValueChange: fieldHandler("email", onFieldChange))
                AppTextField(
                    label: bzrString("auth.phone.label"), placeholder: bzrString("auth.phone.placeholder"),
                    value: state.phone,
                    state: fieldState(state.phone, error: state.phoneError),
                    error: localized(state.phoneError), onValueChange: fieldHandler("phone", onFieldChange))
                SecureTextField(
                    label: bzrString("auth.password.label"), placeholder: bzrString("auth.password.hint"),
                    value: state.password,
                    state: fieldState(state.password, error: state.passwordError),
                    error: localized(state.passwordError),
                    onValueChange: fieldHandler("password", onFieldChange))
                PrimaryButton(
                    text: bzrString("auth.register.action"), state: buttonState(state), onClick: onSubmit)
                LinkButton(text: bzrString("auth.login.open"), onClick: onLogin)
            }
        }
        .background(DesignColors.surface)
    }
}

public struct CustomerEmailVerificationScreen: View {
    private let state: AuthUIState
    private let onCheck: () -> Void
    private let onResend: () -> Void
    private let onLogout: () -> Void

    public init(
        state: AuthUIState, onCheck: @escaping () -> Void = {}, onResend: @escaping () -> Void = {},
        onLogout: @escaping () -> Void = {}
    ) {
        self.state = state
        self.onCheck = onCheck
        self.onResend = onResend
        self.onLogout = onLogout
    }

    public var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: bzrString("auth.verify.title"), onBack: onLogout)
            AuthBody {
                EmptyState(
                    title: bzrString("auth.verify.waiting.title"),
                    body: bzrString("auth.verify.waiting.body") + "\n" + state.email
                )
                authMessage(state: state)
                PrimaryButton(
                    text: bzrString("auth.verify.check"), state: buttonState(state), onClick: onCheck)
                SecondaryButton(
                    text: bzrString("auth.verify.resend"), enabled: state.canSubmit, onClick: onResend)
                LinkButton(text: bzrString("auth.logout.action"), onClick: onLogout)
            }
        }
        .background(DesignColors.surface)
    }
}

public struct CustomerPasswordRecoveryScreen: View {
    private let state: AuthUIState
    private let onFieldChange: ((String, String) -> Void)?
    private let onSubmit: () -> Void
    private let onBack: () -> Void
    private let onLogin: () -> Void

    public init(
        state: AuthUIState,
        onFieldChange: ((String, String) -> Void)? = nil,
        onSubmit: @escaping () -> Void = {},
        onBack: @escaping () -> Void = {},
        onLogin: @escaping () -> Void = {}
    ) {
        self.state = state
        self.onFieldChange = onFieldChange
        self.onSubmit = onSubmit
        self.onBack = onBack
        self.onLogin = onLogin
    }

    public var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: bzrString("auth.password.forgot.title"), onBack: onBack)
            AuthBody {
                BzrText(bzrString("auth.password.forgot.body"), style: DesignType.secondary)
                authMessage(state: state)
                AppTextField(
                    label: bzrString("auth.email.label"), placeholder: bzrString("auth.email.placeholder"),
                    value: state.email,
                    state: fieldState(state.email, error: state.emailError),
                    error: localized(state.emailError),
                    onValueChange: fieldHandler("email", onFieldChange)
                )
                PrimaryButton(
                    text: bzrString("auth.password.forgot.action"), state: buttonState(state),
                    onClick: onSubmit)
                LinkButton(text: bzrString("auth.login.open"), onClick: onLogin)
            }
        }
        .background(DesignColors.surface)
    }
}

private struct AuthBody<Content: View>: View {
    private let content: Content

    init(@ViewBuilder content: () -> Content) { self.content = content() }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DesignSpace.m) { content }
                .padding(.horizontal, DesignSpace.screenHorizontal)
                .padding(.vertical, DesignSpace.l)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DesignColors.surface)
    }
}

@ViewBuilder
private func authMessage(state: AuthUIState) -> some View {
    if let key = state.messageKey {
        if state.phase == .error {
            ErrorState(title: bzrString("error.title"), body: bzrString(key))
        } else {
            InfoBanner(text: bzrString(key))
        }
    }
}

private func localized(_ key: String?) -> String? {
    if key == "auth.error.rate_limited" { return bzrString("auth.error.rate_limited") }
    return key.map(bzrString)
}
private func fieldHandler(_ field: String, _ handler: ((String, String) -> Void)?) -> (
    (String) -> Void
)? {
    guard let handler else { return nil }
    return { handler(field, $0) }
}
private func fieldState(_ value: String, error: String?) -> FieldVisualState {
    error != nil ? .error : (value.isEmpty ? .empty : .filled)
}
private func buttonState(_ state: AuthUIState) -> ButtonVisualState {
    state.phase == .loading ? .loading : (state.canSubmit ? .normal : .disabled)
}
