import Foundation
import Observation

// This file deliberately keeps the provider API contracts, pure reducer, and Linux-testable coordinator together.
// swiftlint:disable file_length

public enum ProviderPhase: String, Equatable, Sendable {
    case empty, loading, content, error
}

public enum ProviderInputValue: Equatable, Sendable {
    case text(String)
    case bool(Bool)
    case integer(Int)
    case strings([String])
}

public struct ProviderUIState: Equatable, Sendable {
    public let screen: String
    public let phase: ProviderPhase
    public let canContinue: Bool
    public let messageKey: String?
    public let fieldErrors: [String]
    public let visibleActions: [String]
    public let items: [String]
    public let options: [String]
    public let secondaryOptions: [String]
    public let selectedCount: Int
    public let selectedPrimaryIndices: [Int]
    public let selectedSecondaryIndices: [Int]
    public let isBusy: Bool
    public let hasProfilePhoto: Bool
    public let hasIDFront: Bool
    public let hasIDBack: Bool
    public let fieldValues: [String]
    public let availableNow: Bool
    public let activeOrderID: Int?
    public let displayStatus: String
    public let stepStates: [String]
    public let selectedOptionIndex: Int
    public let optionCodes: [String]
    public let showPriceGuide: Bool
    public let showOutsideReason: Bool
    public let hasPhoto: Bool
    public let currentAction: String
    public let priceGuideMinimum: String
    public let priceGuideMaximum: String
    public let customerName: String
    public let customerRating: String
    public let itemDetails: [String]
    public let itemStates: [String]
    public let areaOptions: [String]
    public let unreadCount: Int

    public init(
        screen: String, phase: ProviderPhase, canContinue: Bool = false,
        messageKey: String? = nil, fieldErrors: [String] = [], visibleActions: [String] = [],
        items: [String] = [], options: [String] = [], secondaryOptions: [String] = [],
        selectedCount: Int = 0, selectedPrimaryIndices: [Int] = [],
        selectedSecondaryIndices: [Int] = [], isBusy: Bool = false, hasProfilePhoto: Bool = false,
        hasIDFront: Bool = false, hasIDBack: Bool = false, fieldValues: [String] = [],
        availableNow: Bool = false, activeOrderID: Int? = nil, displayStatus: String = "",
        stepStates: [String] = [], selectedOptionIndex: Int = -1, optionCodes: [String] = [],
        showPriceGuide: Bool = false, showOutsideReason: Bool = false, hasPhoto: Bool = false,
        currentAction: String = "", priceGuideMinimum: String = "", priceGuideMaximum: String = "",
        customerName: String = "", customerRating: String = "",
        itemDetails: [String] = [], itemStates: [String] = [], areaOptions: [String] = [],
        unreadCount: Int = 0
    ) {
        self.screen = screen
        self.phase = phase
        self.canContinue = canContinue
        self.messageKey = messageKey
        self.fieldErrors = fieldErrors
        self.visibleActions = visibleActions
        self.items = items
        self.options = options
        self.secondaryOptions = secondaryOptions
        self.selectedCount = selectedCount
        self.selectedPrimaryIndices = selectedPrimaryIndices
        self.selectedSecondaryIndices = selectedSecondaryIndices
        self.isBusy = isBusy
        self.hasProfilePhoto = hasProfilePhoto
        self.hasIDFront = hasIDFront
        self.hasIDBack = hasIDBack
        self.fieldValues = fieldValues
        self.availableNow = availableNow
        self.activeOrderID = activeOrderID
        self.displayStatus = displayStatus
        self.stepStates = stepStates
        self.selectedOptionIndex = selectedOptionIndex
        self.optionCodes = optionCodes
        self.showPriceGuide = showPriceGuide
        self.showOutsideReason = showOutsideReason
        self.hasPhoto = hasPhoto
        self.currentAction = currentAction
        self.priceGuideMinimum = priceGuideMinimum
        self.priceGuideMaximum = priceGuideMaximum
        self.customerName = customerName
        self.customerRating = customerRating
        self.itemDetails = itemDetails
        self.itemStates = itemStates
        self.areaOptions = areaOptions
        self.unreadCount = unreadCount
    }
}

public struct ProviderApplication: Codable, Equatable, Sendable {
    public let status: String
    public let displayStatus: String
    public let rejectionReason: String?
    public let suspensionReason: String?
    public let experienceYears: Int?
    public let bio: String?
    public let categoryIDs: [Int]?
    public let specialtyIDs: [Int]?
    public let areaIDs: [Int]?
    public let payoutMethod: String?
    public let payoutDetails: String?
    public let profilePhotoUploaded: Bool
    public let documents: ProviderDocuments
    public let availableActions: [String]

    enum CodingKeys: String, CodingKey {
        case status, bio, documents
        case displayStatus = "display_status"
        case rejectionReason = "rejection_reason"
        case suspensionReason = "suspension_reason"
        case experienceYears = "experience_years"
        case categoryIDs = "category_ids"
        case specialtyIDs = "specialty_ids"
        case areaIDs = "area_ids"
        case payoutMethod = "payout_method"
        case payoutDetails = "payout_details"
        case profilePhotoUploaded = "profile_photo_uploaded"
        case availableActions = "available_actions"
    }
}

public struct ProviderDocuments: Codable, Equatable, Sendable {
    public let idFront: Bool
    public let idBack: Bool

    enum CodingKeys: String, CodingKey {
        case idFront = "id_front"
        case idBack = "id_back"
    }
}

public struct ProviderChoice: Codable, Equatable, Sendable {
    public let id: Int
    public let name: String
}

public struct ProviderCategory: Codable, Equatable, Sendable {
    public let id: Int
    public let name: String
    public let specialties: [ProviderChoice]

    enum CodingKeys: String, CodingKey {
        case id, name
        case specialties = "problem_types"
    }
}

public struct ProviderPayoutOption: Codable, Equatable, Sendable {
    public let code: String
    public let label: String
    public let fieldLabel: String

    enum CodingKeys: String, CodingKey {
        case code, label
        case fieldLabel = "field_label"
    }
}

public struct ProviderHomeOrder: Codable, Equatable, Sendable {
    public let id: Int
    public let number: Int
    public let category: ProviderNamedValue
    public let problemType: ProviderNamedValue
    public let location: ProviderHomeLocation
    public let timing: ProviderHomeTiming
    public let displayStatus: String

    enum CodingKeys: String, CodingKey {
        case id, number, category, location, timing
        case problemType = "problem_type"
        case displayStatus = "display_status"
    }
}

public struct ProviderNamedValue: Codable, Equatable, Sendable {
    public let id: Int
    public let name: String
}

public struct ProviderHomeLocation: Codable, Equatable, Sendable {
    public let area: String?
    public let city: String?
    public let addressText: String?
    public let building: String?
    public let floor: String?
    public let apartment: String?
    public let landmark: String?
    public let lat: Double?
    public let lng: Double?

    enum CodingKeys: String, CodingKey {
        case area, city, building, floor, apartment, landmark, lat, lng
        case addressText = "address_text"
    }
}

public struct ProviderHomeTiming: Codable, Equatable, Sendable {
    public let type: String
    public let slotStart: String?

    enum CodingKeys: String, CodingKey {
        case type
        case slotStart = "slot_start"
    }
}

public struct ProviderHome: Codable, Equatable, Sendable {
    public let operatingMode: String
    public let availableNow: Bool
    public let activeOrder: ProviderHomeOrder?
    public let availableActions: [String]
    public let unreadNotifications: Int?

    enum CodingKeys: String, CodingKey {
        case operatingMode = "operating_mode"
        case availableNow = "available_now"
        case activeOrder = "active_order"
        case availableActions = "available_actions"
        case unreadNotifications = "unread_notifications"
    }
}

public struct ProviderAvailabilityUpdate: Encodable, Sendable {
    public let availableNow: Bool

    enum CodingKeys: String, CodingKey { case availableNow = "available_now" }
}

public struct ProviderReason: Codable, Equatable, Sendable {
    public let code: String
    public let label: String
}

public struct ProviderOrderCustomer: Codable, Equatable, Sendable {
    public let name: String?
    public let phone: String?
    public let ratingAverage: String?

    enum CodingKeys: String, CodingKey {
        case name, phone
        case ratingAverage = "rating_avg"
    }
}

public struct ProviderOrderAmounts: Codable, Equatable, Sendable {
    public let laborTotal: String?
    public let materialsTotal: String?
    public let finalAmount: String?
    public let paymentMethod: String?
    public let paymentMethodLabel: String?

    enum CodingKeys: String, CodingKey {
        case laborTotal = "labor_total"
        case materialsTotal = "materials_total"
        case finalAmount = "final_amount"
        case paymentMethod = "payment_method"
        case paymentMethodLabel = "payment_method_label"
    }
}

public struct ProviderOrderStep: Codable, Equatable, Sendable {
    public let key: String
    public let state: String
}

public struct ProviderPriceGuide: Codable, Equatable, Sendable {
    public let minimum: String
    public let maximum: String
    public let currency: String
}

public struct ProviderOrder: Codable, Equatable, Sendable {
    public let id: Int
    public let number: Int
    public let version: Int
    public let status: String
    public let displayStatus: String
    public let category: ProviderNamedValue
    public let problemType: ProviderNamedValue
    public let location: ProviderHomeLocation
    public let timing: ProviderHomeTiming
    public let customer: ProviderOrderCustomer?
    public let amounts: ProviderOrderAmounts
    public let stepper: [ProviderOrderStep]?
    public let availableActions: [String]
    public let executionPriceGuide: ProviderPriceGuide?
    public let priceGuideReviewRequired: Bool?
    public let conversationID: Int?
    public let description: String?
    public let pricingMode: String?
    public let pricingModeLabel: String?
    public let materialsResponsibility: String?
    public let materialsResponsibilityLabel: String?
    public let budgetAmount: String?
    public let deadlines: ProviderOrderDeadlines?

    enum CodingKeys: String, CodingKey {
        case id, number, version, status, category, location, timing, customer, amounts, stepper
        case description, deadlines
        case displayStatus = "display_status"
        case problemType = "problem_type"
        case pricingMode = "pricing_mode"
        case pricingModeLabel = "pricing_mode_label"
        case materialsResponsibility = "materials_responsibility"
        case materialsResponsibilityLabel = "materials_responsibility_label"
        case budgetAmount = "budget_amount"
        case availableActions = "available_actions"
        case executionPriceGuide = "execution_price_guide"
        case priceGuideReviewRequired = "price_guide_review_required"
        case conversationID = "conversation_id"
    }
}

public struct ProviderOrderDeadlines: Codable, Equatable, Sendable {
    public let offersCloseAt: String?
    enum CodingKeys: String, CodingKey { case offersCloseAt = "offers_close_at" }
}

public struct ProviderMarketMedia: Codable, Equatable, Sendable {
    public let id: Int
    public let type: String
}

public struct ProviderOwnOffer: Codable, Equatable, Sendable {
    public let id: Int
    public let status: String
    public let displayStatus: String
    public let price: String
    public let netAmount: String
    enum CodingKeys: String, CodingKey {
        case id, status, price
        case displayStatus = "display_status"
        case netAmount = "net_amount"
    }
}

public struct ProviderDeductibleOption: Codable, Equatable, Sendable {
    public let code: String
    public let label: String
    public let value: Bool
}

public struct ProviderOfferContext: Codable, Equatable, Sendable {
    public let minimumAmount: String
    public let commissionRate: String
    public let etaOptions: [Int]
    public let deductibleOptions: [ProviderDeductibleOption]
    public let defaultIncludesText: String
    enum CodingKeys: String, CodingKey {
        case minimumAmount = "min_amount"
        case commissionRate = "commission_rate"
        case etaOptions = "eta_options"
        case deductibleOptions = "deductible_options"
        case defaultIncludesText = "default_includes_text"
    }
}

public struct ProviderMarketRequest: Codable, Equatable, Sendable {
    public let order: ProviderOrder
    public let media: [ProviderMarketMedia]
    public let ownOffer: ProviderOwnOffer?
    public let offerContext: ProviderOfferContext
    enum CodingKeys: String, CodingKey {
        case order, media
        case ownOffer = "own_offer"
        case offerContext = "offer_context"
    }
}

public struct ProviderMarketOfferOrder: Codable, Equatable, Sendable {
    public let id: Int
    public let number: Int
    public let version: Int
    public let category: String?
    public let problemType: String?
    public let area: String?
    public let timingType: String
    public let slotStart: String?
    enum CodingKeys: String, CodingKey {
        case id, number, version, category, area
        case problemType = "problem_type"
        case timingType = "timing_type"
        case slotStart = "slot_start"
    }
}

public struct ProviderMarketOffer: Codable, Equatable, Sendable {
    public let id: Int
    public let status: String
    public let displayStatus: String
    public let price: String
    public let netAmount: String
    public let availableActions: [String]
    public let order: ProviderMarketOfferOrder
    enum CodingKeys: String, CodingKey {
        case id, status, price, order
        case displayStatus = "display_status"
        case netAmount = "net_amount"
        case availableActions = "available_actions"
    }
}

public struct ProviderOfferSubmission: Encodable, Sendable {
    public let price: String
    public let etaMinutes: Int?
    public let inspectionFeeDeductible: Bool?
    public let includesText: String
    public let note: String?
    enum CodingKeys: String, CodingKey {
        case price, note
        case etaMinutes = "eta_minutes"
        case inspectionFeeDeductible = "inspection_fee_deductible"
        case includesText = "includes_text"
    }
}

public struct ProviderEmptySubmission: Encodable, Sendable {}

public struct ProviderPortfolioItem: Codable, Equatable, Sendable {
    public let id: Int
    public let imagePath: String
    public let caption: String?
    enum CodingKeys: String, CodingKey {
        case id, caption
        case imagePath = "image_path"
    }
}

