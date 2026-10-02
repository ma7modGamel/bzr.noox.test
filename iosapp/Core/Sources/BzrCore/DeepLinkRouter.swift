import Foundation

/// Where a notification link opens (17 §الروابط العميقة، DEC-058). `orderID` is 0 when the target has none.
public struct DeepLinkTarget: Equatable, Sendable {
    public static let customer = "CUSTOMER"
    public static let provider = "PROVIDER"
    public static let customerNotifications = DeepLinkTarget(mode: customer, target: "SCR-C32")

    public let mode: String
    public let target: String
    public let orderID: Int

    public init(mode: String, target: String, orderID: Int = 0) {
        self.mode = mode
        self.target = target
        self.orderID = orderID
    }
}

/// Matches a server link against the closed list in 17. The app never builds a link from a code, and any link
/// it does not know opens C32. `providerStatus` is ACTIVE, PENDING, or NONE for the signed-in account.
public enum DeepLinkRouter {
    private static let scheme = "bremo://"

    public static func route(_ link: String, providerStatus: String) -> DeepLinkTarget {
        guard var path = self.path(of: link) else { return .customerNotifications }
        if let index = path.firstIndex(where: { $0 == "?" || $0 == "#" }) { path = String(path[..<index]) }
        let parts = path.split(separator: "/").map(String.init)
        if parts.first == "provider" {
            return provider(Array(parts.dropFirst()), providerStatus: providerStatus)
        }
        return customer(parts) ?? .customerNotifications
    }

    private static func path(of link: String) -> String? {
        if link.hasPrefix(scheme) { return String(link.dropFirst(scheme.count)) }
        guard link.hasPrefix("https://") else { return nil }
        let rest = link.dropFirst("https://".count)
        guard let slash = rest.firstIndex(of: "/"), !rest[..<slash].isEmpty else { return nil }
        let afterHost = rest[slash...]
        guard afterHost.hasPrefix("/app/") else { return nil }
        return String(afterHost.dropFirst("/app/".count))
    }

    private static func customer(_ parts: [String]) -> DeepLinkTarget? {
        if parts == ["notifications"] { return DeepLinkTarget(mode: DeepLinkTarget.customer, target: "SCR-C32") }
        guard parts.first == "orders", parts.count >= 2, parts.count <= 3, let id = positiveID(parts[1]) else {
            return nil
        }
        let target: String
        switch parts.count == 3 ? parts[2] : nil {
        case nil: target = "ORDER"
        case "offers": target = "SCR-C06"
        case "chat": target = "SCR-C19"
        case "rating": target = "SCR-C24"
        case "dispute": target = "SCR-C27"
        default: return nil
        }
        return DeepLinkTarget(mode: DeepLinkTarget.customer, target: target, orderID: id)
    }

    private static func provider(_ parts: [String], providerStatus: String) -> DeepLinkTarget {
        if parts == ["application"], ["ACTIVE", "PENDING"].contains(providerStatus) {
            return DeepLinkTarget(mode: DeepLinkTarget.provider, target: "SCR-P07")
        }
        guard providerStatus == "ACTIVE" else { return .customerNotifications }
        let notifications = DeepLinkTarget(mode: DeepLinkTarget.provider, target: "SCR-C32")
        switch parts.first {
        case "offers":
            return parts.count == 1 ? DeepLinkTarget(mode: DeepLinkTarget.provider, target: "SCR-P11") : notifications
        case "earnings":
            return parts.count == 1 ? DeepLinkTarget(mode: DeepLinkTarget.provider, target: "SCR-P18") : notifications
        case "requests":
            guard parts.count == 2, let id = positiveID(parts[1]) else { return notifications }
            return DeepLinkTarget(mode: DeepLinkTarget.provider, target: "SCR-P09", orderID: id)
        case "orders":
            return providerOrder(Array(parts.dropFirst())) ?? notifications
        default:
            return notifications
        }
    }

    private static func providerOrder(_ parts: [String]) -> DeepLinkTarget? {
        guard !parts.isEmpty, parts.count <= 2, let id = positiveID(parts[0]) else { return nil }
        let target: String
        switch parts.count == 2 ? parts[1] : nil {
        case nil: target = "PROVIDER_ORDER"
        case "chat": target = "SCR-C19"
        case "rating": target = "SCR-P17"
        default: return nil
        }
        return DeepLinkTarget(mode: DeepLinkTarget.provider, target: target, orderID: id)
    }

    private static func positiveID(_ value: String) -> Int? {
        guard let id = Int(value), id > 0 else { return nil }
        return id
    }
}
