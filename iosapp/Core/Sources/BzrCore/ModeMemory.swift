import Foundation

/// Where to start after sign-in: a provider screen, the customer flow, or the pending link (DEC-067).
public struct ModeRestore: Equatable, Sendable {
    public let target: String
    public let forget: Bool

    public init(target: String, forget: Bool) {
        self.target = target
        self.forget = forget
    }
}

/// DEC-067: the last mode each account used on this device, restored on the next launch or sign-in.
public enum ModeMemory {
    public static let customer = "CUSTOMER"
    public static let provider = "PROVIDER"
    public static let link = "LINK"

    /// `providerStatus` is ACTIVE, PENDING, NONE (no provider profile) or UNKNOWN (the check failed).
    public static func restoreTarget(lastMode: String?, providerStatus: String, hasPendingLink: Bool)
        -> ModeRestore
    {
        if hasPendingLink { return ModeRestore(target: link, forget: false) }
        guard lastMode == provider else { return ModeRestore(target: customer, forget: false) }
        switch providerStatus {
        case "ACTIVE": return ModeRestore(target: "SCR-P08", forget: false)
        case "PENDING": return ModeRestore(target: "SCR-P07", forget: false)
        case "NONE": return ModeRestore(target: customer, forget: true)
        default: return ModeRestore(target: customer, forget: false)
        }
    }
}

/// Kept per account (by email), so another account on the same phone starts from its own mode.
public struct ModeMemoryStore: @unchecked Sendable {
    private let defaults: UserDefaults

    public init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    public func lastMode(email: String?) -> String? {
        email.flatMap { defaults.string(forKey: key($0)) }
    }

    public func remember(email: String?, mode: String) {
        guard let email, !email.trimmingCharacters(in: .whitespaces).isEmpty else { return }
        defaults.set(mode, forKey: key(email))
    }

    private func key(_ email: String) -> String {
        "bremo.lastMode." + email.trimmingCharacters(in: .whitespaces).lowercased()
    }
}