public struct ProviderProfileCategory: Codable, Equatable, Sendable {
    public let id: Int
    public let name: String
    public let specialties: [ProviderChoice]
}

public struct ProviderProfileArea: Codable, Equatable, Sendable {
    public let id: Int
    public let name: String
    public let city: String
}

public struct ProviderProfilePayload: Codable, Equatable, Sendable {
    public let name: String
    public let avatarPath: String?
    public let bio: String?
    public let experienceYears: Int
    public let ratingAverage: String?
    public let completedOrders: Int
    public let averageResponseMinutes: Int?
    public let categories: [ProviderProfileCategory]
    public let specialtyIDs: [Int]
    public let areas: [ProviderProfileArea]
    public let areaIDs: [Int]
    public let portfolio: [ProviderPortfolioItem]
    public let availableActions: [String]
    enum CodingKeys: String, CodingKey {
        case name, bio, categories, areas, portfolio
        case avatarPath = "avatar_path"
        case experienceYears = "experience_years"
        case ratingAverage = "rating_avg"
        case completedOrders = "completed_orders"
        case averageResponseMinutes = "avg_response_minutes"
        case specialtyIDs = "specialty_ids"
        case areaIDs = "area_ids"
        case availableActions = "available_actions"
    }
}

public struct ProviderEarningsSummary: Codable, Equatable, Sendable {
    public let balance: String
    public let available: String
    public let pending: String
}

public struct ProviderEarningsTransaction: Codable, Equatable, Sendable {
    public let id: String
    public let type: String
    public let title: String
    public let detail: String
}

public struct ProviderEarningsPayload: Codable, Equatable, Sendable {
    public let operatingMode: String
    public let summary: ProviderEarningsSummary
    public let transactions: [ProviderEarningsTransaction]
    enum CodingKeys: String, CodingKey {
        case summary, transactions
        case operatingMode = "operating_mode"
    }
}

public struct ProviderProfileUpdate: Encodable, Sendable {
    public let experienceYears: Int
    public let bio: String?
    public let specialtyIDs: [Int]
    public let areaIDs: [Int]
    enum CodingKeys: String, CodingKey {
        case bio
        case experienceYears = "experience_years"
        case specialtyIDs = "specialty_ids"
        case areaIDs = "area_ids"
    }
}

public struct ProviderPortfolioSubmission: Encodable, Sendable {
    public let mediaID: Int
    public let caption: String?
    enum CodingKeys: String, CodingKey {
        case caption
        case mediaID = "media_id"
    }
}

public struct ProviderRatingSubmission: Encodable, Sendable { public let stars: Int }
public struct ProviderMessageSubmission: Encodable, Sendable {
    public let body: String?
    public let mediaIDs: [Int]
    enum CodingKeys: String, CodingKey {
        case body
        case mediaIDs = "media_ids"
    }
}

public struct ProviderLocationAction: Encodable, Sendable {
    public let expectedVersion: Int
    public let lat: Double
    public let lng: Double
    enum CodingKeys: String, CodingKey {
        case expectedVersion = "expected_version"
        case lat, lng
    }
}

public struct ProviderVersionAction: Encodable, Sendable {
    public let expectedVersion: Int
    enum CodingKeys: String, CodingKey { case expectedVersion = "expected_version" }
}

public struct ProviderCashAction: Encodable, Sendable {
    public let expectedVersion: Int
    public let amount: String
    enum CodingKeys: String, CodingKey {
        case expectedVersion = "expected_version"
        case amount
    }
}

public struct ProviderReasonAction: Encodable, Sendable {
    public let expectedVersion: Int
    public let reasonCode: String
    public let note: String?
    enum CodingKeys: String, CodingKey {
        case expectedVersion = "expected_version"
        case reasonCode = "reason_code"
        case note
    }
}

public struct ProviderProposalSubmission: Encodable, Sendable {
    public let expectedVersion: Int
    public let type: String
    public let amount: String
    public let reason: String
    public let photoMediaID: Int?
    public let outsidePriceGuideReason: String?
    enum CodingKeys: String, CodingKey {
        case expectedVersion = "expected_version"
        case type, amount, reason
        case photoMediaID = "photo_media_id"
        case outsidePriceGuideReason = "outside_price_guide_reason"
    }
}

public struct ProviderApplicationSubmission: Encodable, Sendable {
    public let experienceYears: Int
    public let bio: String?
    public let categoryIDs: [Int]
    public let specialtyIDs: [Int]
    public let areaIDs: [Int]
    public let payoutMethod: String
    public let payoutDetails: String
    public let profilePhotoMediaID: Int?
    public let idFrontMediaID: Int?
    public let idBackMediaID: Int?

    enum CodingKeys: String, CodingKey {
        case bio
        case experienceYears = "experience_years"
        case categoryIDs = "category_ids"
        case specialtyIDs = "specialty_ids"
        case areaIDs = "area_ids"
        case payoutMethod = "payout_method"
        case payoutDetails = "payout_details"
        case profilePhotoMediaID = "profile_photo_media_id"
        case idFrontMediaID = "id_front_media_id"
        case idBackMediaID = "id_back_media_id"
    }
}

public protocol ProviderAPI: Sendable {
    func notifications() async throws -> [CustomerNotification]
    func markNotificationsRead(ids: [String]) async throws
    func application() async throws -> ProviderApplication
    func submit(_ body: ProviderApplicationSubmission) async throws -> ProviderApplication
    func catalog() async throws -> [ProviderCategory]
    func cities() async throws -> [ProviderChoice]
    func areas(cityID: Int) async throws -> [ProviderChoice]
    func payoutOptions() async throws -> [ProviderPayoutOption]
    func uploadImage(_ upload: CustomerMediaUpload) async throws -> Int
    func home() async throws -> ProviderHome
    func setAvailability(_ availableNow: Bool) async throws -> ProviderHome
    func availableRequests() async throws -> [ProviderOrder]
    func availableRequest(orderID: Int) async throws -> ProviderMarketRequest
    func submitOffer(orderID: Int, body: ProviderOfferSubmission) async throws -> ProviderMarketOffer
    func offers() async throws -> [ProviderMarketOffer]
    func withdrawOffer(id: Int) async throws -> ProviderMarketOffer
    func currentOrders() async throws -> [ProviderOrder]
    func pastOrders() async throws -> [ProviderOrder]
    func providerReasons() async throws -> [ProviderReason]
    func proposalTypes() async throws -> [ProviderReason]
    func startTrip(order: ProviderOrder, latitude: Double, longitude: Double) async throws
        -> ProviderOrder
    func markArrived(order: ProviderOrder, latitude: Double, longitude: Double) async throws
        -> ProviderOrder
    func startWork(order: ProviderOrder) async throws -> ProviderOrder
    func completeInspection(order: ProviderOrder) async throws -> ProviderOrder
    func completeWork(order: ProviderOrder) async throws -> ProviderOrder
    func confirmCash(order: ProviderOrder) async throws -> ProviderOrder
    func backOut(order: ProviderOrder, reasonCode: String) async throws -> ProviderOrder
    func reportUnable(order: ProviderOrder, reasonCode: String, note: String) async throws
        -> ProviderOrder
    func reportNoShow(order: ProviderOrder) async throws -> ProviderOrder
    func submitProposal(order: ProviderOrder, body: ProviderProposalSubmission) async throws
    func rateCustomer(orderID: Int, stars: Int) async throws
    func earnings() async throws -> ProviderEarningsPayload
    func profile() async throws -> ProviderProfilePayload
    func updateProfile(_ body: ProviderProfileUpdate) async throws -> ProviderProfilePayload
    func addPortfolio(_ body: ProviderPortfolioSubmission) async throws -> ProviderPortfolioItem
    func deletePortfolio(id: Int) async throws
    func updateAvatar(mediaID: Int) async throws
    func conversations() async throws -> [CustomerConversation]
    func conversationMessages(id: Int) async throws -> CustomerConversationMessages
    func sendMessage(conversationID: Int, body: String?, mediaIDs: [Int]) async throws
}

public struct LiveProviderAPI: ProviderAPI {
    private let client: APIClient

    public init(baseURL: URL, token: String, transport: any HTTPTransport = URLSessionTransport()) {
        client = APIClient(baseURL: baseURL, token: token, appMode: "PROVIDER", transport: transport)
    }

    /// C32 in provider mode: the server filters by `X-App-Mode` (DEC-058).
    public func notifications() async throws -> [CustomerNotification] {
        let response: ProviderNotificationsEnvelope = try await client.get("notifications")
        return response.data
    }

    public func markNotificationsRead(ids: [String]) async throws {
        try await client.postNoContent("notifications/read", body: ProviderNotificationReadBody(notificationIds: ids))
    }

    public func application() async throws -> ProviderApplication {
        let response: ProviderApplicationEnvelope = try await client.get("provider/application")
        return response.data
    }

    public func submit(_ body: ProviderApplicationSubmission) async throws -> ProviderApplication {
        let response: ProviderApplicationEnvelope = try await client.post(
            "provider/application", body: body)
        return response.data
    }

    public func catalog() async throws -> [ProviderCategory] {
        let response: ProviderListEnvelope<ProviderCategory> = try await client.get("catalog")
        return response.data
    }

    public func cities() async throws -> [ProviderChoice] {
        let response: ProviderListEnvelope<ProviderChoice> = try await client.get("cities")
        return response.data
    }

    public func areas(cityID: Int) async throws -> [ProviderChoice] {
        let response: ProviderListEnvelope<ProviderChoice> = try await client.get(
            "cities/\(cityID)/areas")
        return response.data
    }

    public func payoutOptions() async throws -> [ProviderPayoutOption] {
        let response: ProviderConfig = try await client.get("config")
        return response.optionLists.providerPayoutMethods
    }

    public func uploadImage(_ upload: CustomerMediaUpload) async throws -> Int {
        let response: ProviderMediaEnvelope = try await client.multipart(
            "media", field: "file", fileName: upload.fileName, mimeType: upload.mimeType,
            data: upload.data)
        return response.media.id
    }

    public func home() async throws -> ProviderHome {
        let response: ProviderHomeEnvelope = try await client.get("provider/home")
        return response.data
    }

    public func setAvailability(_ availableNow: Bool) async throws -> ProviderHome {
        let response: ProviderHomeEnvelope = try await client.put(
            "provider/availability",
            body: ProviderAvailabilityUpdate(availableNow: availableNow))
        return response.data
    }

    public func availableRequests() async throws -> [ProviderOrder] {
        let response: ProviderOrderListEnvelope = try await client.get("provider/requests")
        return response.data
    }

    public func availableRequest(orderID: Int) async throws -> ProviderMarketRequest {
        let response: ProviderMarketRequestEnvelope = try await client.get(
            "provider/requests/\(orderID)")
        return response.data
    }

    public func submitOffer(orderID: Int, body: ProviderOfferSubmission) async throws
        -> ProviderMarketOffer
    {
        let response: ProviderMarketOfferEnvelope = try await client.post(
            "provider/requests/\(orderID)/offers", body: body)
        return response.data
    }

    public func offers() async throws -> [ProviderMarketOffer] {
        let response: ProviderMarketOfferListEnvelope = try await client.get("provider/offers")
        return response.data
    }

    public func withdrawOffer(id: Int) async throws -> ProviderMarketOffer {
        let response: ProviderMarketOfferEnvelope = try await client.post(
            "provider/offers/\(id)/withdraw", body: ProviderEmptySubmission())
        return response.data
    }

    public func currentOrders() async throws -> [ProviderOrder] {
        let response: ProviderOrderListEnvelope = try await client.get("provider/orders?scope=current")
        return response.data
    }

    public func pastOrders() async throws -> [ProviderOrder] {
        let response: ProviderOrderListEnvelope = try await client.get("provider/orders?scope=past")
        return response.data
    }

    public func providerReasons() async throws -> [ProviderReason] {
        let response: ProviderConfig = try await client.get("config")
        return response.optionLists.providerCancellationReasons
    }

    public func proposalTypes() async throws -> [ProviderReason] {
        let response: ProviderConfig = try await client.get("config")
        return response.optionLists.proposalTypes
    }

    public func startTrip(order: ProviderOrder, latitude: Double, longitude: Double) async throws
        -> ProviderOrder
    {
        try await orderAction(
            "provider/orders/\(order.id)/start-trip",
            ProviderLocationAction(expectedVersion: order.version, lat: latitude, lng: longitude))
    }

    public func markArrived(order: ProviderOrder, latitude: Double, longitude: Double) async throws
        -> ProviderOrder
    {
        try await orderAction(
            "provider/orders/\(order.id)/arrived",
            ProviderLocationAction(expectedVersion: order.version, lat: latitude, lng: longitude))
    }

    public func startWork(order: ProviderOrder) async throws -> ProviderOrder {
        try await orderAction(
            "provider/orders/\(order.id)/start-work",
            ProviderVersionAction(expectedVersion: order.version))
    }

    public func completeInspection(order: ProviderOrder) async throws -> ProviderOrder {
        try await orderAction(
            "provider/orders/\(order.id)/complete-inspection-only",
            ProviderVersionAction(expectedVersion: order.version))
    }

    public func completeWork(order: ProviderOrder) async throws -> ProviderOrder {
        try await orderAction(
            "provider/orders/\(order.id)/complete", ProviderVersionAction(expectedVersion: order.version))
    }

    public func confirmCash(order: ProviderOrder) async throws -> ProviderOrder {
        try await orderAction(
            "provider/orders/\(order.id)/cash-received",
            ProviderCashAction(
                expectedVersion: order.version, amount: order.amounts.finalAmount ?? "0.00"))
    }

    public func backOut(order: ProviderOrder, reasonCode: String) async throws -> ProviderOrder {
        try await orderAction(
            "provider/orders/\(order.id)/back-out",
            ProviderReasonAction(expectedVersion: order.version, reasonCode: reasonCode, note: nil))
    }

