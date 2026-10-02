import Foundation
import Observation

public enum AuthPhase: String, Codable, Sendable {
    case editing
    case loading
    case waiting
    case success
    case error
}

public struct AuthUIState: Equatable, Sendable {
    public var screen: String
    public var phase: AuthPhase
    public var name: String
    public var email: String
    public var phone: String
    public var password: String
    public var canSubmit: Bool
    public var nameError: String?
    public var emailError: String?
    public var phoneError: String?
    public var passwordError: String?
    public var messageKey: String?
    public var route: String?

    public init(
        screen: String,
        phase: AuthPhase,
        name: String = "",
        email: String = "",
        phone: String = "",
        password: String = "",
        canSubmit: Bool = true,
        nameError: String? = nil,
        emailError: String? = nil,
        phoneError: String? = nil,
        passwordError: String? = nil,
        messageKey: String? = nil,
        route: String? = nil
    ) {
        self.screen = screen
        self.phase = phase
        self.name = name
        self.email = email
        self.phone = phone
        self.password = password
        self.canSubmit = canSubmit
        self.nameError = nameError
        self.emailError = emailError
        self.phoneError = phoneError
        self.passwordError = passwordError
        self.messageKey = messageKey
        self.route = route
    }
}

public enum AuthLogic {
    private static let emailPattern = "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$"
    private static let phonePattern = "^01[0-9]{9}$"

    public static func reduce(screen: String, input: [String: AuthInputValue]) -> AuthUIState {
        switch screen {
        case "SCR-C10": login(input)
        case "SCR-C11": register(input)
        case "SCR-C12": verify(input)
        case "SCR-C13": forgot(input)
        default: preconditionFailure("Unknown auth screen: \(screen)")
        }
    }

    private static func login(_ input: [String: AuthInputValue]) -> AuthUIState {
        let email = input.text("email")
        let password = input.text("password")
        let emailError = emailError(email)
        let passwordError = password.isEmpty ? "auth.validation.password_required" : nil
        let valid = emailError == nil && passwordError == nil
        var state = AuthUIState(
            screen: "SCR-C10", phase: .editing, email: email, password: password,
            canSubmit: valid, emailError: emailError, passwordError: passwordError
        )
        switch input.text("event") {
        case "submit" where valid:
            state.phase = .loading
            state.canSubmit = false
        case "api_error":
            state.phase = .error
            switch input.text("error_code") {
            case "ACCOUNT_BLOCKED": state.messageKey = "auth.error.blocked"
            case "RATE_LIMITED": state.messageKey = "auth.error.rate_limited"
            default: state.messageKey = "auth.error.credentials"
            }
        case "network_error":
            state.phase = .error
            state.messageKey = "error.body"
        case "login_succeeded":
            state.phase = .success
            state.canSubmit = false
            state.route = input.bool("is_verified") ? "SCR-C01" : "SCR-C12"
        default: break
        }
        return state
    }

    private static func register(_ input: [String: AuthInputValue]) -> AuthUIState {
        let name = input.text("name")
        let email = input.text("email")
        let phone = input.text("phone")
        let password = input.text("password")
        let nameError = name.isEmpty ? "auth.validation.name_required" : nil
        let emailValidation = emailError(email)
        let phoneError = matches(phone, pattern: phonePattern) ? nil : "auth.validation.phone_invalid"
        let passwordError = password.count < 8 ? "auth.validation.password_short" : nil
        let valid = [nameError, emailValidation, phoneError, passwordError].allSatisfy { $0 == nil }
        var state = AuthUIState(
            screen: "SCR-C11", phase: .editing, name: name, email: email, phone: phone,
            password: password,
            canSubmit: valid, nameError: nameError, emailError: emailValidation, phoneError: phoneError,
            passwordError: passwordError
        )
        switch input.text("event") {
        case "submit" where valid:
            state.phase = .loading
            state.canSubmit = false
        case "api_error":
            state.phase = .error
            if input.text("field") == "email" { state.emailError = "auth.validation.email_used" }
            if input.text("error_code") == "RATE_LIMITED" { state.messageKey = "auth.error.rate_limited" }
        case "network_error":
            state.phase = .error
            state.messageKey = "error.body"
        case "register_succeeded":
            state.phase = .success
            state.canSubmit = false
            state.route = "SCR-C12"
        default: break
        }
        return state
    }

