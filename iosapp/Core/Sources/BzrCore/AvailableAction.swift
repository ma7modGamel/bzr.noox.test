public enum AvailableAction: String, Codable, CaseIterable, Sendable {
    case acceptOffer = "accept_offer"
    case editRequest = "edit_request"
    case republish
    case cancel
    case changePaymentMethod = "change_payment_method"
    case payElectronic = "pay_electronic"
    case approveProposal = "approve_proposal"
    case rejectProposal = "reject_proposal"
    case confirmCompletion = "confirm_completion"
    case openDispute = "open_dispute"
    case rate
    case shareVisit = "share_visit"
    case call
    case chat
    case reportProvider = "report_provider"
    case submitOffer = "submit_offer"
    case withdrawOffer = "withdraw_offer"
    case startTrip = "start_trip"
    case backOut = "back_out"
    case markArrived = "mark_arrived"
    case startWork = "start_work"
    case submitExecutionQuote = "submit_execution_quote"
    case completeInspectionOnly = "complete_inspection_only"
    case submitProposal = "submit_proposal"
    case withdrawProposal = "withdraw_proposal"
    case completeWork = "complete_work"
    case reportUnable = "report_unable"
    case reportNoShow = "report_no_show"
    case confirmCash = "confirm_cash"
    case rateCustomer = "rate_customer"
    case navigate

    public var textKey: TextKey {
        TextKey("action.\(rawValue)")
    }
}

public struct ActionBinding: Equatable, Sendable {
    public let action: AvailableAction
    public let textKey: TextKey

    public init(action: AvailableAction) {
        self.action = action
        textKey = action.textKey
    }

    public static func bind(_ actions: [AvailableAction]) -> [ActionBinding] {
        actions.map(ActionBinding.init)
    }
}