    public func reportUnable(order: ProviderOrder, reasonCode: String, note: String) async throws
        -> ProviderOrder
    {
        try await orderAction(
            "provider/orders/\(order.id)/unable",
            ProviderReasonAction(expectedVersion: order.version, reasonCode: reasonCode, note: note))
    }

    public func reportNoShow(order: ProviderOrder) async throws -> ProviderOrder {
        try await orderAction(
            "provider/orders/\(order.id)/customer-no-show",
            ProviderVersionAction(expectedVersion: order.version))
    }

    public func submitProposal(order: ProviderOrder, body: ProviderProposalSubmission) async throws {
        let _: ProviderProposalEnvelope = try await client.post(
            "provider/orders/\(order.id)/proposals", body: body)
    }

    public func rateCustomer(orderID: Int, stars: Int) async throws {
        let _: ProviderRatingEnvelope = try await client.post(
            "provider/orders/\(orderID)/customer-rating", body: ProviderRatingSubmission(stars: stars))
    }

    public func earnings() async throws -> ProviderEarningsPayload {
        let response: ProviderEarningsEnvelope = try await client.get("provider/earnings")
        return response.data
    }

    public func profile() async throws -> ProviderProfilePayload {
        let response: ProviderProfileEnvelope = try await client.get("provider/profile")
        return response.data
    }

    public func updateProfile(_ body: ProviderProfileUpdate) async throws -> ProviderProfilePayload {
        let response: ProviderProfileEnvelope = try await client.patch("provider/profile", body: body)
        return response.data
    }

    public func addPortfolio(_ body: ProviderPortfolioSubmission) async throws
        -> ProviderPortfolioItem
    {
        let response: ProviderPortfolioEnvelope = try await client.post(
            "provider/portfolio", body: body)
        return response.data
    }

    public func deletePortfolio(id: Int) async throws {
        try await client.delete("provider/portfolio/\(id)")
    }

    public func updateAvatar(mediaID: Int) async throws {
        let _: ProviderAccountEnvelope = try await client.patch(
            "me", body: ProviderAvatarUpdate(avatarMediaID: mediaID))
    }

    public func conversations() async throws -> [CustomerConversation] {
        let response: ProviderConversationListEnvelope = try await client.get("conversations")
        return response.data
    }

    public func conversationMessages(id: Int) async throws -> CustomerConversationMessages {
        let response: ProviderConversationMessagesEnvelope = try await client.get(
            "conversations/\(id)/messages")
        return CustomerConversationMessages(
            conversation: response.conversation, messages: response.data)
    }

    public func sendMessage(conversationID: Int, body: String?, mediaIDs: [Int]) async throws {
        let _: ProviderMessageEnvelope = try await client.post(
            "conversations/\(conversationID)/messages",
            body: ProviderMessageSubmission(body: body, mediaIDs: mediaIDs))
    }

    private func orderAction<Body: Encodable & Sendable>(_ path: String, _ body: Body) async throws
        -> ProviderOrder
    {
        let response: ProviderOrderEnvelope = try await client.post(path, body: body)
        return response.data
    }
}

private struct ProviderApplicationEnvelope: Decodable, Sendable { let data: ProviderApplication }
private struct ProviderHomeEnvelope: Decodable, Sendable { let data: ProviderHome }
private struct ProviderOrderEnvelope: Decodable, Sendable { let data: ProviderOrder }
private struct ProviderOrderListEnvelope: Decodable, Sendable { let data: [ProviderOrder] }
private struct ProviderMarketRequestEnvelope: Decodable, Sendable { let data: ProviderMarketRequest }
private struct ProviderMarketOfferEnvelope: Decodable, Sendable { let data: ProviderMarketOffer }
private struct ProviderMarketOfferListEnvelope: Decodable, Sendable {
    let data: [ProviderMarketOffer]
}
private struct ProviderProposalEnvelope: Decodable, Sendable { let data: ProviderProposalResponse }
private struct ProviderRatingEnvelope: Decodable, Sendable { let data: ProviderRatingResult }
private struct ProviderRatingResult: Decodable, Sendable {
    let id: Int
    let stars: Int
}
private struct ProviderEarningsEnvelope: Decodable, Sendable { let data: ProviderEarningsPayload }
private struct ProviderProfileEnvelope: Decodable, Sendable { let data: ProviderProfilePayload }
private struct ProviderPortfolioEnvelope: Decodable, Sendable { let data: ProviderPortfolioItem }
private struct ProviderConversationListEnvelope: Decodable, Sendable {
    let data: [CustomerConversation]
}
private struct ProviderConversationMessagesEnvelope: Decodable, Sendable {
    let conversation: CustomerConversation
    let data: [CustomerMessage]
}
private struct ProviderMessageEnvelope: Decodable, Sendable { let data: CustomerMessage }
private struct ProviderAvatarUpdate: Encodable, Sendable {
    let avatarMediaID: Int
    enum CodingKeys: String, CodingKey { case avatarMediaID = "avatar_media_id" }
}
private struct ProviderAccountEnvelope: Decodable, Sendable { let user: ProviderAccountUser }
private struct ProviderAccountUser: Decodable, Sendable { let id: Int }
private struct ProviderProposalResponse: Decodable, Sendable { let id: Int }
private struct ProviderListEnvelope<Value: Decodable & Sendable>: Decodable, Sendable {
    let data: [Value]
}
private struct ProviderMediaEnvelope: Decodable, Sendable { let media: CustomerMedia }
private struct ProviderConfig: Decodable, Sendable {
    let optionLists: ProviderOptionLists
    enum CodingKeys: String, CodingKey { case optionLists = "option_lists" }
}
private struct ProviderOptionLists: Decodable, Sendable {
    let providerPayoutMethods: [ProviderPayoutOption]
    let providerCancellationReasons: [ProviderReason]
    let proposalTypes: [ProviderReason]
    enum CodingKeys: String, CodingKey {
        case providerPayoutMethods = "provider_payout_methods"
        case providerCancellationReasons = "provider_cancellation_reasons"
        case proposalTypes = "proposal_types"
    }
}

@MainActor
@Observable
// swiftlint:disable:next type_body_length
public final class ProviderViewModel {
    public private(set) var state = ProviderUIState(screen: "SCR-P01", phase: .loading)
    private let api: any ProviderAPI
    private var applicationValue: ProviderApplication?
    private var experience = ""
    private var bio = ""
    private var profilePhotoMediaID: Int?
    private var idFrontMediaID: Int?
    private var idBackMediaID: Int?
    private var hasProfilePhoto = false
    private var hasIDFront = false
    private var hasIDBack = false
    private var categories: [ProviderCategory] = []
    private var categoryIDs: Set<Int> = []
    private var specialtyIDs: Set<Int> = []
    private var citiesValue: [ProviderChoice] = []
    private var areasValue: [ProviderChoice] = []
    private var areaIDs: Set<Int> = []
    private var cityIndex = -1
    private var payoutOptionsValue: [ProviderPayoutOption] = []
    private var payoutIndex = -1
    private var payoutDetails = ""
    private var homeValue: ProviderHome?
    private var activeOrderValue: ProviderOrder?
    private var providerReasonsValue: [ProviderReason] = []
    private var proposalTypesValue: [ProviderReason] = []
    private var selectedProposalType = -1
    private var proposalAmount = ""
    private var proposalReason = ""
    private var outsidePriceGuideReason = ""
    private var proposalPhotoMediaID: Int?
    private var selectedUnableReason = -1
    private var unableNote = ""
    private var requestedUnableAction = ""
    private var ratingStars = 0
    private var workspaceProfileValue: ProviderProfilePayload?
    private var workspaceSpecialtyIDs: Set<Int> = []
    private var workspaceAreaIDs: Set<Int> = []
    private var workspaceAreas: [ProviderChoice] = []
    private var workspacePortfolio: [ProviderPortfolioItem] = []
    private var portfolioCaption = ""
    private var activeConversationID: Int?
    private var chatValue: CustomerConversationMessages?
    private var chatDraft = ""
    private var availableRequestsValue: [ProviderOrder] = []
    private var marketRequestValue: ProviderMarketRequest?
    private var marketOffersValue: [ProviderMarketOffer] = []
    private var offerPrice = ""
    private var offerIncludes = ""
    private var offerNote = ""
    private var selectedOfferETA = -1
    private var selectedOfferDeductible = -1
    private var selectedMarketOffer = -1
    private var notificationIDs: [String] = []
    private var notificationLinks: [String] = []
    private let currencyLabel: String

    /// C32 in provider mode is the customer screen fed by provider-mode data, as C19 is (DEC-058).
    public private(set) var notificationsState = CustomerLogic.reduce(
        screen: "SCR-C32", input: ["event": .text("loading")])
    /// Rows open through the app root, which routes across modes (17 §الروابط العميقة).
    public var onDeepLink: (String) -> Void = { _ in }

    public init(api: any ProviderAPI, currencyLabel: String) {
        self.api = api
        self.currencyLabel = currencyLabel
    }

    public func loadApplication() async {
        state = ProviderLogic.reduce(screen: "SCR-P01", input: ["event": .text("loading")])
        do {
            let application = try await api.application()
            applicationValue = application
            hydrate(application)
            state =
                application.status == "NOT_STARTED" ? introState(application) : statusState(application)
        } catch { state = ProviderLogic.reduce(screen: "SCR-P01", input: ["event": .text("error")]) }
    }

    public func handleAction(_ action: String) {
        guard state.visibleActions.contains(action) else { return }
        if action == "start_provider_application" || action == "resubmit_provider_application" {
            showProfile()
        } else if action == "open_provider_home" {
            Task { await loadHome() }
        } else if action == "open_assigned_order" {
            Task { await loadActiveOrder() }
        } else if action == "open_earnings" {
            Task { await loadEarnings() }
        } else if action == "open_provider_profile" {
            Task { await loadProviderProfile() }
        } else if action == "open_available_requests" {
            Task { await openMarketRequest(0) }
        } else if action == "open_my_offers" {
            Task { await loadMarketOffers() }
        } else if action == "open_messages" || action == "chat" {
            Task { await openChat() }
        } else if action == "open_notifications" {
            Task { await loadNotifications() }
        }
    }

    /// ACTIVE, PENDING (any submitted application), or NONE — the input DeepLinkRouter needs.
    public func providerStatus() async -> String {
        switch (try? await api.application())?.status ?? "NOT_STARTED" {
        case "ACTIVE": "ACTIVE"
        case "NOT_STARTED": "NONE"
        default: "PENDING"
        }
    }

    public func loadNotifications(notificationsDenied: Bool = false) async {
        notificationsState = CustomerLogic.reduce(screen: "SCR-C32", input: ["event": .text("loading")])
        state = ProviderUIState(screen: "SCR-C32", phase: .loading)
        do {
            let values = try await api.notifications()
            notificationIDs = values.map(\.id)
            notificationLinks = values.map { $0.deepLink ?? "" }
            notificationsState = CustomerLogic.reduce(
                screen: "SCR-C32",
                input: [
                    "event": .text("loaded"), "notification_titles": .strings(values.map(\.title)),
                    "notification_bodies": .strings(values.map(\.body)),
                    "notification_states": .strings(values.map { $0.readAt == nil ? "unread" : "read" }),
                    "deep_links": .strings(notificationLinks),
                    "notification_times": .strings(values.map { displayDateTime($0.createdAt) }),
                    "notifications_denied": .bool(notificationsDenied),
                ])
        } catch {
            notificationsState = CustomerLogic.reduce(screen: "SCR-C32", input: ["event": .text("error")])
        }
        state = ProviderUIState(screen: "SCR-C32", phase: .content, canContinue: notificationsState.canContinue)
    }

    public func openNotification(index: Int) async {
        guard notificationIDs.indices.contains(index) else { return }
        try? await api.markNotificationsRead(ids: [notificationIDs[index]])
        let link = notificationLinks[index]
        if link.isEmpty { await loadNotifications() } else { onDeepLink(link) }
    }

    /// 17 §الروابط العميقة — provider targets; the router has already checked the provider status.
    public func openDeepLink(_ target: DeepLinkTarget, notificationsDenied: Bool) async {
        switch target.target {
        case "SCR-P09":
            state = ProviderLogic.reduce(screen: "SCR-P09", input: ["event": .text("loading")])
            do {
                let request = try await api.availableRequest(orderID: target.orderID)
                marketRequestValue = request
                state = marketRequestState(request)
            } catch {
                state = ProviderLogic.reduce(screen: "SCR-P09", input: ["event": .text("error")])
            }
        case "SCR-P11": await loadMarketOffers()
        case "SCR-P18": await loadEarnings()
        case "SCR-P07": await loadApplication()
        case "PROVIDER_ORDER", "SCR-C19", "SCR-P17": await openOrderLink(target)
        default: await loadNotifications(notificationsDenied: notificationsDenied)
        }
    }

    private func openOrderLink(_ target: DeepLinkTarget) async {
        state = ProviderLogic.reduce(screen: "SCR-P12", input: ["event": .text("loading")])
        let current = (try? await api.currentOrders()) ?? []
        let past = current.contains { $0.id == target.orderID } ? [] : ((try? await api.pastOrders()) ?? [])
        guard let order = (current + past).first(where: { $0.id == target.orderID }) else {
            await loadHome()
            return
        }
        activeOrderValue = order
        if target.target == "SCR-C19" {
            await openChat()
        } else if target.target == "SCR-P17" && order.availableActions.contains("rate_customer") {
            ratingStars = 0
            state = ratingState(event: "loaded")
        } else if ["CANCELLED", "EXPIRED", "OPEN"].contains(order.status) {
            await loadHome()
        } else {
            state = activeOrderState(order)
        }
    }

