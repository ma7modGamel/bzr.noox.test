public struct TextKey: RawRepresentable, Hashable, Codable, Sendable, ExpressibleByStringLiteral {
    public let rawValue: String

    public init(rawValue: String) {
        self.rawValue = rawValue
    }

    public init(_ rawValue: String) {
        self.rawValue = rawValue
    }

    public init(stringLiteral value: StringLiteralType) {
        rawValue = value
    }

    public static func displayStatus(actor: String, status: OrderStatus) -> TextKey {
        TextKey("order.status.\(actor).\(status.rawValue)")
    }

    public static func step(_ key: String) -> TextKey {
        TextKey("order.step.\(key)")
    }
}
