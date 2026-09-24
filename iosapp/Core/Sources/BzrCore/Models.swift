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
        decoder.keyDecodingStrategy = .convertFromSnakeCase
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