    public func loadHome() async {
        state = ProviderLogic.reduce(screen: "SCR-P08", input: ["event": .text("loading")])
        do {
            let value = try await api.home()
            homeValue = value
            availableRequestsValue =
                value.availableActions.contains("open_available_requests")
                ? try await api.availableRequests() : []
            state = homeState(value, event: "loaded")
        } catch {
            state = ProviderLogic.reduce(screen: "SCR-P08", input: ["event": .text("error")])
        }
    }

    public func openMarketRequest(_ index: Int) async {
        guard availableRequestsValue.indices.contains(index) else { return }
        state = ProviderLogic.reduce(screen: "SCR-P09", input: ["event": .text("loading")])
        do {
            marketRequestValue = try await api.availableRequest(
                orderID: availableRequestsValue[index].id)
            state = marketRequestState(marketRequestValue!)
        } catch {
            state = ProviderLogic.reduce(screen: "SCR-P09", input: ["event": .text("error")])
        }
    }

    public func handleMarketRequestAction(_ action: String) async {
        guard let request = marketRequestValue, request.order.availableActions.contains(action) else {
            return
        }
        switch action {
        case "submit_offer": openOfferForm(request)
        case "withdraw_offer": await loadMarketOffers(preselect: request.ownOffer?.id)
        case "chat": await openChat()
        default: break
        }
    }

    private func openOfferForm(_ request: ProviderMarketRequest) {
        offerPrice = ""
        offerIncludes = request.offerContext.defaultIncludesText
        offerNote = ""
        selectedOfferETA = -1
        selectedOfferDeductible = -1
        state = offerState(event: "loaded", request: request)
    }

    public func selectOfferETA(_ index: Int) {
        guard let request = marketRequestValue, request.offerContext.etaOptions.indices.contains(index)
        else { return }
        selectedOfferETA = index
        state = offerState(event: "validate")
    }

    public func selectOfferDeductible(_ index: Int) {
        guard let request = marketRequestValue,
            request.offerContext.deductibleOptions.indices.contains(index)
        else { return }
        selectedOfferDeductible = index
        state = offerState(event: "validate")
    }

    public func updateOfferPrice(_ value: String) {
        offerPrice = String(value.filter { $0.isNumber || $0 == "." }.prefix(12))
        state = offerState(event: "validate")
    }

    public func updateOfferIncludes(_ value: String) {
        offerIncludes = String(value.prefix(150))
        state = offerState(event: "validate")
    }

    public func updateOfferNote(_ value: String) {
        offerNote = String(value.prefix(300))
        state = offerState(event: "validate")
    }

    public func submitMarketOffer() async {
        guard let request = marketRequestValue else { return }
        let checked = offerState(event: "validate", request: request)
        guard checked.canContinue else {
            state = checked
            return
        }
        state = offerState(event: "submitting", request: request)
        do {
            let eta =
                request.order.timing.type == "NOW"
                ? request.offerContext.etaOptions[selectedOfferETA] : nil
            let deductible =
                request.order.pricingMode == "INSPECTION"
                ? request.offerContext.deductibleOptions[selectedOfferDeductible].value : nil
            _ = try await api.submitOffer(
                orderID: request.order.id,
                body: ProviderOfferSubmission(
                    price: offerPrice, etaMinutes: eta,
                    inspectionFeeDeductible: deductible, includesText: offerIncludes,
                    note: offerNote.isEmpty ? nil : offerNote))
            state = offerState(event: "success", request: request, actions: [])
        } catch {
            state = offerState(event: "unavailable", request: request, actions: [])
        }
    }

    public func loadMarketOffers(preselect offerID: Int? = nil) async {
        state = ProviderLogic.reduce(screen: "SCR-P11", input: ["event": .text("loading")])
        do {
            marketOffersValue = try await api.offers()
            selectedMarketOffer =
                offerID.flatMap { id in
                    marketOffersValue.firstIndex { $0.id == id }
                } ?? -1
            state = offersState(event: selectedMarketOffer >= 0 ? "confirm_withdraw" : "loaded")
        } catch {
            state = ProviderLogic.reduce(screen: "SCR-P11", input: ["event": .text("error")])
        }
    }

    public func handleOfferListAction(_ index: Int, action: String) async {
        guard marketOffersValue.indices.contains(index),
            marketOffersValue[index].availableActions.contains(action)
        else { return }
        let offer = marketOffersValue[index]
        switch action {
        case "withdraw_offer":
            selectedMarketOffer = index
            state = offersState(event: "confirm_withdraw")
        case "open_available_request":
            state = ProviderLogic.reduce(screen: "SCR-P09", input: ["event": .text("loading")])
            do {
                marketRequestValue = try await api.availableRequest(orderID: offer.order.id)
                state = marketRequestState(marketRequestValue!)
            } catch {
                state = ProviderLogic.reduce(screen: "SCR-P09", input: ["event": .text("error")])
            }
        case "open_assigned_order": await loadActiveOrder()
        default: break
        }
    }

    public func confirmOfferWithdrawal() async {
        guard marketOffersValue.indices.contains(selectedMarketOffer) else { return }
        let index = selectedMarketOffer
        state = offersState(event: "withdrawing")
        do {
            marketOffersValue[index] = try await api.withdrawOffer(id: marketOffersValue[index].id)
            selectedMarketOffer = -1
            state = offersState(event: "withdrawn")
        } catch {
            if let refreshed = try? await api.offers() { marketOffersValue = refreshed }
            selectedMarketOffer = -1
            state = offersState(event: "changed")
        }
    }

    public func cancelOfferWithdrawal() {
        selectedMarketOffer = -1
        state = offersState(event: "loaded")
    }

    public func backToMarketRequest() {
        if let value = marketRequestValue { state = marketRequestState(value) }
    }

    public func toggleAvailability(_ availableNow: Bool) async {
        guard let current = homeValue,
            current.availableActions.contains("set_availability"), !state.isBusy
        else { return }
        state = homeState(
            ProviderHome(
                operatingMode: current.operatingMode, availableNow: availableNow,
                activeOrder: current.activeOrder, availableActions: current.availableActions,
                unreadNotifications: current.unreadNotifications),
            event: "saving")
        do {
            let value = try await api.setAvailability(availableNow)
            homeValue = value
            state = homeState(value, event: "availability_saved")
        } catch {
            state = ProviderLogic.reduce(screen: "SCR-P08", input: ["event": .text("error")])
        }
    }

    public func loadActiveOrder() async {
        state = ProviderLogic.reduce(screen: "SCR-P12", input: ["event": .text("loading")])
        do {
            guard let value = try await api.currentOrders().first else {
                await loadHome()
                return
            }
            activeOrderValue = value
            state = activeOrderState(value)
        } catch {
            state = ProviderLogic.reduce(screen: "SCR-P12", input: ["event": .text("error")])
        }
    }

    public func performLocationAction(_ action: String, latitude: Double, longitude: Double) async {
        guard let order = activeOrderValue, order.availableActions.contains(action), !state.isBusy
        else { return }
        state = activeOrderState(order, event: "submitting", currentAction: action)
        do {
            let updated =
                if action == "start_trip" {
                    try await api.startTrip(order: order, latitude: latitude, longitude: longitude)
                } else {
                    try await api.markArrived(order: order, latitude: latitude, longitude: longitude)
                }
            activeOrderValue = updated
            state = activeOrderState(updated)
        } catch {
            state = ProviderLogic.reduce(screen: screen(for: order), input: ["event": .text("error")])
        }
    }

    public func handleOrderAction(_ action: String) async {
        guard let order = activeOrderValue, order.availableActions.contains(action), !state.isBusy
        else { return }
        switch action {
        case "submit_execution_quote", "submit_proposal": await openProposal(action)
        case "back_out", "report_unable", "report_no_show": await openUnable(action)
        case "start_work": await perform(order, action: action) { try await api.startWork(order: $0) }
        case "complete_inspection_only":
            await perform(order, action: action) { try await api.completeInspection(order: $0) }
        case "complete_work":
            await perform(order, action: action) { try await api.completeWork(order: $0) }
        case "confirm_cash":
            await perform(order, action: action) { try await api.confirmCash(order: $0) }
        case "chat": await openChat()
        default: break
        }
    }

    public func selectCustomerRating(_ index: Int) {
        guard (0..<5).contains(index) else { return }
        ratingStars = index + 1
        state = ratingState(event: "loaded")
    }

    public func submitCustomerRating() async {
        guard let order = activeOrderValue else { return }
        let checked = ratingState(event: "validate")
        guard checked.canContinue else {
            state = checked
            return
        }
        state = ratingState(event: "submitting")
        do {
            try await api.rateCustomer(orderID: order.id, stars: ratingStars)
            state = ratingState(event: "success", actions: [])
        } catch {
            state = ProviderLogic.reduce(screen: "SCR-P17", input: ["event": .text("error")])
        }
    }

    public func loadEarnings() async {
        state = ProviderLogic.reduce(screen: "SCR-P18", input: ["event": .text("loading")])
        do {
            let value = try await api.earnings()
            state = ProviderLogic.reduce(
                screen: "SCR-P18",
                input: [
                    "event": .text("loaded"), "operating_mode": .text(value.operatingMode),
                    "summary": .strings([
                        formatAmount(value.summary.balance), formatAmount(value.summary.available),
                        formatAmount(value.summary.pending),
                    ]),
                    "transaction_titles": .strings(value.transactions.map(\.title)),
                    "transaction_details": .strings(value.transactions.map(\.detail)),
                    "transaction_states": .strings(value.transactions.map(\.type)),
                ])
        } catch {
            state = ProviderLogic.reduce(screen: "SCR-P18", input: ["event": .text("error")])
        }
    }

    public func loadProviderProfile() async {
        state = ProviderLogic.reduce(screen: "SCR-P19", input: ["event": .text("loading")])
        do {
            let value = try await api.profile()
            workspaceProfileValue = value
            experience = String(value.experienceYears)
            bio = value.bio ?? ""
            categories = value.categories.map {
                ProviderCategory(id: $0.id, name: $0.name, specialties: $0.specialties)
            }
            workspaceSpecialtyIDs = Set(value.specialtyIDs)
            workspaceAreaIDs = Set(value.areaIDs)
            var loadedAreas: [ProviderChoice] = []
            for city in try await api.cities() {
                loadedAreas.append(contentsOf: try await api.areas(cityID: city.id))
            }
            workspaceAreas = Dictionary(grouping: loadedAreas, by: \.id).compactMap(\.value.first)
                .sorted { $0.id < $1.id }
            workspacePortfolio = value.portfolio
            hasProfilePhoto = value.avatarPath != nil
            state = workspaceProfileState(event: "loaded")
        } catch {
            state = ProviderLogic.reduce(screen: "SCR-P19", input: ["event": .text("error")])
        }
    }

    public func toggleWorkspaceSpecialty(_ index: Int) {
        let values = categories.flatMap(\.specialties)
        guard values.indices.contains(index) else { return }
        if !workspaceSpecialtyIDs.insert(values[index].id).inserted {
            workspaceSpecialtyIDs.remove(values[index].id)
        }
        state = workspaceProfileState(event: "validate")
    }

    public func toggleWorkspaceArea(_ index: Int) {
        guard workspaceAreas.indices.contains(index) else { return }
        if !workspaceAreaIDs.insert(workspaceAreas[index].id).inserted {
            workspaceAreaIDs.remove(workspaceAreas[index].id)
        }
        state = workspaceProfileState(event: "validate")
    }

    public func updateWorkspaceExperience(_ value: String) {
        experience = String(value.filter(\.isNumber).prefix(2))
        state = workspaceProfileState(event: "validate")
    }

    public func updateWorkspaceBio(_ value: String) {
        bio = value
        state = workspaceProfileState(event: "validate")
    }

    public func updatePortfolioCaption(_ value: String) {
        portfolioCaption = String(value.prefix(150))
        state = workspaceProfileState(event: "validate")
    }

    public func saveWorkspaceProfile() async {
        let checked = workspaceProfileState(event: "validate")
        guard checked.canContinue, let years = Int(experience) else {
            state = checked
            return
        }
        state = workspaceProfileState(event: "saving")
        do {
            workspaceProfileValue = try await api.updateProfile(
                ProviderProfileUpdate(
                    experienceYears: years, bio: bio.isEmpty ? nil : bio,
                    specialtyIDs: Array(workspaceSpecialtyIDs), areaIDs: Array(workspaceAreaIDs)))
            state = workspaceProfileState(event: "saved")
        } catch {
            state = ProviderLogic.reduce(screen: "SCR-P19", input: ["event": .text("error")])
        }
    }

    public func uploadWorkspaceAvatar(_ upload: CustomerMediaUpload) async {
        state = workspaceProfileState(event: "uploading")
        do {
            let mediaID = try await api.uploadImage(upload)
            try await api.updateAvatar(mediaID: mediaID)
            hasProfilePhoto = true
            state = workspaceProfileState(event: "saved")
        } catch {
            state = ProviderLogic.reduce(screen: "SCR-P19", input: ["event": .text("error")])
        }
    }

    public func addWorkspacePortfolio(_ upload: CustomerMediaUpload) async {
        guard workspacePortfolio.count < 20 else { return }
        state = workspaceProfileState(event: "uploading")
        do {
            let mediaID = try await api.uploadImage(upload)
            let item = try await api.addPortfolio(
                ProviderPortfolioSubmission(
                    mediaID: mediaID, caption: portfolioCaption.isEmpty ? nil : portfolioCaption))
            workspacePortfolio.append(item)
            portfolioCaption = ""
            state = workspaceProfileState(event: "saved")
        } catch {
            state = ProviderLogic.reduce(screen: "SCR-P19", input: ["event": .text("error")])
        }
    }

