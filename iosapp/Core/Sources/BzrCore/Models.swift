import Foundation

public enum OrderStatus: String, Codable, Sendable {
    case open = "OPEN"
    case confirmed = "CONFIRMED"
    case onTheWay = "ON_THE_WAY"
    case arrived = "ARRIVED"
    case awaitingQuoteApproval = "AWAITING_QUOTE_APPROVAL"
    case inProgress = "IN_PROGRESS"
    case awaitingPayment = "AWAITING_PAYMENT"
    case awaitingConfirmation = "AWAITING_CONFIRMATION"
    case closed = "CLOSED"
    case disputed = "DISPUTED"
    case cancelled = "CANCELLED"
    case expired = "EXPIRED"
}

public enum StepperState: String, Codable, Sendable {
    case done
    case active
    case pending
    case onHold = "on_hold"
}

public struct StatusStep: Codable, Equatable, Sendable {
    public let key: String
    public let state: StepperState

    public init(key: String, state: StepperState) {
        self.key = key
        self.state = state
    }
}

public struct Deadlines: Codable, Equatable, Sendable {
    public let offersCloseAt: Date?
    public let selectionDeadlineAt: Date?
    public let proposalExpiresAt: Date?
    public let noShowAllowedAt: Date?
    public let autoCloseAt: Date?

    enum CodingKeys: String, CodingKey {
        case offersCloseAt = "offers_close_at"
        case selectionDeadlineAt = "selection_deadline_at"
        case proposalExpiresAt = "proposal_expires_at"
        case noShowAllowedAt = "no_show_allowed_at"
        case autoCloseAt = "auto_close_at"
    }

    public init(
        offersCloseAt: Date?,
        selectionDeadlineAt: Date?,
        proposalExpiresAt: Date?,
        noShowAllowedAt: Date?,
        autoCloseAt: Date?
    ) {
        self.offersCloseAt = offersCloseAt
        self.selectionDeadlineAt = selectionDeadlineAt
        self.proposalExpiresAt = proposalExpiresAt
        self.noShowAllowedAt = noShowAllowedAt
        self.autoCloseAt = autoCloseAt
    }
}

public struct OrderPresentation: Codable, Equatable, Sendable {
    public let id: Int
    public let number: String
    public let status: OrderStatus
    public let displayStatus: String
    public let availableActions: [AvailableAction]
    public let deadlines: Deadlines
    public let stepper: [StatusStep]?

    enum CodingKeys: String, CodingKey {
        case id, number, status, deadlines, stepper
        case displayStatus = "display_status"
        case availableActions = "available_actions"
    }

    public init(
        id: Int,
        number: String,
        status: OrderStatus,
        displayStatus: String,
        availableActions: [AvailableAction],
        deadlines: Deadlines,
        stepper: [StatusStep]?
    ) {
        self.id = id
        self.number = number
        self.status = status
        self.displayStatus = displayStatus
        self.availableActions = availableActions
        self.deadlines = deadlines
        self.stepper = stepper
    }
}

public struct APIEnvelope<Value: Codable & Equatable & Sendable>: Codable, Equatable, Sendable {
    public let data: Value

    public init(data: Value) {
        self.data = data
    }
}

public enum BzrJSON {
    public static func decoder() -> JSONDecoder {
        let decoder = JSONDecoder()
        // Models declare their wire keys in CodingKeys; converting keys first would hide every snake_case key
        // ("display_status" never matches the converted "displayStatus"), so keys are read as sent.
        decoder.dateDecodingStrategy = .iso8601
        return decoder
    }

    public static func encoder() -> JSONEncoder {
        let encoder = JSONEncoder()
        encoder.keyEncodingStrategy = .convertToSnakeCase
        encoder.dateEncodingStrategy = .iso8601
        return encoder
    }
}

/// The server sends order numbers as integers (`"number": 1042`); the screens show them as text.
/// Accepts a number or a string (or nothing) and keeps it as text, so decoding never fails on the type.
@propertyWrapper
public struct FlexibleString: Codable, Equatable, Sendable {
    public var wrappedValue: String?

    public init(wrappedValue: String?) {
        self.wrappedValue = wrappedValue
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if container.decodeNil() {
            wrappedValue = nil
        } else if let text = try? container.decode(String.self) {
            wrappedValue = text
        } else if let whole = try? container.decode(Int64.self) {
            wrappedValue = String(whole)
        } else {
            wrappedValue = String(try container.decode(Double.self))
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(wrappedValue)
    }
}

extension KeyedDecodingContainer {
    /// A missing key decodes as nil, as it did for the plain `String?` it replaces.
    public func decode(_ type: FlexibleString.Type, forKey key: Key) throws -> FlexibleString {
        try decodeIfPresent(type, forKey: key) ?? FlexibleString(wrappedValue: nil)
    }
}

/// Decimal columns (coordinates, money) arrive as strings (`"30.0600000"`); accepts a number or numeric text.
@propertyWrapper
public struct FlexibleDouble: Codable, Equatable, Sendable {
    public var wrappedValue: Double?

    public init(wrappedValue: Double?) {
        self.wrappedValue = wrappedValue
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if container.decodeNil() {
            wrappedValue = nil
        } else if let number = try? container.decode(Double.self) {
            wrappedValue = number
        } else {
            let text = try container.decode(String.self)
            guard let number = Double(text) else {
                throw DecodingError.dataCorruptedError(
                    in: container, debugDescription: "Expected a decimal number or numeric text.")
            }
            wrappedValue = number
        }
    }

    public func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(wrappedValue)
    }
}

extension KeyedDecodingContainer {
    /// A missing key decodes as nil, as it did for the plain `Double?` it replaces.
    public func decode(_ type: FlexibleDouble.Type, forKey key: Key) throws -> FlexibleDouble {
        try decodeIfPresent(type, forKey: key) ?? FlexibleDouble(wrappedValue: nil)
    }
}