    private static func verify(_ input: [String: AuthInputValue]) -> AuthUIState {
        var state = AuthUIState(screen: "SCR-C12", phase: .waiting, email: input.text("email"))
        switch input.text("event") {
        case "check", "resend":
            state.phase = .loading
            state.canSubmit = false
        case "resend_succeeded":
            state.phase = .success
            state.messageKey = "auth.verify.resent"
        case "verification_checked" where input.bool("is_verified"):
            state.phase = .success
            state.canSubmit = false
            state.route = "SCR-C01"
        case "verification_checked": state.messageKey = "auth.verify.not_yet"
        case "network_error":
            state.phase = .error
            state.messageKey = "error.body"
        case "api_error":
            state.phase = .error
            state.messageKey =
                input.text("error_code") == "RATE_LIMITED" ? "auth.error.rate_limited" : "error.body"
        case "session_expired":
            state.phase = .success
            state.canSubmit = false
            state.route = "SCR-C10"
        default: break
        }
        return state
    }

    private static func forgot(_ input: [String: AuthInputValue]) -> AuthUIState {
        let email = input.text("email")
        let validation = emailError(email)
        var state = AuthUIState(
            screen: "SCR-C13", phase: .editing, email: email, canSubmit: validation == nil,
            emailError: validation)
        switch input.text("event") {
        case "submit" where validation == nil:
            state.phase = .loading
            state.canSubmit = false
        case "forgot_succeeded":
            state.phase = .success
            state.messageKey = "auth.password.forgot.sent"
        case "network_error":
            state.phase = .error
            state.messageKey = "error.body"
        case "api_error":
            state.phase = .error
            state.messageKey =
                input.text("error_code") == "RATE_LIMITED" ? "auth.error.rate_limited" : "error.body"
        default: break
        }
        return state
    }

    private static func emailError(_ value: String) -> String? {
        if value.isEmpty { return "auth.validation.email_required" }
        return matches(value, pattern: emailPattern) ? nil : "auth.validation.email_invalid"
    }

    private static func matches(_ value: String, pattern: String) -> Bool {
        value.range(of: pattern, options: [.regularExpression, .caseInsensitive]) != nil
    }
}

public enum AuthInputValue: Equatable, Sendable {
    case text(String)
    case bool(Bool)
}

extension Dictionary where Key == String, Value == AuthInputValue {
    fileprivate func text(_ key: String) -> String {
        if case let .text(value) = self[key] { return value }
        return ""
    }

    fileprivate func bool(_ key: String) -> Bool {
        if case let .bool(value) = self[key] { return value }
        return false
    }
}

public struct AuthSession: Equatable, Sendable {
    public let token: String
    public let email: String
    public let isVerified: Bool
}

public protocol AuthAPI: Sendable {
    func login(email: String, password: String) async throws -> AuthSession
    func register(name: String, email: String, phone: String, password: String) async throws
        -> AuthSession
    func isVerified(token: String) async throws -> Bool
    func resendVerification(token: String) async throws
    func forgotPassword(email: String) async throws
    func logout(token: String) async throws
}

public struct LiveAuthAPI: AuthAPI {
    private let baseURL: URL
    private let transport: any HTTPTransport

    public init(baseURL: URL, transport: any HTTPTransport = URLSessionTransport()) {
        self.baseURL = baseURL
        self.transport = transport
    }

    public func login(email: String, password: String) async throws -> AuthSession {
        let response: AuthResponse = try await client().post(
            "auth/login", body: LoginBody(email: email, password: password))
        return response.session
    }

    public func register(name: String, email: String, phone: String, password: String) async throws
        -> AuthSession
    {
        let body = RegisterBody(name: name, email: email, phone: phone, password: password)
        let response: AuthResponse = try await client().post("auth/register", body: body)
        return response.session
    }

    public func isVerified(token: String) async throws -> Bool {
        let response: MeResponse = try await client(token: token).get("me")
        return response.user.isVerified
    }

    public func resendVerification(token: String) async throws {
        try await client(token: token).postNoContent("auth/email/resend")
    }
    public func forgotPassword(email: String) async throws {
        try await client().postNoContent("auth/password/forgot", body: EmailBody(email: email))
    }
    public func logout(token: String) async throws {
        try await client(token: token).postNoContent("auth/logout")
    }

    private func client(token: String? = nil) -> APIClient {
        APIClient(baseURL: baseURL, token: token, transport: transport)
    }
}

public protocol AuthSessionStoring: AnyObject {
    var token: String? { get set }
    var email: String? { get set }
    func clear()
}