    public func deleteWorkspacePortfolio(_ index: Int) async {
        guard workspacePortfolio.indices.contains(index) else { return }
        let item = workspacePortfolio[index]
        state = workspaceProfileState(event: "saving")
        do {
            try await api.deletePortfolio(id: item.id)
            workspacePortfolio.removeAll { $0.id == item.id }
            state = workspaceProfileState(event: "saved")
        } catch {
            state = ProviderLogic.reduce(screen: "SCR-P19", input: ["event": .text("error")])
        }
    }

    public func openChat() async {
        state = ProviderLogic.reduce(screen: "SCR-C19", input: ["event": .text("loading")])
        do {
            let order = activeOrderValue
            let conversationID: Int?
            if let existingID = order?.conversationID {
                conversationID = existingID
            } else {
                conversationID =
                    (try await api.conversations()).first { order == nil || $0.order.id == order?.id }?.id
            }
            guard let conversationID else { throw ProviderWorkspaceError.conversationUnavailable }
            activeConversationID = conversationID
            chatValue = try await api.conversationMessages(id: conversationID)
            chatDraft = ""
            state = chatState(event: "loaded")
        } catch {
            state = ProviderLogic.reduce(screen: "SCR-C19", input: ["event": .text("error")])
        }
    }

    public func updateChatDraft(_ value: String) {
        chatDraft = String(value.prefix(1_000))
        state = chatState(event: "loaded")
    }

    public func sendChatMessage() async {
        guard let id = activeConversationID, chatState(event: "loaded").canContinue else { return }
        state = chatState(event: "sending")
        do {
            try await api.sendMessage(conversationID: id, body: chatDraft, mediaIDs: [])
            chatDraft = ""
            chatValue = try await api.conversationMessages(id: id)
            state = chatState(event: "loaded")
        } catch {
            state = ProviderLogic.reduce(screen: "SCR-C19", input: ["event": .text("error")])
        }
    }

    public func sendChatPhoto(_ upload: CustomerMediaUpload) async {
        guard let id = activeConversationID else { return }
        state = chatState(event: "sending")
        do {
            let mediaID = try await api.uploadImage(upload)
            try await api.sendMessage(conversationID: id, body: nil, mediaIDs: [mediaID])
            chatValue = try await api.conversationMessages(id: id)
            state = chatState(event: "loaded")
        } catch {
            state = ProviderLogic.reduce(screen: "SCR-C19", input: ["event": .text("error")])
        }
    }

    public func selectProposalType(_ index: Int) {
        guard proposalTypesValue.indices.contains(index) else { return }
        selectedProposalType = index
        state = proposalState(event: "loaded")
    }

    public func updateProposalAmount(_ value: String) {
        proposalAmount = String(value.filter { $0.isNumber || $0 == "." })
        state = proposalState(event: "loaded")
    }

    public func updateProposalReason(_ value: String) {
        proposalReason = value
        state = proposalState(event: "loaded")
    }

    public func updateOutsidePriceGuideReason(_ value: String) {
        outsidePriceGuideReason = value
        state = proposalState(event: "loaded")
    }

    public func uploadProposalPhoto(_ upload: CustomerMediaUpload) async {
        state = proposalState(event: "submitting")
        do {
            proposalPhotoMediaID = try await api.uploadImage(upload)
            state = proposalState(event: "loaded")
        } catch {
            state = ProviderLogic.reduce(screen: "SCR-P20", input: ["event": .text("error")])
        }
    }

    public func deleteProposalPhoto() {
        proposalPhotoMediaID = nil
        state = proposalState(event: "loaded")
    }

    public func submitProposal() async {
        guard let order = activeOrderValue, proposalTypesValue.indices.contains(selectedProposalType)
        else { return }
        let checked = proposalState(event: "loaded")
        guard checked.canContinue else {
            state = checked
            return
        }
        state = proposalState(event: "submitting")
        do {
            try await api.submitProposal(
                order: order,
                body: ProviderProposalSubmission(
                    expectedVersion: order.version, type: proposalTypesValue[selectedProposalType].code,
                    amount: proposalAmount, reason: proposalReason, photoMediaID: proposalPhotoMediaID,
                    outsidePriceGuideReason: outsidePriceGuideReason.isEmpty ? nil : outsidePriceGuideReason))
            state = proposalState(event: "success")
        } catch {
            state = ProviderLogic.reduce(screen: "SCR-P20", input: ["event": .text("error")])
        }
    }

    public func selectUnableReason(_ index: Int) {
        guard providerReasonsValue.indices.contains(index) else { return }
        selectedUnableReason = index
        state = unableState(event: "loaded")
    }

    public func updateUnableNote(_ value: String) {
        unableNote = value
        state = unableState(event: "loaded")
    }

    public func confirmUnable() async {
        guard let order = activeOrderValue else { return }
        let checked = unableState(event: "loaded")
        guard checked.canContinue else {
            state = checked
            return
        }
        state = unableState(event: "submitting")
        do {
            let updated: ProviderOrder
            switch requestedUnableAction {
            case "back_out":
                updated = try await api.backOut(
                    order: order, reasonCode: providerReasonsValue[selectedUnableReason].code)
            case "report_unable":
                updated = try await api.reportUnable(
                    order: order, reasonCode: providerReasonsValue[selectedUnableReason].code,
                    note: unableNote)
            case "report_no_show": updated = try await api.reportNoShow(order: order)
            default: return
            }
            activeOrderValue = updated
            if ["OPEN", "CLOSED", "CANCELLED", "EXPIRED"].contains(updated.status) {
                await loadHome()
            } else {
                state = activeOrderState(updated)
            }
        } catch {
            state = ProviderLogic.reduce(screen: "SCR-P21", input: ["event": .text("error")])
        }
    }

    public func backToActiveOrder() {
        if let order = activeOrderValue { state = activeOrderState(order) }
    }

    public func leaveChat() async {
        if let order = activeOrderValue {
            state = activeOrderState(order)
        } else {
            await loadHome()
        }
    }

    public var activeDestination: (Double, Double)? {
        guard let latitude = activeOrderValue?.location.lat,
            let longitude = activeOrderValue?.location.lng
        else { return nil }
        return (latitude, longitude)
    }

    public var activeCustomerPhone: String? { activeOrderValue?.customer?.phone }

    public func showProfile() { state = profileState(event: "validate") }
    public func updateExperience(_ value: String) {
        experience = String(value.filter(\.isNumber).prefix(2))
        state = profileState(event: "validate")
    }
    public func updateBio(_ value: String) {
        bio = value
        state = profileState(event: "validate")
    }

    public func uploadProfilePhoto(_ upload: CustomerMediaUpload) async {
        state = profileState(event: "uploading")
        do {
            profilePhotoMediaID = try await api.uploadImage(upload)
            hasProfilePhoto = true
            state = profileState(event: "validate")
        } catch { state = profileState(event: "media_error") }
    }

    public func openIdentity() {
        if profileState(event: "validate").canContinue { state = identityState(event: "validate") }
    }

    public func uploadIdentity(front: Bool, upload: CustomerMediaUpload) async {
        state = identityState(event: "uploading")
        do {
            let id = try await api.uploadImage(upload)
            if front {
                idFrontMediaID = id
                hasIDFront = true
            } else {
                idBackMediaID = id
                hasIDBack = true
            }
            state = identityState(event: "validate")
        } catch { state = identityState(event: "media_error") }
    }

    public func loadCatalog() async {
        guard identityState(event: "validate").canContinue else { return }
        state = ProviderLogic.reduce(screen: "SCR-P04", input: ["event": .text("loading")])
        do {
            categories = try await api.catalog()
            state = catalogState()
        } catch { state = ProviderLogic.reduce(screen: "SCR-P04", input: ["event": .text("error")]) }
    }

    public func toggleCategory(_ index: Int) {
        guard categories.indices.contains(index) else { return }
        let category = categories[index]
        if categoryIDs.contains(category.id) {
            categoryIDs.remove(category.id)
            specialtyIDs.subtract(category.specialties.map(\.id))
        } else {
            categoryIDs.insert(category.id)
        }
        state = catalogState()
    }

    public func toggleSpecialty(_ index: Int) {
        let values = selectedSpecialties
        guard values.indices.contains(index) else { return }
        let id = values[index].id
        if specialtyIDs.contains(id) { specialtyIDs.remove(id) } else { specialtyIDs.insert(id) }
        state = catalogState()
    }

    public func loadCities() async {
        guard catalogState().canContinue else { return }
        state = ProviderLogic.reduce(screen: "SCR-P05", input: ["event": .text("loading")])
        do {
            citiesValue = try await api.cities()
            cityIndex = citiesValue.isEmpty ? -1 : 0
            if let city = citiesValue.first {
                areasValue = try await api.areas(cityID: city.id)
            } else {
                areasValue = []
            }
            state = areasState()
        } catch { state = ProviderLogic.reduce(screen: "SCR-P05", input: ["event": .text("error")]) }
    }

    public func selectCity(_ index: Int) async {
        guard citiesValue.indices.contains(index) else { return }
        state = ProviderLogic.reduce(screen: "SCR-P05", input: ["event": .text("loading")])
        do {
            cityIndex = index
            areasValue = try await api.areas(cityID: citiesValue[index].id)
            areaIDs.formIntersection(areasValue.map(\.id))
            state = areasState()
        } catch { state = ProviderLogic.reduce(screen: "SCR-P05", input: ["event": .text("error")]) }
    }

    public func toggleArea(_ index: Int) {
        guard areasValue.indices.contains(index) else { return }
        let id = areasValue[index].id
        if areaIDs.contains(id) { areaIDs.remove(id) } else { areaIDs.insert(id) }
        state = areasState()
    }

    public func loadPayout() async {
        guard areasState().canContinue else { return }
        state = ProviderLogic.reduce(screen: "SCR-P06", input: ["event": .text("loading")])
        do {
            payoutOptionsValue = try await api.payoutOptions()
            payoutIndex =
                payoutOptionsValue.firstIndex { $0.code == applicationValue?.payoutMethod } ?? -1
            state = payoutState(event: "validate")
        } catch { state = ProviderLogic.reduce(screen: "SCR-P06", input: ["event": .text("error")]) }
    }

    public func selectPayout(_ index: Int) {
        guard payoutOptionsValue.indices.contains(index) else { return }
        payoutIndex = index
        state = payoutState(event: "validate")
    }
    public func updatePayoutDetails(_ value: String) {
        payoutDetails = value
        state = payoutState(event: "validate")
    }

    public func submit() async {
        guard payoutState(event: "validate").canContinue,
            payoutOptionsValue.indices.contains(payoutIndex),
            let years = Int(experience)
        else {
            state = payoutState(event: "validate")
            return
        }
        state = payoutState(event: "submitting")
        let body = ProviderApplicationSubmission(
            experienceYears: years, bio: bio.isEmpty ? nil : bio,
            categoryIDs: Array(categoryIDs), specialtyIDs: Array(specialtyIDs), areaIDs: Array(areaIDs),
            payoutMethod: payoutOptionsValue[payoutIndex].code, payoutDetails: payoutDetails,
            profilePhotoMediaID: profilePhotoMediaID, idFrontMediaID: idFrontMediaID,
            idBackMediaID: idBackMediaID)
        do {
            let submitted = try await api.submit(body)
            applicationValue = submitted
            state = statusState(submitted)
        } catch {
            state = ProviderLogic.reduce(screen: "SCR-P06", input: ["event": .text("error")])
        }
    }

    private func openProposal(_ action: String) async {
        guard let order = activeOrderValue else { return }
        state = ProviderLogic.reduce(screen: "SCR-P20", input: ["event": .text("loading")])
        do {
            proposalTypesValue = try await api.proposalTypes().filter {
                action == "submit_execution_quote"
                    ? $0.code == "EXECUTION_QUOTE" : $0.code != "EXECUTION_QUOTE"
            }
            selectedProposalType = proposalTypesValue.isEmpty ? -1 : 0
            proposalAmount = ""
            proposalReason = ""
            outsidePriceGuideReason = ""
            proposalPhotoMediaID = nil
            state = proposalState(event: "loaded", order: order)
        } catch {
            state = ProviderLogic.reduce(screen: "SCR-P20", input: ["event": .text("error")])
        }
    }

    private func openUnable(_ action: String) async {
        guard let order = activeOrderValue else { return }
        state = ProviderLogic.reduce(screen: "SCR-P21", input: ["event": .text("loading")])
        do {
            providerReasonsValue = action == "report_no_show" ? [] : try await api.providerReasons()
            selectedUnableReason = -1
            unableNote = ""
            requestedUnableAction = action
            state = unableState(event: "loaded", order: order)
        } catch {
            state = ProviderLogic.reduce(screen: "SCR-P21", input: ["event": .text("error")])
        }
    }

    private func perform(
        _ order: ProviderOrder, action: String,
        operation: (ProviderOrder) async throws -> ProviderOrder
    ) async {
        state = activeOrderState(order, event: "submitting", currentAction: action)
        do {
            let updated = try await operation(order)
            activeOrderValue = updated
            if updated.status == "CLOSED" && updated.availableActions.contains("rate_customer") {
                ratingStars = 0
                state = ratingState(event: "loaded")
            } else if ["OPEN", "CLOSED", "CANCELLED", "EXPIRED"].contains(updated.status) {
                await loadHome()
            } else {
                state = activeOrderState(updated)
            }
        } catch {
            state = ProviderLogic.reduce(screen: screen(for: order), input: ["event": .text("error")])
        }
    }

    private func activeOrderState(
        _ order: ProviderOrder, event: String = "loaded", currentAction: String = ""
    ) -> ProviderUIState {
        let target = screen(for: order)
        let message: String
        if order.priceGuideReviewRequired == true && target == "SCR-P14" {
            message = "provider.price_guide.other_review"
            // swiftlint:disable opening_brace
        } else if target == "SCR-P15"
            && !order.availableActions.contains("submit_proposal")
            && !order.availableActions.contains("complete_work")
        {
            message = "provider.proposal.pending"
        } else if target == "SCR-P16" && !order.availableActions.contains("confirm_cash") {
            message = "provider.payment.waiting"
        } else {
            message = ""
        }
        // swiftlint:enable opening_brace
        let service = [order.category.name, order.problemType.name].filter { !$0.isEmpty }.joined(
            separator: " — ")
        let address = [
            order.location.addressText, order.location.building, order.location.floor,
            order.location.apartment, order.location.landmark,
        ].compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: ", ")
        let amounts =
            target == "SCR-P16"
            ? [
                formatAmount(order.amounts.laborTotal), formatAmount(order.amounts.materialsTotal),
                formatAmount(order.amounts.finalAmount), order.amounts.paymentMethodLabel ?? "",
            ] : []
        return ProviderLogic.reduce(
            screen: target,
            input: [
                "event": .text(event), "current_action": .text(currentAction),
                "display_status": .text(order.displayStatus),
                "customer_name": .text(order.customer?.name ?? ""),
                "customer_rating": .text(order.customer?.ratingAverage ?? ""),
                "order_summary": .strings([
                    "#\(order.number)", service, address.isEmpty ? (order.location.area ?? "") : address,
                    timingLabel(order),
                ]),
                "amount_summary": .strings(amounts),
                "step_states": .strings(order.stepper?.map(\.state) ?? []),
                "available_actions": .strings(order.availableActions),
                "price_guide_min": .text(
                    order.executionPriceGuide?.minimum.split(separator: ".").first.map(String.init) ?? ""),
                "price_guide_max": .text(
                    order.executionPriceGuide?.maximum.split(separator: ".").first.map(String.init) ?? ""),
                "price_review_required": .bool(order.priceGuideReviewRequired == true),
                "message_key": .text(message),
            ])
    }

    private func proposalState(event: String, order: ProviderOrder? = nil) -> ProviderUIState {
        guard let value = order ?? activeOrderValue else {
            return ProviderLogic.reduce(screen: "SCR-P20", input: ["event": .text("error")])
        }
        return ProviderLogic.reduce(
            screen: "SCR-P20",
            input: [
                "event": .text(event), "proposal_type_codes": .strings(proposalTypesValue.map(\.code)),
                "proposal_type_labels": .strings(proposalTypesValue.map(\.label)),
                "selected_type_index": .integer(selectedProposalType), "amount": .text(proposalAmount),
                "reason": .text(proposalReason), "outside_reason": .text(outsidePriceGuideReason),
                "photo_uploaded": .bool(proposalPhotoMediaID != nil),
                "price_guide_min": .text(value.executionPriceGuide?.minimum ?? ""),
                "price_guide_max": .text(value.executionPriceGuide?.maximum ?? ""),
                "price_review_required": .bool(value.priceGuideReviewRequired == true),
                "available_actions": .strings(value.availableActions),
            ])
    }

    private func unableState(event: String, order: ProviderOrder? = nil) -> ProviderUIState {
        guard let value = order ?? activeOrderValue else {
            return ProviderLogic.reduce(screen: "SCR-P21", input: ["event": .text("error")])
        }
        return ProviderLogic.reduce(
            screen: "SCR-P21",
            input: [
                "event": .text(event), "requested_action": .text(requestedUnableAction),
                "reason_codes": .strings(providerReasonsValue.map(\.code)),
                "reason_labels": .strings(providerReasonsValue.map(\.label)),
                "selected_reason_index": .integer(selectedUnableReason), "note": .text(unableNote),
                "available_actions": .strings(value.availableActions),
            ])
    }

    private func ratingState(event: String, actions: [String]? = nil) -> ProviderUIState {
        let order = activeOrderValue
        return ProviderLogic.reduce(
            screen: "SCR-P17",
            input: [
                "event": .text(event), "stars": .integer(ratingStars),
                "order_summary": .strings([
                    "#\(order?.number ?? 0)", order?.customer?.name ?? "",
                    [order?.category.name ?? "", order?.problemType.name ?? ""]
                        .filter { !$0.isEmpty }.joined(separator: " — "),
                ]),
                "available_actions": .strings(actions ?? order?.availableActions ?? []),
            ])
    }

    private func workspaceProfileState(event: String) -> ProviderUIState {
        guard let value = workspaceProfileValue else {
            return ProviderLogic.reduce(screen: "SCR-P19", input: ["event": .text("error")])
        }
        let specialties = categories.flatMap(\.specialties)
        let base = ProviderLogic.reduce(
            screen: "SCR-P19",
            input: [
                "event": .text(event), "name": .text(value.name),
                "rating": .text(value.ratingAverage ?? ""),
                "profile_stats": .strings([
                    value.ratingAverage ?? "", String(value.completedOrders),
                    value.averageResponseMinutes.map(String.init) ?? "—",
                ]),
                "profile_photo_uploaded": .bool(hasProfilePhoto),
                "experience_years": .text(experience), "bio": .text(bio),
                "category_labels": .strings(categories.map(\.name)),
                "specialty_labels": .strings(specialties.map(\.name)),
                "specialty_ids": .strings(workspaceSpecialtyIDs.map(String.init)),
                "area_labels": .strings(workspaceAreas.map(\.name)),
                "area_ids": .strings(workspaceAreaIDs.map(String.init)),
                "portfolio_ids": .strings(workspacePortfolio.map { String($0.id) }),
                "portfolio_labels": .strings(workspacePortfolio.map { $0.caption ?? "#\($0.id)" }),
                "portfolio_caption": .text(portfolioCaption),
                "available_actions": .strings(value.availableActions),
            ])
        return copied(
            base,
            primary: specialties.indices.filter { workspaceSpecialtyIDs.contains(specialties[$0].id) },
            secondary: workspaceAreas.indices.filter { workspaceAreaIDs.contains(workspaceAreas[$0].id) })
    }

    private func chatState(event: String) -> ProviderUIState {
        guard let chat = chatValue else {
            return ProviderLogic.reduce(screen: "SCR-C19", input: ["event": .text("error")])
        }
        let conversation = chat.conversation
        let messages = chat.messages
        return ProviderLogic.reduce(
            screen: "SCR-C19",
            input: [
                "event": .text(event), "viewer_role": .text("PROVIDER"),
                "counterpart_name": .text(conversation.customer?.name ?? ""),
                "conversation_status": .text(conversation.status),
                "order_summary": .text(
                    "\(conversation.order.category ?? "") #\(conversation.order.number ?? "") · \(conversation.order.statusLabel ?? "")"
                ),
                "message_bodies": .strings(messages.map { $0.body ?? "" }),
                "message_kinds": .strings(
                    messages.map {
                        let direction = $0.isMine ? "sent" : "received"
                        let kind = $0.wasMasked ? "blocked" : ($0.media.isEmpty ? "text" : "image")
                        return "\(direction)_\(kind)"
                    }),
                "message_times": .strings(messages.map { displayDateTime($0.createdAt) }),
                "draft": .text(chatDraft),
            ])
    }

    // swiftlint:disable:next cyclomatic_complexity
    private func screen(for order: ProviderOrder) -> String {
        switch order.status {
        case "CONFIRMED": return "SCR-P12"
        case "ON_THE_WAY": return "SCR-P13"
        case "ARRIVED", "AWAITING_QUOTE_APPROVAL": return "SCR-P14"
        case "IN_PROGRESS": return "SCR-P15"
        case "AWAITING_PAYMENT", "AWAITING_CONFIRMATION": return "SCR-P16"
        case "CLOSED": return "SCR-P17"
        case "DISPUTED":
            switch order.stepper?.firstIndex(where: { $0.state == "on_hold" }) {
            case 2: return "SCR-P14"
            case 3: return "SCR-P15"
            case 4, 5: return "SCR-P16"
            default: return "SCR-P12"
            }
        default: return "SCR-P12"
        }
    }

    private func timingLabel(_ order: ProviderOrder) -> String {
        guard order.timing.type != "NOW" else { return "provider.home.timing.now" }
        guard let raw = order.timing.slotStart else { return "" }
        let parser = ISO8601DateFormatter()
        guard let date = parser.date(from: raw) else { return raw }
        let formatter = DateFormatter()
        formatter.locale = .autoupdatingCurrent
        formatter.dateFormat = "d MMM, h:mm a"
        return formatter.string(from: date)
    }

    private func formatAmount(_ value: String?) -> String {
        "\((value ?? "0").split(separator: ".").first ?? "0") \(currencyLabel)"
    }

    private func displayDateTime(_ value: String?) -> String {
        guard let value, !value.isEmpty, let date = ISO8601DateFormatter().date(from: value) else {
            return value ?? ""
        }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ar_EG")
        formatter.dateFormat = "yyyy-MM-dd · h:mm a"
        return formatter.string(from: date)
    }

    private func introState(_ value: ProviderApplication) -> ProviderUIState {
        ProviderLogic.reduce(
            screen: "SCR-P01",
            input: [
                "event": .text("loaded"), "status": .text(value.status),
                "available_actions": .strings(value.availableActions),
            ])
    }
    private func profileState(event: String) -> ProviderUIState {
        ProviderLogic.reduce(
            screen: "SCR-P02",
            input: [
                "event": .text(event), "experience_years": .text(experience), "bio": .text(bio),
                "profile_photo_uploaded": .bool(hasProfilePhoto),
            ])
    }
    private func identityState(event: String) -> ProviderUIState {
        ProviderLogic.reduce(
            screen: "SCR-P03",
            input: [
                "event": .text(event), "id_front_uploaded": .bool(hasIDFront),
                "id_back_uploaded": .bool(hasIDBack),
            ])
    }
    private func catalogState() -> ProviderUIState {
        let base = ProviderLogic.reduce(
            screen: "SCR-P04",
            input: [
                "event": .text("validate"), "category_labels": .strings(categories.map(\.name)),
                "specialty_labels": .strings(selectedSpecialties.map(\.name)),
                "category_ids": .strings(categoryIDs.map(String.init)),
                "specialty_ids": .strings(specialtyIDs.map(String.init)),
            ])
        return copied(
            base, primary: categories.indices.filter { categoryIDs.contains(categories[$0].id) },
            secondary: selectedSpecialties.indices.filter {
                specialtyIDs.contains(selectedSpecialties[$0].id)
            })
    }
    private func areasState() -> ProviderUIState {
        let base = ProviderLogic.reduce(
            screen: "SCR-P05",
            input: [
                "event": .text("validate"), "city_labels": .strings(citiesValue.map(\.name)),
                "area_labels": .strings(areasValue.map(\.name)),
                "area_ids": .strings(areaIDs.map(String.init)),
            ])
        return copied(
            base, primary: cityIndex >= 0 ? [cityIndex] : [],
            secondary: areasValue.indices.filter { areaIDs.contains(areasValue[$0].id) })
    }
    private func payoutState(event: String) -> ProviderUIState {
        let base = ProviderLogic.reduce(
            screen: "SCR-P06",
            input: [
                "event": .text(event), "payout_labels": .strings(payoutOptionsValue.map(\.label)),
                "payout_field_labels": .strings(payoutOptionsValue.map(\.fieldLabel)),
                "payout_code": .text(
                    payoutOptionsValue.indices.contains(payoutIndex)
                        ? payoutOptionsValue[payoutIndex].code : ""),
                "payout_details": .text(payoutDetails),
                "available_actions": .strings(applicationValue?.availableActions ?? []),
                "summary": .strings(
                    [categoryIDs.count, specialtyIDs.count, areaIDs.count, 3].map(String.init)),
            ])
        return copied(base, primary: payoutIndex >= 0 ? [payoutIndex] : [], secondary: [])
    }
    private func statusState(_ value: ProviderApplication) -> ProviderUIState {
        ProviderLogic.reduce(
            screen: "SCR-P07",
            input: [
                "event": .text("loaded"), "status": .text(value.status),
                "display_status": .text(statusKey(value.status)),
                "reason": .text(value.rejectionReason ?? value.suspensionReason ?? ""),
                "available_actions": .strings(value.availableActions),
            ])
    }
    private func homeState(_ value: ProviderHome, event: String) -> ProviderUIState {
        let order = value.activeOrder
        let summary =
            order.map {
                [
                    String($0.number),
                    [$0.category.name, $0.problemType.name].filter { !$0.isEmpty }.joined(separator: " — "),
                    $0.location.area ?? "",
                    timingLabel($0),
                ]
            } ?? []
        return ProviderLogic.reduce(
            screen: "SCR-P08",
            input: [
                "event": .text(event), "available_now": .bool(value.availableNow),
                "active_order_id": .integer(order?.id ?? 0), "order_summary": .strings(summary),
                "display_status": .text(order?.displayStatus ?? ""),
                "available_actions": .strings(value.availableActions),
                "unread_notifications": .integer(value.unreadNotifications ?? 0),
                "request_titles": .strings(
                    availableRequestsValue.map { "\($0.number)|\($0.category.name)" }),
                "request_details": .strings(
                    availableRequestsValue.map {
                        [$0.problemType.name, $0.location.area ?? "", timingLabel($0)]
                            .filter { !$0.isEmpty }.joined(separator: "|")
                    }),
                "request_ids": .strings(availableRequestsValue.map { String($0.id) }),
            ])
    }

    private func marketRequestState(
        _ value: ProviderMarketRequest, event: String = "loaded"
    ) -> ProviderUIState {
        let order = value.order
        let own = value.ownOffer.map { [$0.price, $0.netAmount, $0.displayStatus] } ?? []
        return ProviderLogic.reduce(
            screen: "SCR-P09",
            input: [
                "event": .text(event), "customer_name": .text(order.customer?.name ?? ""),
                "customer_rating": .text(order.customer?.ratingAverage ?? ""),
                "display_status": .text(order.displayStatus),
                "order_summary": .strings([
                    "#\(order.number)",
                    [order.category.name, order.problemType.name].filter { !$0.isEmpty }
                        .joined(separator: " — "),
                    order.description ?? "", order.location.area ?? "", timingLabel(order),
                    order.pricingModeLabel ?? "", formatAmount(order.budgetAmount),
                    order.materialsResponsibilityLabel ?? "",
                ]),
                "media_labels": .strings(
                    value.media.enumerated().map { "\($0.element.type):\($0.offset + 1)" }),
                "offer_summary": .strings(own),
                "available_actions": .strings(order.availableActions),
            ])
    }

    private func offerState(
        event: String, request: ProviderMarketRequest? = nil, actions: [String]? = nil
    ) -> ProviderUIState {
        guard let value = request ?? marketRequestValue else {
            return ProviderLogic.reduce(screen: "SCR-P10", input: ["event": .text("error")])
        }
        return ProviderLogic.reduce(
            screen: "SCR-P10",
            input: [
                "event": .text(event), "timing_type": .text(value.order.timing.type),
                "pricing_mode": .text(value.order.pricingMode ?? ""),
                "min_amount": .text(value.offerContext.minimumAmount),
                "commission_rate": .text(value.offerContext.commissionRate),
                "price": .text(offerPrice), "includes_text": .text(offerIncludes),
                "note": .text(offerNote),
                "eta_codes": .strings(value.offerContext.etaOptions.map(String.init)),
                "eta_labels": .strings(value.offerContext.etaOptions.map(String.init)),
                "selected_eta_index": .integer(selectedOfferETA),
                "deductible_codes": .strings(value.offerContext.deductibleOptions.map(\.code)),
                "deductible_labels": .strings(value.offerContext.deductibleOptions.map(\.label)),
                "selected_deductible_index": .integer(selectedOfferDeductible),
                "order_summary": .strings([
                    "#\(value.order.number)", value.order.location.area ?? "", timingLabel(value.order),
                ]),
                "available_actions": .strings(actions ?? value.order.availableActions),
            ])
    }

    private func offersState(event: String) -> ProviderUIState {
        ProviderLogic.reduce(
            screen: "SCR-P11",
            input: [
                "event": .text(event),
                "offer_titles": .strings(
                    marketOffersValue.map { "\($0.order.number)|\($0.order.category ?? "")" }),
                "offer_details": .strings(
                    marketOffersValue.map { "\($0.order.area ?? "")|\($0.price)|\($0.netAmount)" }),
                "offer_statuses": .strings(marketOffersValue.map(\.displayStatus)),
                "offer_actions": .strings(
                    marketOffersValue.map { $0.availableActions.joined(separator: ",") }),
                "selected_offer_index": .integer(selectedMarketOffer),
            ])
    }
    private func timingLabel(_ order: ProviderHomeOrder) -> String {
        guard order.timing.type != "NOW" else { return "provider.home.timing.now" }
        guard let raw = order.timing.slotStart else { return "" }
        let parser = ISO8601DateFormatter()
        parser.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        guard let date = parser.date(from: raw) ?? ISO8601DateFormatter().date(from: raw) else {
            return raw
        }
        let formatter = DateFormatter()
        formatter.locale = .autoupdatingCurrent
        formatter.dateFormat = "d MMM, h:mm a"
        return formatter.string(from: date)
    }
    private var selectedSpecialties: [ProviderChoice] {
        categories.filter { categoryIDs.contains($0.id) }.flatMap(\.specialties)
    }
    private func hydrate(_ value: ProviderApplication) {
        experience = value.experienceYears.map(String.init) ?? ""
        bio = value.bio ?? ""
        categoryIDs = Set(value.categoryIDs ?? [])
        specialtyIDs = Set(value.specialtyIDs ?? [])
        areaIDs = Set(value.areaIDs ?? [])
        payoutDetails = value.payoutDetails ?? ""
        hasProfilePhoto = value.profilePhotoUploaded
        hasIDFront = value.documents.idFront
        hasIDBack = value.documents.idBack
    }
    private func statusKey(_ status: String) -> String {
        [
            "PENDING_REVIEW": "provider.application.pending", "REJECTED": "provider.application.rejected",
            "ACTIVE": "provider.application.active", "SUSPENDED": "provider.application.suspended",
        ][status]
            ?? "provider.application.pending"
    }
    private func copied(_ value: ProviderUIState, primary: [Int], secondary: [Int]) -> ProviderUIState {
        ProviderUIState(
            screen: value.screen, phase: value.phase, canContinue: value.canContinue,
            messageKey: value.messageKey, fieldErrors: value.fieldErrors,
            visibleActions: value.visibleActions,
            items: value.items, options: value.options, secondaryOptions: value.secondaryOptions,
            selectedCount: value.selectedCount, selectedPrimaryIndices: primary,
            selectedSecondaryIndices: secondary, isBusy: value.isBusy,
            hasProfilePhoto: value.hasProfilePhoto, hasIDFront: value.hasIDFront,
            hasIDBack: value.hasIDBack, fieldValues: value.fieldValues,
            availableNow: value.availableNow, activeOrderID: value.activeOrderID,
            displayStatus: value.displayStatus, stepStates: value.stepStates,
            selectedOptionIndex: value.selectedOptionIndex, optionCodes: value.optionCodes,
            showPriceGuide: value.showPriceGuide, showOutsideReason: value.showOutsideReason,
            hasPhoto: value.hasPhoto, currentAction: value.currentAction,
            priceGuideMinimum: value.priceGuideMinimum, priceGuideMaximum: value.priceGuideMaximum,
            customerName: value.customerName, customerRating: value.customerRating,
            itemDetails: value.itemDetails, itemStates: value.itemStates, areaOptions: value.areaOptions)
    }
}