@MainActor
@Observable
public final class AuthViewModel {
    public private(set) var state = AuthUIState(screen: "SCR-C10", phase: .editing)
    private let api: any AuthAPI
    private let session: any AuthSessionStoring

    public init(api: any AuthAPI, session: any AuthSessionStoring) {
        self.api = api
        self.session = session
    }

    public func update(field: String, value: String) {
        state = AuthLogic.reduce(
            screen: state.screen,
            input: values().merging([field: .text(value), "event": .text("validate")]) { _, new in new })
    }

    public func open(_ screen: String) {
        state = AuthLogic.reduce(
            screen: screen, input: ["event": .text("show"), "email": .text(session.email ?? "")])
    }

    public func submit() async {
        state = AuthLogic.reduce(
            screen: state.screen, input: values().merging(["event": .text("submit")]) { _, new in new })
        guard state.phase == .loading else { return }
        do {
            switch state.screen {
            case "SCR-C10":
                accept(
                    try await api.login(email: state.email, password: state.password),
                    event: "login_succeeded")
            case "SCR-C11":
                accept(
                    try await api.register(
                        name: state.name, email: state.email, phone: state.phone, password: state.password),
                    event: "register_succeeded")
            case "SCR-C13":
                try await api.forgotPassword(email: state.email)
                transition("forgot_succeeded")
            default: break
            }
        } catch let error as APIClientError {
            let code = submitErrorCode(for: error)
            var extra: [String: AuthInputValue] = ["error_code": .text(code)]
            if state.screen == "SCR-C11", code == "VALIDATION_FAILED" { extra["field"] = .text("email") }
            transition("api_error", extra: extra)
        } catch {
            transition("network_error")
        }
    }

    private func submitErrorCode(for error: APIClientError) -> String {
        switch error {
        case .httpStatus(403): "ACCOUNT_BLOCKED"
        case .httpStatus(429): "RATE_LIMITED"
        default: "VALIDATION_FAILED"
        }
    }

    public func checkVerification() async {
        transition("check")
        do {
            transition(
                "verification_checked",
                extra: ["is_verified": .bool(try await api.isVerified(token: session.token ?? ""))])
        } catch let error as APIClientError {
            handleVerificationError(error)
        } catch {
            transition("network_error")
        }
    }

    public func resendVerification() async {
        transition("resend")
        do {
            try await api.resendVerification(token: session.token ?? "")
            transition("resend_succeeded")
        } catch let error as APIClientError {
            if error == .httpStatus(409) {
                do {
                    transition(
                        "verification_checked",
                        extra: ["is_verified": .bool(try await api.isVerified(token: session.token ?? ""))])
                } catch let verificationError as APIClientError {
                    handleVerificationError(verificationError)
                } catch {
                    transition("network_error")
                }
            } else {
                handleVerificationError(error)
            }
        } catch {
            transition("network_error")
        }
    }

    public func logout() async {
        if let token = session.token { try? await api.logout(token: token) }
        session.clear()
        open("SCR-C10")
    }

    private func accept(_ authSession: AuthSession, event: String) {
        session.token = authSession.token
        session.email = authSession.email
        transition(event, extra: ["is_verified": .bool(authSession.isVerified)])
    }

    private func handleVerificationError(_ error: APIClientError) {
        if error == .httpStatus(401) {
            session.clear()
            transition("session_expired")
        } else {
            transition(
                "api_error",
                extra: ["error_code": .text(error == .httpStatus(429) ? "RATE_LIMITED" : "HTTP_ERROR")])
        }
    }

    private func transition(_ event: String, extra: [String: AuthInputValue] = [:]) {
        state = AuthLogic.reduce(
            screen: state.screen,
            input: values().merging(extra.merging(["event": .text(event)]) { _, new in new }) { _, new in
                new
            })
    }

    private func values() -> [String: AuthInputValue] {
        [
            "name": .text(state.name), "email": .text(state.email), "phone": .text(state.phone),
            "password": .text(state.password),
        ]
    }
}

private struct LoginBody: Encodable {
    let email: String
    let password: String
}
private struct RegisterBody: Encodable {
    let name: String
    let email: String
    let phone: String
    let password: String
}
private struct EmailBody: Encodable { let email: String }
private struct AuthUser: Decodable, Sendable {
    let email: String
    let isVerified: Bool
}
private struct AuthResponse: Decodable, Sendable {
    let user: AuthUser
    let token: String
    var session: AuthSession {
        AuthSession(token: token, email: user.email, isVerified: user.isVerified)
    }
}
private struct MeResponse: Decodable, Sendable { let user: AuthUser }