private enum ProviderWorkspaceError: Error {
    case conversationUnavailable
}

/// Pure provider state and validation, shared by Linux tests and the SwiftUI presentation layer.
public enum ProviderLogic {
    // swiftlint:disable:next cyclomatic_complexity
    public static func reduce(screen: String, input: [String: ProviderInputValue]) -> ProviderUIState {
        switch input.text("event") {
        case "loading": return ProviderUIState(screen: screen, phase: .loading)
        case "error": return ProviderUIState(screen: screen, phase: .error, messageKey: "error.body")
        default:
            switch screen {
            case "SCR-P01": return intro(input)
            case "SCR-P02": return profile(input)
            case "SCR-P03": return identity(input)
            case "SCR-P04": return catalog(input)
            case "SCR-P05": return areas(input)
            case "SCR-P06": return payout(input)
            case "SCR-P07": return status(input)
            case "SCR-P08": return home(input)
            case "SCR-P09": return availableRequest(input)
            case "SCR-P10": return offerForm(input)
            case "SCR-P11": return offers(input)
            case "SCR-P12", "SCR-P13", "SCR-P14", "SCR-P15", "SCR-P16":
                return activeOrder(screen, input)
            case "SCR-P17": return rating(input)
            case "SCR-P18": return earnings(input)
            case "SCR-P19": return providerProfile(input)
            case "SCR-P20": return proposal(input)
            case "SCR-P21": return unable(input)
            case "SCR-C19": return chat(input)
            default: preconditionFailure("Unknown provider screen: \(screen)")
            }
        }
    }

    private static func intro(_ input: [String: ProviderInputValue]) -> ProviderUIState {
        ProviderUIState(
            screen: "SCR-P01", phase: .content, canContinue: true,
            messageKey: statusKey(input.text("status")),
            visibleActions: input.strings("available_actions"))
    }

    private static func profile(_ input: [String: ProviderInputValue]) -> ProviderUIState {
        let hasPhoto = input.bool("profile_photo_uploaded")
        let experience = Int(input.text("experience_years"))
        let bioLength = input.integer("bio_length", default: input.text("bio").count)
        var errors: [String] = []
        if !hasPhoto { errors.append("provider.onboarding.profile_photo.required") }
        if experience == nil || !(0...50).contains(experience!) {
            errors.append("provider.onboarding.experience.invalid")
        }
        if bioLength > 300 { errors.append("provider.onboarding.bio.max") }
        let empty = input.text("experience_years").isEmpty && input.text("bio").isEmpty
        return ProviderUIState(
            screen: "SCR-P02", phase: empty ? .empty : .content,
            canContinue: errors.isEmpty && input.text("event") != "uploading",
            messageKey: input.text("event") == "media_error" ? "media.error.invalid" : nil,
            fieldErrors: errors, isBusy: input.text("event") == "uploading",
            hasProfilePhoto: hasPhoto,
            fieldValues: [input.text("experience_years"), input.text("bio")])
    }

    private static func identity(_ input: [String: ProviderInputValue]) -> ProviderUIState {
        let front = input.bool("id_front_uploaded")
        let back = input.bool("id_back_uploaded")
        let errors = front && back ? [] : ["provider.onboarding.identity.required"]
        let mediaError = input.text("event") == "media_error"
        return ProviderUIState(
            screen: "SCR-P03", phase: !front && !back && !mediaError ? .empty : .content,
            canContinue: errors.isEmpty && input.text("event") != "uploading",
            messageKey: mediaError ? "media.error.invalid" : nil, fieldErrors: errors,
            isBusy: input.text("event") == "uploading", hasIDFront: front, hasIDBack: back)
    }

    private static func catalog(_ input: [String: ProviderInputValue]) -> ProviderUIState {
        let categoryIDs = input.strings("category_ids")
        let specialtyIDs = input.strings("specialty_ids")
        var errors: [String] = []
        if categoryIDs.isEmpty { errors.append("provider.onboarding.categories.required") }
        if specialtyIDs.isEmpty || !input.bool("specialty_relation_valid", default: true) {
            errors.append("provider.onboarding.specialties.required")
        }
        let categories = input.strings("category_labels")
        return ProviderUIState(
            screen: "SCR-P04", phase: categories.isEmpty ? .empty : .content,
            canContinue: errors.isEmpty, fieldErrors: errors, options: categories,
            secondaryOptions: input.strings("specialty_labels"),
            selectedCount: categoryIDs.count + specialtyIDs.count,
            selectedPrimaryIndices: Array(categories.indices.prefix(categoryIDs.count)),
            selectedSecondaryIndices: Array(
                input.strings("specialty_labels").indices.prefix(specialtyIDs.count)))
    }

    private static func areas(_ input: [String: ProviderInputValue]) -> ProviderUIState {
        let selected = input.strings("area_ids")
        let areas = input.strings("area_labels")
        let valid = !selected.isEmpty && input.bool("area_selection_valid", default: true)
        return ProviderUIState(
            screen: "SCR-P05", phase: areas.isEmpty ? .empty : .content, canContinue: valid,
            fieldErrors: valid ? [] : ["provider.onboarding.areas.required"],
            options: input.strings("city_labels"), secondaryOptions: areas,
            selectedCount: selected.count,
            selectedPrimaryIndices: input.strings("city_labels").isEmpty ? [] : [0],
            selectedSecondaryIndices: Array(areas.indices.prefix(selected.count)))
    }

    private static func payout(_ input: [String: ProviderInputValue]) -> ProviderUIState {
        let method = input.text("payout_code")
        let details = input.text("payout_details")
        var errors: [String] = []
        if method.isEmpty { errors.append("provider.onboarding.payout.method.required") }
        if details.isEmpty { errors.append("provider.onboarding.payout.details.required") }
        let actions = input.strings("available_actions")
        let busy = input.text("event") == "submitting"
        return ProviderUIState(
            screen: "SCR-P06", phase: .content,
            canContinue: errors.isEmpty && !busy && actions.contains("submit_provider_application"),
            messageKey: input.text("event") == "not_editable" ? "provider.application.not_editable" : nil,
            fieldErrors: errors, visibleActions: actions, items: input.strings("summary"),
            options: input.strings("payout_labels"),
            secondaryOptions: input.strings("payout_field_labels"),
            selectedPrimaryIndices: method.isEmpty ? [] : [0],
            isBusy: busy, fieldValues: [details])
    }

    private static func status(_ input: [String: ProviderInputValue]) -> ProviderUIState {
        let reason = input.text("reason")
        let display = input.text("display_status")
        return ProviderUIState(
            screen: "SCR-P07", phase: .content, canContinue: true,
            messageKey: display.isEmpty ? statusKey(input.text("status")) : display,
            visibleActions: input.strings("available_actions"),
            items: reason.isEmpty ? [] : [reason])
    }

    private static func home(_ input: [String: ProviderInputValue]) -> ProviderUIState {
        let busy = input.text("event") == "saving"
        return ProviderUIState(
            screen: "SCR-P08", phase: .content,
            canContinue: !busy && input.strings("available_actions").contains("set_availability"),
            messageKey: input.text("event") == "availability_saved"
                ? "provider.home.availability.saved" : nil,
            visibleActions: input.strings("available_actions"),
            items: input.strings("order_summary"), options: input.strings("request_titles"),
            secondaryOptions: input.strings("request_details"), isBusy: busy,
            availableNow: input.bool("available_now"),
            activeOrderID: input.integer("active_order_id", default: 0) > 0
                ? input.integer("active_order_id", default: 0) : nil,
            displayStatus: input.text("display_status"),
            optionCodes: input.strings("request_ids"),
            unreadCount: input.integer("unread_notifications", default: 0))
    }

    private static func availableRequest(_ input: [String: ProviderInputValue]) -> ProviderUIState {
        let busy = input.text("event") == "withdrawing"
        let message: String? =
            switch input.text("event") {
            case "unavailable": "provider.market.request.unavailable"
            case "withdrawn": "provider.offers.withdraw.success"
            default: nil
            }
        return ProviderUIState(
            screen: "SCR-P09", phase: .content,
            canContinue: !busy && !input.strings("available_actions").isEmpty,
            messageKey: message, visibleActions: input.strings("available_actions"),
            items: input.strings("order_summary"), options: input.strings("media_labels"),
            secondaryOptions: input.strings("offer_summary"), isBusy: busy,
            displayStatus: input.text("display_status"),
            customerName: input.text("customer_name"), customerRating: input.text("customer_rating"))
    }

    private static func offerForm(_ input: [String: ProviderInputValue]) -> ProviderUIState {
        let price = Decimal(string: input.text("price"))
        let minimum = Decimal(string: input.text("min_amount")) ?? 0
        let commission = Decimal(string: input.text("commission_rate")) ?? 0
        let selectedETA = input.integer("selected_eta_index", default: -1)
        let selectedDeductible = input.integer("selected_deductible_index", default: -1)
        let now = input.text("timing_type") == "NOW"
        let inspection = input.text("pricing_mode") == "INSPECTION"
        let includes = input.text("includes_text")
        let note = input.text("note")
        var errors: [String] = []
        if price == nil || price! < minimum { errors.append("provider.offer.price.invalid") }
        if now && !input.strings("eta_codes").indices.contains(selectedETA) {
            errors.append("provider.offer.eta.required")
        }
        if inspection && !input.strings("deductible_codes").indices.contains(selectedDeductible) {
            errors.append("provider.offer.deductible.required")
        }
        if includes.isEmpty || includes.count > 150 { errors.append("provider.offer.includes.invalid") }
        if note.count > 300 { errors.append("provider.offer.note.invalid") }
        let busy = input.text("event") == "submitting"
        let success = input.text("event") == "success"
        let net = price.map { NSDecimalNumber(decimal: $0 * (1 - commission)).stringValue } ?? ""
        return ProviderUIState(
            screen: "SCR-P10", phase: .content,
            canContinue: !busy && !success && errors.isEmpty
                && input.strings("available_actions").contains("submit_offer"),
            messageKey: success
                ? "provider.offer.success"
                : (input.text("event") == "unavailable" ? "provider.market.request.unavailable" : nil),
            fieldErrors: errors, visibleActions: input.strings("available_actions"),
            items: input.strings("order_summary"), options: input.strings("eta_labels"),
            secondaryOptions: [net],
            selectedSecondaryIndices: selectedDeductible < 0 ? [] : [selectedDeductible],
            isBusy: busy, fieldValues: [input.text("price"), includes, note],
            selectedOptionIndex: selectedETA, optionCodes: input.strings("eta_codes"),
            showPriceGuide: now, showOutsideReason: inspection, currentAction: "submit_offer",
            itemDetails: input.strings("deductible_labels"),
            itemStates: input.strings("deductible_codes"))
    }

    private static func offers(_ input: [String: ProviderInputValue]) -> ProviderUIState {
        let titles = input.strings("offer_titles")
        let busy = input.text("event") == "withdrawing"
        let message: String? =
            switch input.text("event") {
            case "withdrawn": "provider.offers.withdraw.success"
            case "changed": "provider.offers.withdraw.changed"
            default: nil
            }
        return ProviderUIState(
            screen: "SCR-P11", phase: titles.isEmpty ? .empty : .content,
            canContinue: !titles.isEmpty && !busy, messageKey: message,
            items: titles, secondaryOptions: input.strings("offer_actions"), isBusy: busy,
            selectedOptionIndex: input.integer("selected_offer_index", default: -1),
            currentAction: ["confirm_withdraw", "withdrawing"].contains(input.text("event")) ? "withdraw_offer" : "",
            itemDetails: input.strings("offer_details"), itemStates: input.strings("offer_statuses"))
    }

    private static func activeOrder(
        _ screen: String, _ input: [String: ProviderInputValue]
    ) -> ProviderUIState {
        let busy = input.text("event") == "submitting"
        let reviewRequired = input.bool("price_review_required")
        let explicitMessage = input.text("message_key")
        return ProviderUIState(
            screen: screen, phase: .content,
            canContinue: !busy && !input.strings("available_actions").isEmpty,
            messageKey: !explicitMessage.isEmpty
                ? explicitMessage : (reviewRequired ? "provider.price_guide.other_review" : nil),
            visibleActions: input.strings("available_actions"),
            items: input.strings("order_summary"),
            secondaryOptions: input.strings("amount_summary"), isBusy: busy,
            displayStatus: input.text("display_status"), stepStates: input.strings("step_states"),
            showPriceGuide: !input.text("price_guide_min").isEmpty
                && !input.text("price_guide_max").isEmpty,
            currentAction: input.text("current_action"),
            priceGuideMinimum: input.text("price_guide_min"),
            priceGuideMaximum: input.text("price_guide_max"),
            customerName: input.text("customer_name"), customerRating: input.text("customer_rating"))
    }

    private static func proposal(_ input: [String: ProviderInputValue]) -> ProviderUIState {
        let selected = input.integer("selected_type_index", default: -1)
        let codes = input.strings("proposal_type_codes")
        let type = codes.indices.contains(selected) ? codes[selected] : ""
        let amount = Decimal(string: input.text("amount"))
        let reason = input.text("reason").trimmingCharacters(in: .whitespacesAndNewlines)
        let outsideReason = input.text("outside_reason").trimmingCharacters(in: .whitespacesAndNewlines)
        let minimum = Decimal(string: input.text("price_guide_min"))
        let maximum = Decimal(string: input.text("price_guide_max"))
        let hasGuide = type == "EXECUTION_QUOTE" && minimum != nil && maximum != nil
        let outside = hasGuide && amount != nil && (amount! < minimum! || amount! > maximum!)
        let reviewRequired = input.bool("price_review_required")
        var errors: [String] = []
        if amount == nil || amount! <= 0 { errors.append("provider.proposal.amount.invalid") }
        if !(5...300).contains(reason.count) { errors.append("provider.proposal.reason.invalid") }
        if outside && !(10...300).contains(outsideReason.count) {
            errors.append("provider.proposal.outside_reason.invalid")
        }
        let action = type == "EXECUTION_QUOTE" ? "submit_execution_quote" : "submit_proposal"
        let busy = input.text("event") == "submitting"
        let success = input.text("event") == "success"
        return ProviderUIState(
            screen: "SCR-P20", phase: .content,
            canContinue: !busy && !success && selected >= 0 && errors.isEmpty
                && input.strings("available_actions").contains(action),
            messageKey: success
                ? "provider.proposal.success"
                : (reviewRequired ? "provider.price_guide.other_review" : nil),
            fieldErrors: errors, visibleActions: input.strings("available_actions"),
            options: input.strings("proposal_type_labels"),
            secondaryOptions: [input.text("amount"), input.text("reason"), input.text("outside_reason")],
            isBusy: busy, selectedOptionIndex: selected, optionCodes: codes,
            showPriceGuide: hasGuide, showOutsideReason: outside,
            hasPhoto: input.bool("photo_uploaded"), currentAction: action,
            priceGuideMinimum: input.text("price_guide_min"),
            priceGuideMaximum: input.text("price_guide_max"))
    }

    private static func unable(_ input: [String: ProviderInputValue]) -> ProviderUIState {
        let action = input.text("requested_action")
        let selected = input.integer("selected_reason_index", default: -1)
        let note = input.text("note").trimmingCharacters(in: .whitespacesAndNewlines)
        let noShow = action == "report_no_show"
        var errors: [String] = []
        if !noShow && !input.strings("reason_codes").indices.contains(selected) {
            errors.append("provider.unable.reason.required")
        }
        if action == "report_unable" && !(5...500).contains(note.count) {
            errors.append("provider.unable.note.invalid")
        }
        let busy = input.text("event") == "submitting"
        let explicitMessage = input.text("message_key")
        return ProviderUIState(
            screen: "SCR-P21", phase: .content,
            canContinue: !busy && errors.isEmpty && input.strings("available_actions").contains(action),
            messageKey: !explicitMessage.isEmpty
                ? explicitMessage : (noShow ? "provider.no_show.confirm" : nil),
            fieldErrors: errors, visibleActions: input.strings("available_actions"),
            options: input.strings("reason_labels"), isBusy: busy, fieldValues: [note],
            selectedOptionIndex: selected, optionCodes: input.strings("reason_codes"),
            currentAction: action)
    }

    private static func rating(_ input: [String: ProviderInputValue]) -> ProviderUIState {
        let stars = input.integer("stars")
        let busy = input.text("event") == "submitting"
        let success = input.text("event") == "success"
        let valid = (1...5).contains(stars)
        let explicit = input.text("message_key")
        return ProviderUIState(
            screen: "SCR-P17", phase: .content,
            canContinue: valid && !busy && !success
                && input.strings("available_actions").contains("rate_customer"),
            messageKey: success ? "provider.rating.success" : (explicit.isEmpty ? nil : explicit),
            fieldErrors: !valid && input.text("event") == "validate" ? ["provider.rating.required"] : [],
            visibleActions: input.strings("available_actions"), items: input.strings("order_summary"),
            isBusy: busy, selectedOptionIndex: stars - 1)
    }

    private static func earnings(_ input: [String: ProviderInputValue]) -> ProviderUIState {
        let titles = input.strings("transaction_titles")
        let busy = input.text("event") == "loading_more"
        let empty = titles.isEmpty
        return ProviderUIState(
            screen: "SCR-P18", phase: empty ? .empty : .content,
            canContinue: !empty && !busy,
            messageKey: empty
                ? "provider.earnings.empty"
                : (input.text("operating_mode") == "EMPLOYEE" ? "provider.earnings.employee.info" : nil),
            items: titles, secondaryOptions: input.strings("summary"), isBusy: busy,
            itemDetails: input.strings("transaction_details"),
            itemStates: input.strings("transaction_states"))
    }

    private static func providerProfile(_ input: [String: ProviderInputValue]) -> ProviderUIState {
        let experience = Int(input.text("experience_years"))
        let specialties = input.strings("specialty_ids")
        let areas = input.strings("area_ids")
        let busy = ["uploading", "saving"].contains(input.text("event"))
        var errors: [String] = []
        if experience == nil || !(0...50).contains(experience!) {
            errors.append("provider.onboarding.experience.invalid")
        }
        if input.text("bio").count > 300 { errors.append("provider.onboarding.bio.max") }
        if specialties.isEmpty { errors.append("provider.onboarding.specialties.required") }
        if areas.isEmpty { errors.append("provider.onboarding.areas.required") }
        let portfolio = input.strings("portfolio_labels")
        let message =
            input.text("event") == "saved"
            ? "provider.profile.saved" : (portfolio.count >= 20 ? "provider.profile.portfolio.full" : nil)
        return ProviderUIState(
            screen: "SCR-P19", phase: .content,
            canContinue: !busy && errors.isEmpty
                && input.strings("available_actions").contains("update_provider_profile"),
            messageKey: message, fieldErrors: errors,
            visibleActions: input.strings("available_actions"), items: portfolio,
            options: input.strings("category_labels"),
            secondaryOptions: input.strings("specialty_labels"),
            selectedCount: specialties.count + areas.count,
            selectedPrimaryIndices: Array(
                input.strings("specialty_labels").indices.prefix(specialties.count)),
            selectedSecondaryIndices: Array(input.strings("area_labels").indices.prefix(areas.count)),
            isBusy: busy, hasProfilePhoto: input.bool("profile_photo_uploaded"),
            fieldValues: [
                input.text("experience_years"), input.text("bio"), input.text("portfolio_caption"),
            ],
            customerName: input.text("name"), customerRating: input.text("rating"),
            itemDetails: input.strings("portfolio_ids"), itemStates: input.strings("profile_stats"),
            areaOptions: input.strings("area_labels"))
    }

    private static func chat(_ input: [String: ProviderInputValue]) -> ProviderUIState {
        let messages = input.strings("message_bodies")
        let status = input.text("conversation_status")
        let busy = input.text("event") == "sending"
        let draft = input.text("draft")
        let counterpart =
            input.text("counterpart_name").isEmpty
            ? input.text("provider_name") : input.text("counterpart_name")
        return ProviderUIState(
            screen: "SCR-C19", phase: messages.isEmpty ? .empty : .content,
            canContinue: status == "OPEN" && !draft.isEmpty && draft.count <= 1000 && !busy,
            messageKey: status == "READ_ONLY"
                ? "chat.read_only"
                : (input.strings("message_kinds").contains { $0.hasSuffix("_blocked") }
                    ? "chat.masking.notice" : nil),
            items: messages,
            options: [counterpart, input.text("order_summary"), status, input.text("viewer_role")],
            isBusy: busy, fieldValues: [draft], itemDetails: input.strings("message_times"),
            itemStates: input.strings("message_kinds"))
    }

    private static func statusKey(_ status: String) -> String? {
        switch status {
        case "PENDING_REVIEW": return "provider.application.pending"
        case "REJECTED": return "provider.application.rejected"
        case "ACTIVE": return "provider.application.active"
        case "SUSPENDED": return "provider.application.suspended"
        default: return nil
        }
    }
}

extension Dictionary where Key == String, Value == ProviderInputValue {
    fileprivate func text(_ key: String) -> String {
        guard case let .text(value)? = self[key] else { return "" }
        return value
    }

    fileprivate func strings(_ key: String) -> [String] {
        guard case let .strings(value)? = self[key] else { return [] }
        return value
    }

    fileprivate func bool(_ key: String, default defaultValue: Bool = false) -> Bool {
        guard case let .bool(value)? = self[key] else { return defaultValue }
        return value
    }

    fileprivate func integer(_ key: String, default defaultValue: Int = 0) -> Int {
        guard case let .integer(value)? = self[key] else { return defaultValue }
        return value
    }
}

private struct ProviderNotificationsEnvelope: Decodable, Sendable { let data: [CustomerNotification] }

private struct ProviderNotificationReadBody: Encodable, Sendable {
    let notificationIds: [String]
    enum CodingKeys: String, CodingKey { case notificationIds = "notification_ids" }
}
