// swiftlint:disable file_length
import Foundation
import Observation

public enum CustomerPhase: String, Codable, Sendable {
    case empty, loading, content, error, offline
}

public struct CustomerUIState: Equatable, Sendable {
    public var screen: String
    public var phase: CustomerPhase
    public var canContinue: Bool
    public var messageKey: String?
    public var descriptionError: String?
    public var termsError: String?
    public var showPricing: Bool
    public var showMap: Bool
    public var showEta: Bool
    public var showWaiting: Bool
    public var showStepper: Bool
    public var showStatusCard: Bool
    public var etaApproximate: Bool
    public var visibleActions: [String]
    public var displayStatus: String
    public var items: [String]
    public var itemDetails: [String]
    public var itemStates: [String]
    public var options: [String]
    public var selectedIndex: Int
    public var selectedOptionIndex: Int
    public var fieldValues: [String]
    public var fieldErrors: [String]
    public var isBusy: Bool
    public var selectionMode: Bool
    public var isEditing: Bool
    public var isDefault: Bool
    public var showConfirmation: Bool
    public var pendingIndex: Int
    public var countdownSeconds: Int
    public var hasPhoto: Bool
    public var stepStates: [String]
    public var ratings: [Int]
    public var mapLatitude: Double?
    public var mapLongitude: Double?
    public var notificationsDenied: Bool
    public var ratingRemindersEnabled: Bool
    /// DEC-059: catalog icon keys beside `options` (C01, C03).
    public var optionIcons: [String] = []
    public var materialOptions: [String] = []
    public var selectedMaterialIndex = -1
    public var pricingOptions: [String] = []
    public var selectedPricingIndex = -1
    public var noBudget = false
    public var providerVerified = false

    public init(
        screen: String, phase: CustomerPhase, canContinue: Bool = false,
        messageKey: String? = nil, descriptionError: String? = nil, termsError: String? = nil,
        showPricing: Bool = false, showMap: Bool = false, showEta: Bool = false,
        showWaiting: Bool = false,
        showStepper: Bool = false, showStatusCard: Bool = false, etaApproximate: Bool = false,
        visibleActions: [String] = [], displayStatus: String = "", items: [String] = [],
        itemDetails: [String] = [], itemStates: [String] = [], options: [String] = [],
        selectedIndex: Int = -1, selectedOptionIndex: Int = -1, fieldValues: [String] = [],
        fieldErrors: [String] = [], isBusy: Bool = false, selectionMode: Bool = false,
        isEditing: Bool = false, isDefault: Bool = false,
        showConfirmation: Bool = false, pendingIndex: Int = -1,
        countdownSeconds: Int = 0, hasPhoto: Bool = false, stepStates: [String] = [],
        ratings: [Int] = [], mapLatitude: Double? = nil, mapLongitude: Double? = nil,
        notificationsDenied: Bool = false, ratingRemindersEnabled: Bool = true
    ) {
        self.screen = screen
        self.phase = phase
        self.canContinue = canContinue
        self.messageKey = messageKey
        self.descriptionError = descriptionError
        self.termsError = termsError
        self.showPricing = showPricing
        self.showMap = showMap
        self.showEta = showEta
        self.showWaiting = showWaiting
        self.showStepper = showStepper
        self.showStatusCard = showStatusCard
        self.etaApproximate = etaApproximate
        self.visibleActions = visibleActions
        self.displayStatus = displayStatus
        self.items = items
        self.itemDetails = itemDetails
        self.itemStates = itemStates
        self.options = options
        self.selectedIndex = selectedIndex
        self.selectedOptionIndex = selectedOptionIndex
        self.fieldValues = fieldValues
        self.fieldErrors = fieldErrors
        self.isBusy = isBusy
        self.selectionMode = selectionMode
        self.isEditing = isEditing
        self.isDefault = isDefault
        self.showConfirmation = showConfirmation
        self.pendingIndex = pendingIndex
        self.countdownSeconds = countdownSeconds
        self.hasPhoto = hasPhoto
        self.stepStates = stepStates
        self.ratings = ratings
        self.mapLatitude = mapLatitude
        self.mapLongitude = mapLongitude
        self.notificationsDenied = notificationsDenied
        self.ratingRemindersEnabled = ratingRemindersEnabled
    }
}

public enum CustomerInputValue: Equatable, Sendable {
    case text(String)
    case bool(Bool)
    case integer(Int)
    case strings([String])
}

public enum CustomerLogic {
    /// C16 day chips from the real dates (6ب, `design/fixtures/navigation/slot-days.json`): today, tomorrow, then
    /// the weekday name. `names` is today, tomorrow, Monday…Sunday.
    public static func slotDayLabels(isoDates: [String], names: [String]) -> [String] {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC") ?? .current
        let formatter = DateFormatter()
        formatter.calendar = calendar
        formatter.timeZone = calendar.timeZone
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return isoDates.enumerated().map { index, value in
            if index < 2 { return names[index] }
            guard let date = formatter.date(from: value) else { return value }
            // Calendar weekday: Sunday = 1 … Saturday = 7; names start at Monday.
            let mondayBased = (calendar.component(.weekday, from: date) + 5) % 7
            return names[2 + mondayBased]
        }
    }

    public static func reduce(screen: String, input: [String: CustomerInputValue]) -> CustomerUIState {
        if let transientState = transientState(screen: screen, event: input.text("event")) {
            return transientState
        }
        return screenState(screen: screen, input: input)
    }

    private static func transientState(screen: String, event: String) -> CustomerUIState? {
        switch event {
        case "loading": return CustomerUIState(screen: screen, phase: .loading)
        case "error": return CustomerUIState(screen: screen, phase: .error, messageKey: "error.body")
        default: return nil
        }
    }

    private static func screenState(screen: String, input: [String: CustomerInputValue])
        -> CustomerUIState
    {
        switch screen {
        case "SCR-C01": return home(input)
        case "SCR-C02": return account(input)
        case "SCR-C03": return problem(input)
        case "SCR-C04": return timing(input)
        case "SCR-C05": return review(input)
        case "SCR-C06": return offers(input)
        case "SCR-C07": return provider(input)
        case "SCR-C08": return offer(input)
        case "SCR-C09": return tracking(input)
        default: return extendedScreenState(screen: screen, input: input)
        }
    }

    // swiftlint:disable:next cyclomatic_complexity
    private static func extendedScreenState(
        screen: String, input: [String: CustomerInputValue]
    ) -> CustomerUIState {
        switch screen {
        case "SCR-C14": return addresses(input)
        case "SCR-C15": return addressForm(input)
        case "SCR-C16": return slots(input)
        case "SCR-C17": return media(input)
        case "SCR-C18": return conversations(input)
        case "SCR-C19": return chat(input)
        case "SCR-C20": return cancellation(input)
        case "SCR-C21": return paymentSummary(input)
        case "SCR-C22": return electronicPayment(input)
        case "SCR-C23": return completion(input)
        case "SCR-C24": return rating(input)
        case "SCR-C25": return orders(input)
        case "SCR-C26": return orderHistory(input)
        case "SCR-C27": return dispute(input)
        case "SCR-C28": return providerReport(input)
        case "SCR-C29": return help(input)
        case "SCR-C30": return proposal(input, screen: "SCR-C30")
        case "SCR-C31": return proposal(input, screen: "SCR-C31")
        case "SCR-C32": return notifications(input)
        case "SCR-C33": return accountSettings(input)
        case "SCR-C34": return terms(input)
        case "SCR-C35": return noOffers(input)
        case "SCR-C36": return assignment(input)
        default: preconditionFailure("Unknown customer screen: \(screen)")
        }
    }

    private static func home(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let titles = input.strings("order_titles")
        let hasOrders = input["has_orders"] == nil ? !titles.isEmpty : input.bool("has_orders")
        var state = CustomerUIState(
            screen: "SCR-C01", phase: hasOrders ? .content : .empty,
            canContinue: true, showPricing: input.text("operating_mode") != "staff",
            items: titles, itemDetails: input.strings("order_subtitles"),
            itemStates: input.strings("order_statuses"), options: input.strings("category_labels"),
            selectedIndex: input.integer("selected_category_index", default: -1),
            // The default address labels the home header's location row (DEC-063).
            fieldValues: [input.text("customer_name"), input.text("address_label")])
        state.optionIcons = input.strings("category_icons")
        return state
    }

    private static func account(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        CustomerUIState(
            screen: "SCR-C02", phase: .content, canContinue: true,
            messageKey: input.bool("is_verified") ? "account.verified" : "account.unverified",
            fieldValues: [input.text("name"), input.text("phone")]
        )
    }

    /// C03 (DEC-059): catalog categories and the chosen category's problem types; `is_other` needs 10 characters (BR-013).
    private static func problem(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let selected = input.integer("selected_problem_index", default: -1)
        let other = input.strings("problem_other")
        let isOther = other.indices.contains(selected) && other[selected] == "true"
        let description = input.text("description")
        let error: String? =
            if selected < 0 {
                "request.problem.required"
            } else if isOther && description.trimmingCharacters(in: .whitespacesAndNewlines).count < 10 {
                "request.description.other_min"
            } else if description.count > 1000 {
                "request.description.max"
            } else {
                nil
            }
        var state = CustomerUIState(
            screen: "SCR-C03", phase: selected < 0 ? .empty : .content,
            canContinue: error == nil, descriptionError: error,
            itemDetails: input.strings("problem_labels"), options: input.strings("category_labels"),
            selectedIndex: input.integer("selected_category_index", default: -1),
            selectedOptionIndex: selected,
            fieldValues: [description, String(input.integer("media_count", default: 0))])
        state.optionIcons = input.strings("category_icons")
        return state
    }

    /// C04: address, timing, materials; marketplace adds pricing mode and an optional budget (07, BR-015).
    private static func timing(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let timingType = input.text("timing_type")
        let hasAddress = !input.text("address_id").isEmpty
        let marketplace = input.text("operating_mode") != "staff"
        let budget = input.text("budget").trimmingCharacters(in: .whitespaces)
        let budgetValid = budget.isEmpty || (Decimal(string: budget).map { $0 > 0 } ?? false)
        let materialSelected = input.integer("selected_material_index", default: -1) >= 0
        let pricingSelected = !marketplace || input.integer("selected_pricing_index", default: -1) >= 0
        let message: String? =
            if !hasAddress {
                "request.address.required"
            } else if input["category_available"] != nil && !input.bool("category_available") {
                "request.category.unavailable"
            } else if timingType == "now" && !input.bool("now_available") {
                "request.timing.now_unavailable"
            } else if timingType == "scheduled" && input.text("slot_id").isEmpty {
                "request.slot.required"
            } else {
                nil
            }
        var state = CustomerUIState(
            screen: "SCR-C04", phase: .content,
            canContinue: hasAddress && !timingType.isEmpty && message == nil && materialSelected
                && pricingSelected && (!marketplace || budgetValid),
            messageKey: message, showPricing: marketplace,
            options: input.strings("timing_labels"),
            selectedOptionIndex: input.integer("selected_timing_index", default: -1),
            fieldValues: [input.text("address_label"), budget, input.text("slot_label")],
            fieldErrors: marketplace && !budgetValid ? ["request.budget.invalid"] : [])
        state.materialOptions = input.strings("material_labels")
        state.selectedMaterialIndex = input.integer("selected_material_index", default: -1)
        state.pricingOptions = marketplace ? input.strings("pricing_labels") : []
        state.selectedPricingIndex = input.integer("selected_pricing_index", default: -1)
        state.noBudget = input.bool("no_budget")
        return state
    }

    /// C05: the real draft (DEC-059) — service, visit, and in marketplace the pricing rows.
    private static func review(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let accepted = input.bool("terms_accepted")
        let marketplace = input.text("operating_mode") != "staff"
        let submitting = input.text("event") == "submitting"
        let refused = input["error_message"] != nil
        // A refused publish stays here with the server's reason; an empty reason means the shared network error.
        return CustomerUIState(
            screen: "SCR-C05", phase: .content, canContinue: accepted && !submitting,
            messageKey: refused ? "request.publish.error" : nil,
            termsError: accepted ? nil : "request.terms.required", showPricing: marketplace,
            items: input.strings("service_rows"), itemDetails: input.strings("visit_rows"),
            itemStates: marketplace ? input.strings("pricing_rows") : [],
            fieldValues: refused ? [input.text("error_message")] : [], isBusy: submitting)
    }

    /// C06: offers from `GET /orders/{id}/offers`, one pipe-joined row each: id|name|rating|services|price|detail|badge|verified.
    private static func offers(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let rows = input.strings("offer_rows")
        let count = input["offer_rows"] == nil ? input.integer("offer_count", default: 0) : rows.count
        var state = CustomerUIState(
            screen: "SCR-C06", phase: count == 0 ? .empty : .content,
            canContinue: true, showPricing: count > 0,
            showEta: input.text("timing_type") == "now",
            visibleActions: input.strings("available_actions"),
            displayStatus: input.text("display_status"), items: rows,
            selectedIndex: input.integer("sort_index", default: 0),
            fieldValues: [input.text("order_title")],
            countdownSeconds: input.integer("countdown_seconds", default: 0))
        state.pricingOptions = [input.text("pricing_mode")]
        return state
    }

    /// C07 from `GET /providers/{id}?order_id=`: header, stats, specialties, rating breakdown, reviews (name|stars|comment|date).
    private static func provider(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let available = input.bool("provider_available")
        var state = CustomerUIState(
            screen: "SCR-C07", phase: available ? .content : .empty,
            canContinue: available && input.strings("available_actions").contains("accept_offer"),
            visibleActions: input.strings("available_actions"), items: input.strings("review_rows"),
            itemDetails: input.strings("rating_values"), options: input.strings("specialties"),
            fieldValues: [
                input.text("provider_name"), input.text("provider_rating"), input.text("provider_services"),
                input.text("provider_experience"), input.text("provider_about"), input.text("offer_price"),
            ])
        state.providerVerified = input.bool("provider_verified")
        return state
    }

    /// C08: the chosen offer, the order, and the payment method (cash or electronic) sent with O-03.
    private static func offer(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let payment = input.integer("selected_payment_index", default: -1)
        let actions = input.strings("available_actions")
        let submitting = input.text("event") == "submitting"
        var state = CustomerUIState(
            screen: "SCR-C08", phase: .content,
            canContinue: actions.contains("accept_offer") && payment >= 0 && !submitting,
            showPricing: true, showEta: input.text("timing_type") == "now", visibleActions: actions,
            items: input.strings("offer_rows"), itemDetails: input.strings("order_rows"),
            options: input.strings("payment_labels"), selectedOptionIndex: payment,
            fieldValues: [
                input.text("provider_name"), input.text("provider_rating"), input.text("provider_services"),
                input.text("eta_minutes"),
            ],
            isBusy: submitting)
        state.providerVerified = input.bool("provider_verified")
        state.pricingOptions = [input.text("pricing_mode")]
        return state
    }

    /// C36 (employee mode): the published order waiting for assignment, actions from the server.
    private static func assignment(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        CustomerUIState(
            screen: "SCR-C36", phase: .content, canContinue: true,
            visibleActions: input.strings("available_actions"), items: input.strings("order_rows"),
            fieldValues: [input.text("order_title")])
    }

    private static func tracking(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let status = input.text("status")
        let terminal = ["OPEN", "CANCELLED", "EXPIRED"].contains(status)
        return CustomerUIState(
            screen: "SCR-C09", phase: input.text("event") == "offline" ? .offline : .content,
            canContinue: true, messageKey: trackingMessage(status),
            showMap: status == "ON_THE_WAY", showEta: status == "ON_THE_WAY",
            showWaiting: status == "CONFIRMED",
            showStepper: !terminal && input.bool("stepper"), showStatusCard: terminal,
            etaApproximate: input.bool("eta_approximate"),
            visibleActions: input.strings("available_actions"),
            displayStatus: input.text("display_status"), items: input.strings("summary"),
            stepStates: input.strings("step_states")
        )
    }

    /// The status card names the real terminal state; OPEN and EXPIRED are not cancellations.
    private static func trackingMessage(_ status: String) -> String? {
        switch status {
        case "DISPUTED": "status.reviewing"
        case "CANCELLED": "tracking.status.cancelled"
        case "EXPIRED": "tracking.status.expired"
        case "OPEN": "tracking.status.open"
        default: nil
        }
    }

    private static func addresses(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let labels = input.strings("address_labels")
        let event = input.text("event")
        let busy = event == "deleting"
        let selected = input.integer("selected_index", default: -1)
        return CustomerUIState(
            screen: "SCR-C14", phase: labels.isEmpty ? .empty : .content,
            canContinue: !busy && (!input.bool("selection_mode") || selected >= 0),
            items: labels, itemDetails: input.strings("address_details"), selectedIndex: selected,
            isBusy: busy, selectionMode: input.bool("selection_mode"),
            showConfirmation: event == "confirm_delete" || busy,
            pendingIndex: input.integer("pending_delete_index", default: -1))
    }

    private static func addressForm(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let label = input.text("label")
        let area = input.text("area_id")
        let details = input.text("address_text")
        let latitude = Double(input.text("lat"))
        let longitude = Double(input.text("lng"))
        var errors: [String] = []
        if label.isEmpty || label.count > 50 { errors.append("address.validation.label_required") }
        if area.isEmpty { errors.append("address.validation.area_required") }
        if details.isEmpty || details.count > 255 {
            errors.append("address.validation.details_required")
        }
        if latitude == nil || !(-90...90).contains(latitude!) || longitude == nil
            || !(-180...180).contains(longitude!)
        {
            errors.append("address.validation.location_required")
        }
        if !area.isEmpty && !input.bool("area_served") {
            errors.append("address.validation.unserved_area")
        }
        let busy = input.text("event") == "saving"
        return CustomerUIState(
            screen: "SCR-C15", phase: label.isEmpty && details.isEmpty ? .empty : .content,
            canContinue: errors.isEmpty && !busy,
            messageKey: errors.first(where: { $0 == "address.validation.unserved_area" }),
            showMap: true, options: input.strings("area_labels"),
            selectedOptionIndex: input.integer("selected_area_index", default: -1),
            fieldValues: [
                label, details, input.text("building"), input.text("floor"),
                input.text("apartment"), input.text("landmark"),
            ], fieldErrors: errors, isBusy: busy, isEditing: input.bool("editing"),
            isDefault: input.bool("is_default"), mapLatitude: latitude, mapLongitude: longitude)
    }

    private static func slots(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let periods = input.strings("slot_labels")
        let changingDay = input.text("event") == "changing_day"
        let selected = input.integer("selected_slot_index", default: -1)
        return CustomerUIState(
            screen: "SCR-C16",
            phase: changingDay ? .loading : (periods.isEmpty ? .empty : .content),
            canContinue: !changingDay && selected >= 0, items: periods,
            options: input.strings("day_labels"), selectedIndex: selected,
            selectedOptionIndex: input.integer("selected_day_index"), isBusy: changingDay)
    }

    private static func media(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let kinds = input.strings("media_kinds")
        let states = input.strings("media_states")
        var errors: [String] = []
        if input.integer("photo_count") > 5 { errors.append("media.validation.photo_limit") }
        if input.integer("video_count") > 1 { errors.append("media.validation.video_limit") }
        if input.integer("audio_count") > 1 { errors.append("media.validation.audio_limit") }
        if states.contains("failed") { errors.append("media.validation.rejected") }
        let busy = input.bool("recording") || states.contains("uploading")
        let message = input.bool("recording") ? "media.recording" : errors.first
        return CustomerUIState(
            screen: "SCR-C17", phase: kinds.isEmpty ? .empty : .content,
            canContinue: !errors.contains(where: { $0 != "media.validation.rejected" }) && !busy,
            messageKey: message, items: kinds, itemStates: states, fieldErrors: errors,
            isBusy: busy)
    }

    private static func cancellation(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let selected = input.integer("selected_reason_index", default: -1)
        let busy = input.text("event") == "submitting"
        let actions = input.strings("available_actions")
        return CustomerUIState(
            screen: "SCR-C20", phase: .content,
            canContinue: selected >= 0 && actions.contains("cancel") && !busy,
            visibleActions: actions, options: input.strings("reason_labels"),
            selectedOptionIndex: selected, fieldValues: [input.text("note")], isBusy: busy)
    }

    private static func dispute(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let event = input.text("event")
        let selected = input.integer("selected_reason_index", default: -1)
        let description = input.text("description")
        let existing = input.bool("existing")
        let busy = event == "submitting"
        return CustomerUIState(
            screen: "SCR-C27", phase: .content,
            canContinue: !existing && selected >= 0
                && !description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                && description.count <= 1_000 && input.strings("available_actions").contains("open_dispute")
                && !busy,
            messageKey: event == "success" ? "dispute.success" : nil,
            descriptionError: !existing && description.count > 1_000 ? "request.description.max" : nil,
            visibleActions: input.strings("available_actions"), items: input.strings("details"),
            itemStates: input.strings("status_details"), options: input.strings("reason_labels"),
            selectedOptionIndex: selected, fieldValues: [description], isBusy: busy,
            hasPhoto: input.integer("photo_count") > 0)
    }

    private static func providerReport(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let event = input.text("event")
        let selected = input.integer("selected_reason_index", default: -1)
        let description = input.text("description")
        let busy = event == "submitting"
        return CustomerUIState(
            screen: "SCR-C28", phase: .content,
            canContinue: selected >= 0 && description.count <= 1_000
                && input.strings("available_actions").contains("report_provider") && !busy,
            messageKey: event == "success" ? "provider.report.success" : nil,
            descriptionError: description.count > 1_000 ? "request.description.max" : nil,
            visibleActions: input.strings("available_actions"), options: input.strings("reason_labels"),
            selectedOptionIndex: selected, fieldValues: [description], isBusy: busy)
    }

    private static func help(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let subject = input.text("subject")
        let message = input.text("message")
        let busy = input.text("event") == "submitting"
        var errors: [String] = []
        if !subject.isEmpty && !(5...120).contains(subject.count) {
            errors.append("help.validation.subject")
        }
        if !message.isEmpty && !(10...2_000).contains(message.count) {
            errors.append("help.validation.message")
        }
        return CustomerUIState(
            screen: "SCR-C29", phase: .content,
            canContinue: (5...120).contains(subject.count) && (10...2_000).contains(message.count)
                && !busy,
            messageKey: input.text("event") == "success" ? "help.success" : nil,
            items: input.strings("faq_questions"), itemDetails: input.strings("faq_answers"),
            selectedIndex: input.integer("expanded_index", default: -1), fieldValues: [subject, message],
            fieldErrors: errors, isBusy: busy)
    }

    private static func notifications(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let titles = input.strings("notification_titles")
        return CustomerUIState(
            screen: "SCR-C32", phase: titles.isEmpty ? .empty : .content,
            canContinue: !titles.isEmpty, items: titles,
            itemDetails: input.strings("notification_bodies"),
            itemStates: input.strings("notification_states"), options: input.strings("deep_links"),
            fieldValues: input.strings("notification_times"),
            isBusy: input.text("event") == "marking_read",
            notificationsDenied: input.bool("notifications_denied"))
    }

    private static func accountSettings(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let mode = input.text("mode").isEmpty ? "overview" : input.text("mode")
        let values = input.strings("field_values")
        var errors: [String] = []
        if mode == "edit"
            && (values.first ?? "").trimmingCharacters(in: .whitespacesAndNewlines).count < 2
        {
            errors.append("account.validation.name")
        }
        if mode == "password" && (values.first ?? "").isEmpty {
            errors.append("account.validation.current_password")
        }
        if mode == "password" && (values[safe: 1] ?? "").count < 8 {
            errors.append("account.validation.new_password")
        }
        if mode == "password" && values[safe: 1] != values[safe: 2] {
            errors.append("account.validation.password_confirmation")
        }
        let busy = input.text("event") == "submitting"
        let messageKey: String? =
            switch input.text("event") {
            case "success": "account.settings.success"
            case "active_order": "account.delete.active_order"
            default: nil
            }
        return CustomerUIState(
            screen: "SCR-C33", phase: .content,
            canContinue: !busy && (mode == "overview" || errors.isEmpty), messageKey: messageKey,
            visibleActions: input.strings("available_actions"), items: [mode], fieldValues: values,
            fieldErrors: errors, isBusy: busy, isEditing: mode != "overview",
            showConfirmation: input.text("event") == "confirm_delete" || (busy && mode == "delete"),
            ratingRemindersEnabled: input.optionalBool("rating_reminders_enabled") ?? true)
    }

    private static func terms(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        CustomerUIState(
            screen: "SCR-C34", phase: .content, canContinue: !input.text("body").isEmpty,
            items: [input.text("body")], itemDetails: [input.text("version"), input.text("effective_at")])
    }

    private static func noOffers(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let actions = input.strings("available_actions")
        return CustomerUIState(
            screen: "SCR-C35", phase: .empty,
            canContinue: !actions.isEmpty && input.text("event") != "submitting",
            visibleActions: actions, isBusy: input.text("event") == "submitting")
    }

    private static func conversations(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let providers = input.strings("provider_names")
        return CustomerUIState(
            screen: "SCR-C18", phase: providers.isEmpty ? .empty : .content,
            canContinue: !providers.isEmpty, items: providers,
            itemDetails: input.strings("order_summaries"),
            itemStates: input.strings("conversation_statuses"),
            options: input.strings("last_messages"),
            fieldValues: input.strings("last_message_times"),
            fieldErrors: input.strings("provider_verified"))
    }

    private static func chat(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let messages = input.strings("message_bodies")
        let status = input.text("conversation_status")
        let busy = input.text("event") == "sending"
        let draft = input.text("draft")
        let message: String? =
            if status == "READ_ONLY" {
                "chat.read_only"
            } else if input.strings("message_kinds").contains(where: { $0.hasSuffix("_blocked") }) {
                "chat.masking.notice"
            } else { nil }
        return CustomerUIState(
            screen: "SCR-C19", phase: messages.isEmpty ? .empty : .content,
            canContinue: status == "OPEN"
                && !draft.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                && draft.count <= 1_000 && !busy,
            messageKey: message, items: messages,
            itemDetails: input.strings("message_times"), itemStates: input.strings("message_kinds"),
            options: [
                input.text("counterpart_name").isEmpty
                    ? input.text("provider_name") : input.text("counterpart_name"),
                input.text("order_summary"), status, input.text("viewer_role"),
            ],
            fieldValues: [draft], isBusy: busy)
    }

    private static func orders(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let titles = input.strings("order_titles")
        let tab = input.text("tab")
        let busy = input.text("event") == "loading_more"
        return CustomerUIState(
            screen: "SCR-C25", phase: titles.isEmpty ? .empty : .content,
            canContinue: !titles.isEmpty && !busy,
            messageKey: titles.isEmpty ? "orders.\(tab).empty.title" : nil,
            items: titles, itemDetails: input.strings("order_subtitles"),
            itemStates: input.strings("order_statuses"),
            selectedOptionIndex: tab == "past" ? 1 : 0, isBusy: busy)
    }

    private static func orderHistory(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let actions = input.strings("available_actions")
        let status = input.text("status")
        let message: String? =
            if status == "CANCELLED" { "order.cancelled.reason" } else if status == "EXPIRED" {
                "order.expired.reason"
            } else { nil }
        return CustomerUIState(
            screen: "SCR-C26", phase: .content, canContinue: !actions.isEmpty,
            messageKey: message, visibleActions: actions, displayStatus: input.text("display_status"),
            items: input.strings("summary"), itemDetails: input.strings("proposal_summaries"),
            options: [input.text("provider_name"), input.text("termination_reason")])
    }

    private static func paymentSummary(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let method = input.text("payment_method")
        let actions = input.strings("available_actions")
        let busy = input.text("event") == "switching"
        return CustomerUIState(
            screen: "SCR-C21", phase: .content,
            canContinue: method == "ELECTRONIC" && actions.contains("pay_electronic") && !busy,
            visibleActions: actions, items: input.strings("amounts"),
            options: input.strings("method_labels"),
            selectedOptionIndex: input.integer("selected_method_index", default: -1),
            isBusy: busy)
    }

    private static func electronicPayment(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let event = input.text("event")
        let status = input.text("payment_status")
        let selected = input.integer("selected_channel_index", default: -1)
        let busy = event == "creating"
        let actions = input.strings("available_actions")
        let message: String? =
            if busy { "payment.creating" } else if status == "SUCCEEDED" {
                "payment.success"
            } else if status == "FAILED" { "payment.failed" } else if status == "EXPIRED" {
                "payment.expired"
            } else if status == "PENDING" && !input.text("reference_number").isEmpty {
                "payment.kiosk.ready"
            } else if status == "PENDING" { "payment.checkout.ready" } else { nil }
        return CustomerUIState(
            screen: "SCR-C22", phase: .content,
            canContinue: !busy && status != "PENDING" && status != "SUCCEEDED" && selected >= 0
                && actions.contains("pay_electronic"),
            messageKey: message, visibleActions: actions,
            items: [input.text("total"), input.text("reference_number"), input.text("checkout_url")],
            itemStates: [status], options: input.strings("channel_labels"),
            selectedOptionIndex: selected, isBusy: busy,
            countdownSeconds: input.integer("countdown_seconds"))
    }

    private static func completion(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let actions = input.strings("available_actions")
        let busy = input.text("event") == "submitting"
        return CustomerUIState(
            screen: "SCR-C23", phase: .content,
            canContinue: actions.contains("confirm_completion") && !busy,
            visibleActions: actions, items: input.strings("summary"), isBusy: busy)
    }

    private static func rating(_ input: [String: CustomerInputValue]) -> CustomerUIState {
        let ratings = input.strings("ratings").compactMap(Int.init)
        let complete = ratings.count == 3 && ratings.allSatisfy { (1...5).contains($0) }
        let commentLength = input.integer("comment_length", default: input.text("comment").count)
        let busy = input.text("event") == "submitting"
        var errors: [String] = []
        if !complete { errors.append("rating.validation.required") }
        if commentLength > 500 { errors.append("rating.validation.comment_max") }
        let message: String? =
            switch input.text("event") {
            case "window_closed": "rating.window_closed"
            case "already_exists": "rating.already_exists"
            case "success": "rating.success"
            default: nil
            }
        return CustomerUIState(
            screen: "SCR-C24",
            phase: input.text("event") == "loaded" && ratings.allSatisfy { $0 == 0 } ? .empty : .content,
            canContinue: complete && errors.isEmpty && !busy
                && input.strings("available_actions").contains("rate"),
            messageKey: message, visibleActions: input.strings("available_actions"),
            fieldValues: [input.text("comment")], fieldErrors: errors, isBusy: busy, ratings: ratings)
    }

    private static func proposal(
        _ input: [String: CustomerInputValue], screen: String
    ) -> CustomerUIState {
        let actions = input.strings("available_actions")
        let expired = input.bool("expired")
        let busy = input.text("event") == "submitting"
        let actionable = actions.contains("approve_proposal") || actions.contains("reject_proposal")
        return CustomerUIState(
            screen: screen, phase: .content, canContinue: !expired && !busy && actionable,
            messageKey: expired ? "proposal.expired" : (busy ? "proposal.deciding" : nil),
            visibleActions: actions,
            items: [
                input.text("type_label"), input.text("amount"), input.text("reason"), input.text("total"),
            ],
            isBusy: busy, countdownSeconds: input.integer("countdown_seconds"),
            hasPhoto: input.bool("has_photo"))
    }
}

extension Dictionary where Key == String, Value == CustomerInputValue {
    fileprivate func text(_ key: String) -> String {
        if case let .text(value) = self[key] { value } else { "" }
    }
    fileprivate func bool(_ key: String) -> Bool {
        if case let .bool(value) = self[key] { value } else { false }
    }
    fileprivate func optionalBool(_ key: String) -> Bool? {
        if case let .bool(value) = self[key] { value } else { nil }
    }
    fileprivate func integer(_ key: String, default defaultValue: Int = 0) -> Int {
        if case let .integer(value) = self[key] { value } else { defaultValue }
    }
    fileprivate func strings(_ key: String) -> [String] {
        if case let .strings(value) = self[key] { value } else { [] }
    }
}

public struct CustomerOrderSummary: Codable, Equatable, Sendable {
    public let id: Int
    public let number: String?
    public let status: String?
    public let version: Int?
    public let displayStatus: String
    public let statusLabel: String?
    public let availableActions: [String]
    public let stepper: [CustomerOrderStep]?
    public let amounts: CustomerOrderAmounts?
    public let provider: CustomerProviderSummary?
    public let latestPayment: CustomerPayment?
    public let category: CustomerOrderReference?
    public let problemType: CustomerOrderReference?
    public let customerAddressId: Int?
    public let description: String?
    public let pricingMode: String?
    public let budgetAmount: String?
    public let materialsResponsibility: String?
    public let location: CustomerOrderLocation?
    public let timing: CustomerOrderTiming?
    public let deadlines: CustomerOrderDeadlines?
    public let termination: CustomerOrderTermination?
    public let createdAt: String?

    enum CodingKeys: String, CodingKey {
        case id, number, status, version, stepper, amounts, provider, category, description, location,
            timing, deadlines, termination
        case problemType = "problem_type"
        case customerAddressId = "customer_address_id"
        case pricingMode = "pricing_mode"
        case budgetAmount = "budget_amount"
        case materialsResponsibility = "materials_responsibility"
        case createdAt = "created_at"
        case displayStatus = "display_status"
        case statusLabel = "status_label"
        case availableActions = "available_actions"
        case latestPayment = "latest_payment"
    }
}

public struct CustomerOrderDeadlines: Codable, Equatable, Sendable {
    public let offersCloseAt: String?
    public let selectionDeadlineAt: String?

    enum CodingKeys: String, CodingKey {
        case offersCloseAt = "offers_close_at"
        case selectionDeadlineAt = "selection_deadline_at"
    }
}

public struct CustomerOrderAmounts: Codable, Equatable, Sendable {
    public let laborTotal: String?
    public let materialsTotal: String?
    public let finalAmount: String?
    public let paymentMethod: String?
    public let paymentStatus: String?

    enum CodingKeys: String, CodingKey {
        case laborTotal = "labor_total"
        case materialsTotal = "materials_total"
        case finalAmount = "final_amount"
        case paymentMethod = "payment_method"
        case paymentStatus = "payment_status"
    }
}

public struct CustomerOrderReference: Codable, Equatable, Sendable {
    public let id: Int?
    public let name: String?
}

public struct CustomerOrderLocation: Codable, Equatable, Sendable {
    public let area: String?
    public let city: String?
    public let latitude: Double?
    public let longitude: Double?

    enum CodingKeys: String, CodingKey {
        case area, city
        case latitude = "lat"
        case longitude = "lng"
    }

    public init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        area = try values.decodeIfPresent(String.self, forKey: .area)
        city = try values.decodeIfPresent(String.self, forKey: .city)
        latitude = try values.decodeFlexibleDoubleIfPresent(forKey: .latitude)
        longitude = try values.decodeFlexibleDoubleIfPresent(forKey: .longitude)
    }
}

public struct CustomerTrackingPayload: Codable, Equatable, Sendable {
    public let lastLocation: CustomerTrackingLocation?
    public let etaMinutes: Int?
    public let etaApproximate: Bool

    enum CodingKeys: String, CodingKey {
        case lastLocation = "last_location"
        case etaMinutes = "eta_minutes"
        case etaApproximate = "eta_approximate"
    }
}

public struct CustomerTrackingLocation: Codable, Equatable, Sendable {
    public let latitude: Double
    public let longitude: Double

    enum CodingKeys: String, CodingKey {
        case latitude = "lat"
        case longitude = "lng"
    }

    public init(from decoder: Decoder) throws {
        let values = try decoder.container(keyedBy: CodingKeys.self)
        latitude = try values.decodeFlexibleDouble(forKey: .latitude)
        longitude = try values.decodeFlexibleDouble(forKey: .longitude)
    }
}

extension KeyedDecodingContainer {
    fileprivate func decodeFlexibleDoubleIfPresent(forKey key: Key) throws -> Double? {
        if !contains(key) { return nil }
        if try decodeNil(forKey: key) { return nil }
        if let number = try? decode(Double.self, forKey: key) { return number }
        if let text = try? decode(String.self, forKey: key), let number = Double(text) { return number }
        throw DecodingError.dataCorruptedError(
            forKey: key, in: self, debugDescription: "Expected a decimal number or string.")
    }

    fileprivate func decodeFlexibleDouble(forKey key: Key) throws -> Double {
        guard let number = try decodeFlexibleDoubleIfPresent(forKey: key) else {
            throw DecodingError.valueNotFound(
                Double.self,
                DecodingError.Context(
                    codingPath: codingPath + [key], debugDescription: "Expected a decimal value."))
        }
        return number
    }
}

public struct CustomerOrderTiming: Codable, Equatable, Sendable {
    public let type: String?
    public let slotStart: String?

    enum CodingKeys: String, CodingKey {
        case type
        case slotStart = "slot_start"
    }
}

public struct CustomerOrderTermination: Codable, Equatable, Sendable {
    public let reasonCode: String?
    public let reasonLabel: String?
    public let note: String?

    enum CodingKeys: String, CodingKey {
        case note
        case reasonCode = "reason_code"
        case reasonLabel = "reason_label"
    }
}

public struct CustomerProviderSummary: Codable, Equatable, Sendable {
    public let name: String
}

public struct CustomerPayment: Codable, Equatable, Sendable {
    public let id: Int
    public let status: String
    public let amount: String
    public let channel: String?
    public let referenceNumber: String?
    public let checkoutURL: String?
    public let expiresAt: String?

    enum CodingKeys: String, CodingKey {
        case id, status, amount, channel
        case referenceNumber = "reference_number"
        case checkoutURL = "checkout_url"
        case expiresAt = "expires_at"
    }
}

public struct CustomerOrderStep: Codable, Equatable, Sendable {
    public let key: String
    public let state: String
}

public struct CustomerProposal: Codable, Equatable, Sendable {
    public let id: Int
    public let type: String
    public let typeLabel: String
    public let amount: String
    public let projectedTotal: String
    public let reason: String
    public let photoPath: String?
    public let status: String
    public let expiresAt: String?

    enum CodingKeys: String, CodingKey {
        case id, type, amount, reason, status
        case typeLabel = "type_label"
        case projectedTotal = "projected_total"
        case photoPath = "photo_path"
        case expiresAt = "expires_at"
    }
}

public struct CustomerOrderEnvelope: Codable, Sendable {
    public let data: CustomerOrderSummary
}

public struct CustomerOrderListEnvelope: Codable, Sendable {
    public let data: [CustomerOrderSummary]
}

public struct CustomerConfig: Codable, Sendable {
    public let operatingMode: String
    public let offersEnabled: Bool
    public let optionLists: [String: [CustomerOption]]
    public let optionDefaults: [String: String]
    public let serviceHours: CustomerServiceHours

    enum CodingKeys: String, CodingKey {
        case operatingMode = "operating_mode"
        case offersEnabled = "offers_enabled"
        case optionLists = "option_lists"
        case optionDefaults = "option_defaults"
        case serviceHours = "service_hours"
    }
}

public struct CustomerServiceHours: Codable, Equatable, Sendable {
    public let from: String
    public let to: String
}

public struct CustomerOption: Codable, Equatable, Sendable {
    public let code: String
    public let label: String
}

public struct CustomerHome: Equatable, Sendable {
    public let operatingMode: String
    public let orderCount: Int
    public let firstOrderId: Int?
    public let addressId: Int?
    public let categories: [CustomerCategoryReference]
    public let optionLists: [String: [CustomerOption]]
    public let optionDefaults: [String: String]
    public let serviceHours: CustomerServiceHours
    public var customerName = ""
    public var addressLabel = ""
    /// The default address's city: C16 slots and the category check need it without opening C14 (6ب).
    public var addressCityID: Int?
    public var orders: [CustomerOrderSummary] = []
}

public struct CustomerDispute: Codable, Equatable, Sendable {
    public let id: Int
    public let reasonCode: String
    public let reasonLabel: String
    public let description: String
    public let status: String
    public let statusLabel: String
    public let resolution: String?
    public let resolutionLabel: String?
    public let resolutionNote: String?
    public let attachments: [CustomerDisputeAttachment]

    enum CodingKeys: String, CodingKey {
        case id, description, status, resolution, attachments
        case reasonCode = "reason_code"
        case reasonLabel = "reason_label"
        case statusLabel = "status_label"
        case resolutionLabel = "resolution_label"
        case resolutionNote = "resolution_note"
    }
}

public struct CustomerDisputeAttachment: Codable, Equatable, Sendable {
    public let mediaId: Int?
    enum CodingKeys: String, CodingKey { case mediaId = "media_id" }
}

public struct CustomerConversation: Codable, Equatable, Sendable {
    public let id: Int
    public let order: CustomerConversationOrder
    public let provider: CustomerConversationProvider
    public let customer: CustomerConversationCustomer?
    public let status: String
    public let lastMessage: CustomerMessage?

    enum CodingKeys: String, CodingKey {
        case id, order, provider, customer, status
        case lastMessage = "last_message"
    }
}

public struct CustomerConversationCustomer: Codable, Equatable, Sendable {
    public let id: Int
    public let name: String?
    public let ratingAverage: String?

    enum CodingKeys: String, CodingKey {
        case id, name
        case ratingAverage = "rating_avg"
    }
}

public struct CustomerConversationOrder: Codable, Equatable, Sendable {
    public let id: Int
    public let number: String?
    public let status: String?
    public let statusLabel: String?
    public let category: String?
    public let problemType: String?
    public let area: String?

    enum CodingKeys: String, CodingKey {
        case id, number, status, category, area
        case statusLabel = "status_label"
        case problemType = "problem_type"
    }
}

public struct CustomerConversationProvider: Codable, Equatable, Sendable {
    public let id: Int
    public let name: String?
    public let isVerified: Bool

    enum CodingKeys: String, CodingKey {
        case id, name
        case isVerified = "is_verified"
    }
}

public struct CustomerMessage: Codable, Equatable, Sendable {
    public let id: Int
    public let isMine: Bool
    public let body: String?
    public let wasMasked: Bool
    public let media: [CustomerMessageMedia]
    public let createdAt: String?

    enum CodingKeys: String, CodingKey {
        case id, body, media
        case isMine = "is_mine"
        case wasMasked = "was_masked"
        case createdAt = "created_at"
    }
}

public struct CustomerMessageMedia: Codable, Equatable, Sendable {
    public let id: Int
    public let type: String
}

public struct CustomerConversationMessages: Sendable {
    public let conversation: CustomerConversation
    public let messages: [CustomerMessage]
}

public struct CustomerNamedReference: Codable, Equatable, Sendable {
    public let id: Int
    public let name: String
}

public struct CustomerAddress: Codable, Equatable, Sendable {
    public let id: Int
    public let label: String
    public let city: CustomerNamedReference
    public let area: CustomerNamedReference
    public let addressText: String
    public let building: String?
    public let floor: String?
    public let apartment: String?
    public let landmark: String?
    public let lat: String
    public let lng: String
    public let isDefault: Bool

    enum CodingKeys: String, CodingKey {
        case id, label, city, area, building, floor, apartment, landmark, lat, lng
        case addressText = "address_text"
        case isDefault = "is_default"
    }
}

public struct CustomerAddressBody: Encodable, Equatable, Sendable {
    public let label: String
    public let cityId: Int
    public let areaId: Int
    public let addressText: String
    public let building: String?
    public let floor: String?
    public let apartment: String?
    public let landmark: String?
    public let lat: Double
    public let lng: Double
    public let isDefault: Bool

    enum CodingKeys: String, CodingKey {
        case label, building, floor, apartment, landmark, lat, lng
        case cityId = "city_id"
        case areaId = "area_id"
        case addressText = "address_text"
        case isDefault = "is_default"
    }
}

public struct CustomerSlot: Codable, Equatable, Sendable {
    public let start: String
    public let end: String
}

public struct CustomerMedia: Codable, Equatable, Sendable {
    public let id: Int
    public let type: String
    public let sizeBytes: Int
    public let durationSeconds: Int?

    enum CodingKeys: String, CodingKey {
        case id, type
        case sizeBytes = "size_bytes"
        case durationSeconds = "duration_sec"
    }
}

public struct CustomerMediaUpload: Equatable, Sendable {
    public let fileName: String
    public let mimeType: String
    public let data: Data

    public init(fileName: String, mimeType: String, data: Data) {
        self.fileName = fileName
        self.mimeType = mimeType
        self.data = data
    }
}

private struct CustomerListEnvelope<Value: Decodable & Sendable>: Decodable, Sendable {
    let data: [Value]
}

private struct CustomerAddressEnvelope: Decodable, Sendable {
    let data: CustomerAddress
}

private struct CustomerMediaEnvelope: Decodable, Sendable {
    let media: CustomerMedia
}

private struct CustomerPaymentEnvelope: Decodable, Sendable {
    let data: CustomerPayment
}

private struct CustomerReviewEnvelope: Decodable, Sendable {
    let data: CustomerReviewResult
}

private struct CustomerReviewResult: Decodable, Sendable {
    let id: Int
    let average: Double
}

private struct CustomerConversationListEnvelope: Decodable, Sendable {
    let data: [CustomerConversation]
}

private struct CustomerConversationMessagesEnvelope: Decodable, Sendable {
    let conversation: CustomerConversation
    let data: [CustomerMessage]
}

private struct CustomerMessageEnvelope: Decodable, Sendable {
    let data: CustomerMessage
}

private struct AddressEnvelope: Decodable, Sendable {
    let data: [AddressReference]
}

private struct AddressReference: Decodable, Sendable {
    let id: Int
    let isDefault: Bool
    let label: String?
    let area: CustomerNamedReference?
    let city: CustomerNamedReference?

    enum CodingKeys: String, CodingKey {
        case id, label, area, city
        case isDefault = "is_default"
    }

    /// C04 address row, as `customerAddressLabel` builds it from a full address.
    var displayLabel: String {
        [label ?? "", area?.name ?? ""].filter { !$0.isEmpty }.joined(separator: " · ")
    }
}

private struct CatalogEnvelope: Decodable, Sendable {
    let data: [CustomerCategoryReference]
}

public struct CustomerCategoryReference: Codable, Equatable, Sendable {
    public let id: Int
    public let name: String
    public let iconPath: String?
    public let problemTypes: [CustomerProblemTypeReference]

    enum CodingKeys: String, CodingKey {
        case id, name
        case iconPath = "icon_path"
        case problemTypes = "problem_types"
    }
}

extension CustomerCategoryReference {
    public var iconKey: String? {
        guard let fileName = iconPath?.split(separator: "/").last else { return nil }
        return fileName.split(separator: ".").first.map(String.init)
    }
}

public struct CustomerProblemTypeReference: Codable, Equatable, Sendable {
    public let id: Int
    public let name: String
    public let isOther: Bool

    enum CodingKeys: String, CodingKey {
        case id, name
        case isOther = "is_other"
    }
}

public struct CustomerCatalogChoice: Equatable, Sendable {
    public let categoryId: Int
    public let problemTypeId: Int?
}

/// Keeps catalog identity selection outside SwiftUI so C01/C03 never fall back to list position zero.
public enum CustomerCatalogSelection {
    public static func category(
        categoryIds: [Int],
        problemIdsByCategory: [[Int]],
        selectedIndex: Int,
        currentProblemId: Int?
    ) -> CustomerCatalogChoice? {
        guard categoryIds.indices.contains(selectedIndex) else { return nil }
        let problemIds =
            problemIdsByCategory.indices.contains(selectedIndex)
            ? problemIdsByCategory[selectedIndex] : []
        return CustomerCatalogChoice(
            categoryId: categoryIds[selectedIndex],
            problemTypeId: currentProblemId.flatMap { problemIds.contains($0) ? $0 : nil })
    }

    public static func problem(
        categoryId: Int?, problemIds: [Int], selectedIndex: Int
    ) -> CustomerCatalogChoice? {
        guard let categoryId, problemIds.indices.contains(selectedIndex) else { return nil }
        return CustomerCatalogChoice(categoryId: categoryId, problemTypeId: problemIds[selectedIndex])
    }
}

public struct ProviderContext: Codable, Equatable, Sendable {
    public let id: Int
    public let name: String
    public let bio: String?
    public let isVerified: Bool
    public let availableNow: Bool
    public let experienceYears: Int?
    public let ratingAverage: String?
    public let completedOrders: Int?
    public let ratingBreakdown: ProviderRatingBreakdown
    public let specialties: [CustomerNamedReference]
    public let reviews: [CustomerProviderReview]
    public let availableActions: [String]

    enum CodingKeys: String, CodingKey {
        case id, name, bio, specialties, reviews
        case isVerified = "is_verified"
        case availableNow = "available_now"
        case experienceYears = "experience_years"
        case ratingAverage = "rating_avg"
        case completedOrders = "completed_orders"
        case ratingBreakdown = "rating_breakdown"
        case availableActions = "available_actions"
    }
}

public struct ProviderRatingBreakdown: Codable, Equatable, Sendable {
    public let quality: String?
    public let punctuality: String?
    public let conduct: String?
}

public struct CustomerProviderReview: Codable, Equatable, Sendable {
    public let customerName: String
    public let quality: Int
    public let punctuality: Int
    public let conduct: Int
    public let comment: String?
    public let createdAt: String?

    enum CodingKeys: String, CodingKey {
        case quality, punctuality, conduct, comment
        case customerName = "customer_name"
        case createdAt = "created_at"
    }
}

public struct CustomerOffer: Codable, Equatable, Sendable {
    public let id: Int
    public let price: String
    public let etaMinutes: Int?
    public let inspectionFeeDeductible: Bool
    public let includesText: String?
    public let note: String?
    public let provider: CustomerOfferProvider

    enum CodingKeys: String, CodingKey {
        case id, price, provider, note
        case etaMinutes = "eta_minutes"
        case inspectionFeeDeductible = "inspection_fee_deductible"
        case includesText = "includes_text"
    }
}

public struct CustomerOfferProvider: Codable, Equatable, Sendable {
    public let id: Int
    public let name: String
    public let isVerified: Bool
    public let ratingAverage: String?
    public let completedOrders: Int?

    enum CodingKeys: String, CodingKey {
        case id, name
        case isVerified = "is_verified"
        case ratingAverage = "rating_avg"
        case completedOrders = "completed_orders"
    }
}

private struct CustomerOfferEnvelope: Decodable, Sendable { let data: [CustomerOffer] }

public struct ProviderEnvelope: Codable, Sendable {
    public let data: ProviderContext
}

public struct PublishRequestBody: Encodable, Equatable, Sendable {
    public let customerAddressId: Int
    public let categoryId: Int
    public let problemTypeId: Int
    public let timingType: String
    public let materialsResponsibility: String
    public let termsAccepted: Bool
    public let description: String?
    public let mediaIds: [Int]
    public let slotStart: String?
    public let pricingMode: String?
    public let budgetAmount: String?

    public init(
        customerAddressId: Int, categoryId: Int, problemTypeId: Int, timingType: String,
        materialsResponsibility: String, termsAccepted: Bool, description: String?,
        mediaIds: [Int] = [], slotStart: String? = nil, pricingMode: String? = nil,
        budgetAmount: String? = nil
    ) {
        self.customerAddressId = customerAddressId
        self.categoryId = categoryId
        self.problemTypeId = problemTypeId
        self.timingType = timingType
        self.materialsResponsibility = materialsResponsibility
        self.termsAccepted = termsAccepted
        self.description = description
        self.mediaIds = mediaIds
        self.slotStart = slotStart
        self.pricingMode = pricingMode
        self.budgetAmount = budgetAmount
    }

    enum CodingKeys: String, CodingKey {
        case customerAddressId = "customer_address_id"
        case categoryId = "category_id"
        case problemTypeId = "problem_type_id"
        case timingType = "timing_type"
        case materialsResponsibility = "materials_responsibility"
        case termsAccepted = "terms_accepted"
        case description
        case mediaIds = "media_ids"
        case slotStart = "slot_start"
        case pricingMode = "pricing_mode"
        case budgetAmount = "budget_amount"
    }
}

public protocol CustomerAPI: Sendable {
    func home() async throws -> CustomerHome
    func catalogCategoryIDs(cityID: Int) async throws -> Set<Int>
    func order(id: Int) async throws -> CustomerOrderSummary
    func tracking(orderId: Int) async throws -> CustomerTrackingPayload
    func orders(scope: String, page: Int) async throws -> [CustomerOrderSummary]
    func conversations(page: Int) async throws -> [CustomerConversation]
    func offers(orderId: Int, sort: String) async throws -> [CustomerOffer]
    func conversationMessages(id: Int, page: Int) async throws -> CustomerConversationMessages
    func sendMessage(conversationId: Int, body: String?, mediaIds: [Int]) async throws
        -> CustomerMessage
    func provider(id: Int, orderId: Int) async throws -> ProviderContext
    func publish(_ body: PublishRequestBody, idempotencyKey: String) async throws
        -> CustomerOrderSummary
    func updateOrder(id: Int, body: PublishRequestBody) async throws -> CustomerOrderSummary
    func acceptOffer(orderId: Int, offerId: Int, expectedVersion: Int, paymentMethod: String)
        async throws -> CustomerOrderSummary
    func cancelOrder(orderId: Int, reasonCode: String, note: String?, expectedVersion: Int)
        async throws -> CustomerOrderSummary
    func proposals(orderId: Int) async throws -> [CustomerProposal]
    func decideProposal(orderId: Int, proposalId: Int, approve: Bool, expectedVersion: Int)
        async throws -> CustomerOrderSummary
    func changePaymentMethod(orderId: Int, method: String, expectedVersion: Int) async throws
        -> CustomerOrderSummary
    func createPayment(orderId: Int, channel: String, expectedVersion: Int) async throws
        -> CustomerPayment
    func confirmCompletion(orderId: Int, expectedVersion: Int) async throws -> CustomerOrderSummary
    func submitReview(orderId: Int, quality: Int, punctuality: Int, conduct: Int, comment: String?)
        async throws
    func addresses() async throws -> [CustomerAddress]
    func cities() async throws -> [CustomerNamedReference]
    func areas(cityId: Int) async throws -> [CustomerNamedReference]
    func saveAddress(id: Int?, body: CustomerAddressBody) async throws -> CustomerAddress
    func deleteAddress(id: Int) async throws
    func slots(cityId: Int, date: String) async throws -> [CustomerSlot]
    func uploadMedia(_ upload: CustomerMediaUpload) async throws -> CustomerMedia
    func deleteMedia(id: Int) async throws
    func disputes(orderId: Int) async throws -> [CustomerDispute]
    func openDispute(orderId: Int, reasonCode: String, description: String, mediaIds: [Int])
        async throws -> CustomerDispute
    func reportProvider(providerId: Int, orderId: Int, reasonCode: String, description: String?)
        async throws
    func faqs() async throws -> [CustomerFAQ]
    func sendSupportMessage(subject: String, message: String) async throws
    func notifications() async throws -> [CustomerNotification]
    func markNotificationsRead(ids: [String]) async throws
    func registerDevice(token: String) async throws
    func unregisterDevice(token: String) async throws
    func logout() async throws
    func updateRatingReminders(enabled: Bool) async throws -> CustomerAccount
    func account() async throws -> CustomerAccount
    func updateAccount(name: String, phone: String) async throws -> CustomerAccount
    func changePassword(current: String, password: String, confirmation: String) async throws
    func deleteAccount() async throws
    func terms() async throws -> CustomerTerms
    func republish(orderId: Int) async throws -> CustomerOrderSummary
}

extension CustomerAPI {
    public func tracking(orderId _: Int) async throws -> CustomerTrackingPayload {
        throw URLError(.unsupportedURL)
    }
}

public struct LiveCustomerAPI: CustomerAPI {
    private let client: APIClient

    public init(
        baseURL: URL, token: String, appMode: String,
        transport: any HTTPTransport = URLSessionTransport()
    ) {
        client = APIClient(baseURL: baseURL, token: token, appMode: appMode, transport: transport)
    }

    public func catalogCategoryIDs(cityID: Int) async throws -> Set<Int> {
        let catalog: CatalogEnvelope = try await client.get("catalog?city_id=\(cityID)")
        return Set(catalog.data.map(\.id))
    }

    public func home() async throws -> CustomerHome {
        let config: CustomerConfig = try await client.get("config")
        let orders: CustomerOrderListEnvelope = try await client.get("orders?scope=current")
        let addresses: AddressEnvelope = try await client.get("addresses")
        let address = addresses.data.first(where: \.isDefault) ?? addresses.data.first
        let catalog: CatalogEnvelope = try await client.get(
            address?.city.map { "catalog?city_id=\($0.id)" } ?? "catalog")
        let user: CustomerAccountEnvelope = try await client.get("me")
        var home = CustomerHome(
            // The app vocabulary is marketplace/staff; the server sends MARKETPLACE/EMPLOYEE (39).
            operatingMode: config.operatingMode == "MARKETPLACE" ? "marketplace" : "staff",
            orderCount: orders.data.count,
            firstOrderId: orders.data.first?.id, addressId: address?.id, categories: catalog.data,
            optionLists: config.optionLists, optionDefaults: config.optionDefaults,
            serviceHours: config.serviceHours)
        home.customerName = user.user.name
        home.addressLabel = address?.displayLabel ?? ""
        home.addressCityID = address?.city?.id
        home.orders = orders.data
        return home
    }

    public func order(id: Int) async throws -> CustomerOrderSummary {
        let response: CustomerOrderEnvelope = try await client.get("orders/\(id)")
        return response.data
    }

    public func tracking(orderId: Int) async throws -> CustomerTrackingPayload {
        try await client.get("orders/\(orderId)/tracking")
    }

    public func orders(scope: String, page: Int = 1) async throws -> [CustomerOrderSummary] {
        let response: CustomerOrderListEnvelope = try await client.get(
            "orders?scope=\(scope)&page=\(page)")
        return response.data
    }

    public func conversations(page: Int = 1) async throws -> [CustomerConversation] {
        let response: CustomerConversationListEnvelope = try await client.get(
            "conversations?page=\(page)")
        return response.data
    }

    public func offers(orderId: Int, sort: String = "rating") async throws -> [CustomerOffer] {
        let response: CustomerOfferEnvelope = try await client.get(
            "orders/\(orderId)/offers?sort=\(sort)")
        return response.data
    }

    public func conversationMessages(id: Int, page: Int = 1) async throws
        -> CustomerConversationMessages
    {
        let response: CustomerConversationMessagesEnvelope = try await client.get(
            "conversations/\(id)/messages?page=\(page)")
        return CustomerConversationMessages(
            conversation: response.conversation, messages: response.data)
    }

    public func sendMessage(conversationId: Int, body: String?, mediaIds: [Int] = []) async throws
        -> CustomerMessage
    {
        let response: CustomerMessageEnvelope = try await client.post(
            "conversations/\(conversationId)/messages",
            body: SendCustomerMessageBody(body: body, mediaIds: mediaIds))
        return response.data
    }

    public func provider(id: Int, orderId: Int) async throws -> ProviderContext {
        let response: ProviderEnvelope = try await client.get("providers/\(id)?order_id=\(orderId)")
        return response.data
    }

    public func publish(_ body: PublishRequestBody, idempotencyKey: String) async throws
        -> CustomerOrderSummary
    {
        let response: CustomerOrderEnvelope = try await client.post(
            "orders", body: body, idempotencyKey: idempotencyKey)
        return response.data
    }

    public func updateOrder(id: Int, body: PublishRequestBody) async throws -> CustomerOrderSummary {
        let response: CustomerOrderEnvelope = try await client.patch("orders/\(id)", body: body)
        return response.data
    }

    public func acceptOffer(orderId: Int, offerId: Int, expectedVersion: Int, paymentMethod: String)
        async throws -> CustomerOrderSummary
    {
        let body = AcceptOfferBody(expectedVersion: expectedVersion, paymentMethod: paymentMethod)
        let response: CustomerOrderEnvelope = try await client.post(
            "orders/\(orderId)/offers/\(offerId)/accept", body: body)
        return response.data
    }

    public func cancelOrder(orderId: Int, reasonCode: String, note: String?, expectedVersion: Int)
        async throws -> CustomerOrderSummary
    {
        let response: CustomerOrderEnvelope = try await client.post(
            "orders/\(orderId)/cancel",
            body: CancelOrderBody(reasonCode: reasonCode, note: note, expectedVersion: expectedVersion))
        return response.data
    }

    public func proposals(orderId: Int) async throws -> [CustomerProposal] {
        let response: CustomerListEnvelope<CustomerProposal> = try await client.get(
            "orders/\(orderId)/proposals")
        return response.data
    }

    public func decideProposal(orderId: Int, proposalId: Int, approve: Bool, expectedVersion: Int)
        async throws -> CustomerOrderSummary
    {
        let response: CustomerOrderEnvelope = try await client.post(
            "orders/\(orderId)/proposals/\(proposalId)/decide",
            body: ProposalDecisionBody(approve: approve, expectedVersion: expectedVersion))
        return response.data
    }

    public func changePaymentMethod(orderId: Int, method: String, expectedVersion: Int) async throws
        -> CustomerOrderSummary
    {
        let response: CustomerOrderEnvelope = try await client.patch(
            "orders/\(orderId)/payment-method",
            body: ChangePaymentMethodBody(paymentMethod: method, expectedVersion: expectedVersion))
        return response.data
    }

    public func createPayment(orderId: Int, channel: String, expectedVersion: Int) async throws
        -> CustomerPayment
    {
        let response: CustomerPaymentEnvelope = try await client.post(
            "orders/\(orderId)/payments",
            body: CreatePaymentBody(channel: channel, expectedVersion: expectedVersion))
        return response.data
    }

    public func confirmCompletion(orderId: Int, expectedVersion: Int) async throws
        -> CustomerOrderSummary
    {
        let response: CustomerOrderEnvelope = try await client.post(
            "orders/\(orderId)/confirm-completion",
            body: ExpectedVersionBody(expectedVersion: expectedVersion))
        return response.data
    }

    public func submitReview(
        orderId: Int, quality: Int, punctuality: Int, conduct: Int, comment: String?
    ) async throws {
        let _: CustomerReviewEnvelope = try await client.post(
            "orders/\(orderId)/review",
            body: SubmitReviewBody(
                quality: quality, punctuality: punctuality, conduct: conduct, comment: comment))
    }

    public func addresses() async throws -> [CustomerAddress] {
        let response: CustomerListEnvelope<CustomerAddress> = try await client.get("addresses")
        return response.data
    }

    public func cities() async throws -> [CustomerNamedReference] {
        let response: CustomerListEnvelope<CustomerNamedReference> = try await client.get("cities")
        return response.data
    }

    public func areas(cityId: Int) async throws -> [CustomerNamedReference] {
        let response: CustomerListEnvelope<CustomerNamedReference> = try await client.get(
            "cities/\(cityId)/areas")
        return response.data
    }

    public func saveAddress(id: Int?, body: CustomerAddressBody) async throws -> CustomerAddress {
        let response: CustomerAddressEnvelope
        if let id {
            response = try await client.patch("addresses/\(id)", body: body)
        } else {
            response = try await client.post("addresses", body: body)
        }
        return response.data
    }

    public func deleteAddress(id: Int) async throws {
        try await client.delete("addresses/\(id)")
    }

    public func slots(cityId: Int, date: String) async throws -> [CustomerSlot] {
        let response: CustomerListEnvelope<CustomerSlot> = try await client.get(
            "slots?city_id=\(cityId)&date=\(date)")
        return response.data
    }

    public func uploadMedia(_ upload: CustomerMediaUpload) async throws -> CustomerMedia {
        let response: CustomerMediaEnvelope = try await client.multipart(
            "media", field: "file", fileName: upload.fileName, mimeType: upload.mimeType,
            data: upload.data)
        return response.media
    }

    public func deleteMedia(id: Int) async throws {
        try await client.delete("media/\(id)")
    }

    public func disputes(orderId: Int) async throws -> [CustomerDispute] {
        let response: CustomerListEnvelope<CustomerDispute> = try await client.get(
            "orders/\(orderId)/disputes")
        return response.data
    }

    public func openDispute(
        orderId: Int, reasonCode: String, description: String, mediaIds: [Int]
    ) async throws -> CustomerDispute {
        let response: CustomerDisputeEnvelope = try await client.post(
            "orders/\(orderId)/disputes",
            body: OpenDisputeBody(reasonCode: reasonCode, description: description, mediaIds: mediaIds))
        return response.data
    }

    public func reportProvider(
        providerId: Int, orderId: Int, reasonCode: String, description: String?
    ) async throws {
        let _: CustomerReportEnvelope = try await client.post(
            "providers/\(providerId)/reports",
            body: ProviderReportBody(orderId: orderId, reasonCode: reasonCode, description: description))
    }

    public func faqs() async throws -> [CustomerFAQ] {
        let response: CustomerListEnvelope<CustomerFAQ> = try await client.get("support/faqs")
        return response.data
    }

    public func sendSupportMessage(subject: String, message: String) async throws {
        try await client.postNoContent(
            "support/messages", body: SupportMessageBody(subject: subject, message: message))
    }

    public func notifications() async throws -> [CustomerNotification] {
        let response: CustomerListEnvelope<CustomerNotification> = try await client.get("notifications")
        return response.data
    }

    public func markNotificationsRead(ids: [String]) async throws {
        try await client.postNoContent(
            "notifications/read", body: NotificationReadBody(notificationIds: ids))
    }

    // DEC-058 — رمز FCM للجهاز؛ Push يصل لكل رموز الحساب أيًّا كان الوضع.
    public func registerDevice(token: String) async throws {
        try await client.postNoContent("me/devices", body: DeviceBody(token: token, platform: "IOS"))
    }

    public func unregisterDevice(token: String) async throws {
        try await client.delete("me/devices", body: DeviceBody(token: token, platform: nil))
    }

    public func logout() async throws { try await client.postNoContent("auth/logout") }

    public func updateRatingReminders(enabled: Bool) async throws -> CustomerAccount {
        let response: CustomerAccountEnvelope = try await client.patch(
            "me", body: RatingRemindersBody(ratingRemindersEnabled: enabled))
        return response.user
    }

    public func account() async throws -> CustomerAccount {
        let response: CustomerAccountEnvelope = try await client.get("me")
        return response.user
    }

    public func updateAccount(name: String, phone: String) async throws -> CustomerAccount {
        let response: CustomerAccountEnvelope = try await client.patch(
            "me", body: AccountUpdateBody(name: name, phone: phone))
        return response.user
    }

    public func changePassword(current: String, password: String, confirmation: String) async throws {
        try await client.postNoContent(
            "me/password",
            body: PasswordChangeBody(
                currentPassword: current, password: password, passwordConfirmation: confirmation))
    }

    public func deleteAccount() async throws { try await client.delete("me") }

    public func terms() async throws -> CustomerTerms {
        let response: CustomerTermsEnvelope = try await client.get("terms/current")
        return response.data
    }

    public func republish(orderId: Int) async throws -> CustomerOrderSummary {
        let response: CustomerOrderEnvelope = try await client.postWithoutBody(
            "orders/\(orderId)/republish")
        return response.data
    }
}

public struct CustomerFAQ: Codable, Sendable {
    public let code: String
    public let title: String
    public let body: String
}

public struct CustomerNotification: Codable, Sendable {
    public let id: String
    public let title: String
    public let body: String
    public let deepLink: String?
    public let readAt: String?
    public let createdAt: String?

    enum CodingKeys: String, CodingKey {
        case id, title, body
        case deepLink = "deep_link"
        case readAt = "read_at"
        case createdAt = "created_at"
    }
}

public struct CustomerAccount: Codable, Sendable {
    public let name: String
    public let email: String
    public let phone: String
    public let availableActions: [String]
    public let ratingRemindersEnabled: Bool?
    public let isVerified: Bool?

    enum CodingKeys: String, CodingKey {
        case name, email, phone
        case availableActions = "available_actions"
        case ratingRemindersEnabled = "rating_reminders_enabled"
        case isVerified = "is_verified"
    }
}

private struct DeviceBody: Encodable, Sendable {
    let token: String
    let platform: String?
}

private struct RatingRemindersBody: Encodable, Sendable {
    let ratingRemindersEnabled: Bool
    enum CodingKeys: String, CodingKey { case ratingRemindersEnabled = "rating_reminders_enabled" }
}

public struct CustomerTerms: Codable, Sendable {
    public let version: Int
    public let body: String
    public let effectiveAt: String

    enum CodingKeys: String, CodingKey {
        case version, body
        case effectiveAt = "effective_at"
    }
}

private struct CustomerAccountEnvelope: Decodable, Sendable { let user: CustomerAccount }
private struct CustomerTermsEnvelope: Decodable, Sendable { let data: CustomerTerms }
private struct SupportMessageBody: Encodable, Sendable {
    let subject: String
    let message: String
}
private struct NotificationReadBody: Encodable, Sendable {
    let notificationIds: [String]
    enum CodingKeys: String, CodingKey { case notificationIds = "notification_ids" }
}
private struct AccountUpdateBody: Encodable, Sendable {
    let name: String
    let phone: String
}
private struct PasswordChangeBody: Encodable, Sendable {
    let currentPassword: String
    let password: String
    let passwordConfirmation: String
    enum CodingKeys: String, CodingKey {
        case currentPassword = "current_password"
        case password
        case passwordConfirmation = "password_confirmation"
    }
}

private struct CustomerDisputeEnvelope: Decodable, Sendable { let data: CustomerDispute }
private struct CustomerReportEnvelope: Decodable, Sendable { let data: CustomerReportResult }
private struct CustomerReportResult: Decodable, Sendable {
    let id: Int
    let status: String
}

private struct OpenDisputeBody: Encodable, Sendable {
    let reasonCode: String
    let description: String
    let mediaIds: [Int]
    enum CodingKeys: String, CodingKey {
        case reasonCode = "reason_code"
        case description
        case mediaIds = "media_ids"
    }
}

private struct ProviderReportBody: Encodable, Sendable {
    let orderId: Int
    let reasonCode: String
    let description: String?
    enum CodingKeys: String, CodingKey {
        case orderId = "order_id"
        case reasonCode = "reason_code"
        case description
    }
}

private struct AcceptOfferBody: Encodable, Sendable {
    let expectedVersion: Int
    let paymentMethod: String

    enum CodingKeys: String, CodingKey {
        case expectedVersion = "expected_version"
        case paymentMethod = "payment_method"
    }
}

private struct CancelOrderBody: Encodable, Sendable {
    let reasonCode: String
    let note: String?
    let expectedVersion: Int
    enum CodingKeys: String, CodingKey {
        case reasonCode = "reason_code"
        case note
        case expectedVersion = "expected_version"
    }
}

private struct ProposalDecisionBody: Encodable, Sendable {
    let approve: Bool
    let expectedVersion: Int
    enum CodingKeys: String, CodingKey {
        case approve
        case expectedVersion = "expected_version"
    }
}

private struct ChangePaymentMethodBody: Encodable, Sendable {
    let paymentMethod: String
    let expectedVersion: Int
    enum CodingKeys: String, CodingKey {
        case paymentMethod = "payment_method"
        case expectedVersion = "expected_version"
    }
}

private struct CreatePaymentBody: Encodable, Sendable {
    let channel: String
    let expectedVersion: Int
    enum CodingKeys: String, CodingKey {
        case channel
        case expectedVersion = "expected_version"
    }
}

private struct ExpectedVersionBody: Encodable, Sendable {
    let expectedVersion: Int
    enum CodingKeys: String, CodingKey { case expectedVersion = "expected_version" }
}

private struct SubmitReviewBody: Encodable, Sendable {
    let quality: Int
    let punctuality: Int
    let conduct: Int
    let comment: String?
}

private struct SendCustomerMessageBody: Encodable, Sendable {
    let body: String?
    let mediaIds: [Int]

    enum CodingKeys: String, CodingKey {
        case body
        case mediaIds = "media_ids"
    }
}

@MainActor
@Observable
// swiftlint:disable:next type_body_length
public final class CustomerViewModel {
    public private(set) var state = CustomerUIState(screen: "SCR-C01", phase: .loading) {
        // Loading is transient; an error screen is shown but not returned to.
        didSet { if state.phase != .loading { history.show(state, restorable: state.phase != .error) } }
    }
    /// The back stack for every customer screen's back (6ب, `design/fixtures/navigation/history.json`).
    @ObservationIgnored private var history = ScreenHistory<CustomerUIState>(root: "SCR-C01") { $0.screen }
    @ObservationIgnored private var categoryAvailable = true

    /// Back: the recorded screen as it was shown, home when nothing is recorded; false on home.
    @discardableResult
    public func goBack() async -> Bool {
        guard let target = history.back(rootEntry: { CustomerUIState(screen: "SCR-C01", phase: .loading) })
        else { return false }
        switch target.screen {
        case "SCR-C01": await loadHome()
        case "SCR-C19":
            if let id = currentConversationId {
                await loadConversation(id: id, showLoading: true)
            } else {
                await loadConversations()
            }
        default:
            stopChatPolling()
            state = target
        }
        return true
    }
    public private(set) var accountDeleted = false
    public private(set) var trackingPayload: CustomerTrackingPayload?
    private let api: any CustomerAPI
    private let currencyLabel: String
    private var currentOrderId: Int?
    private var currentOrderVersion = 0
    private var currentProposalId: Int?
    private var currentOrder: CustomerOrderSummary?
    private var selectedPaymentChannel = -1
    private var ratingValues = [0, 0, 0]
    private var ratingComment = ""
    private var cancellationReasonIndex = -1
    private var cancellationNote = ""
    private var optionLists: [String: [CustomerOption]] = [:]
    private var optionDefaults: [String: String] = [:]
    private var currentProviderId: Int?
    private var currentProviderActions: [String] = []
    private var supportReasonIndex = -1
    private var supportDescription = ""
    private var disputeMedia: [CustomerMediaDraft] = []
    private var addressId: Int?
    private var categoryId: Int?
    private var problemTypeId: Int?
    private var cityId: Int?
    private var addressIDs: [Int] = []
    private var addressPayloads: [CustomerAddress] = []
    private var addressSelectionMode = true
    private var areaIDs: [Int] = []
    private var dayDates: [String] = []
    private var slotStarts: [String] = []
    private var selectedSlotIndex = -1
    private var timingType = ""
    private var slotStart: String?
    private var addressDraft = CustomerAddressDraft()
    private var mediaDrafts: [CustomerMediaDraft] = []
    private var mediaRecording = false
    private var chatPollingTask: Task<Void, Never>?
    private var conversationIDs: [Int] = []
    private var currentConversationId: Int?
    private var currentConversation: CustomerConversation?
    private var chatMessages: [CustomerMessage] = []
    private var chatDraft = ""
    private var orderIDs: [Int] = []
    private var orderStatuses: [String] = []
    private var faqQuestions: [String] = []
    private var faqAnswers: [String] = []
    private var expandedFaqIndex = -1
    private var helpSubject = ""
    private var helpMessage = ""
    private var notificationIDs: [String] = []
    private var notificationLinks: [String] = []
    private var accountValues = ["", "", ""]
    private var accountActions: [String] = []
    private var accountMode = "overview"
    private var editingOrderId: Int?
    private var ratingRemindersEnabled = true
    /// From `GET /config` through home: marketplace or staff (39). Never assumed.
    private var operatingMode = "staff"
    private var addressLabel = ""
    private var catalog: [CustomerCategoryReference] = []
    private var requestDescription = ""
    private var currentOffers: [CustomerOffer] = []
    private var currentOfferId: Int?
    private var offersSortIndex = 0
    private var selectedMaterialIndex = -1
    private var selectedPricingIndex = -1
    private var requestBudget = ""
    private var serviceHours = CustomerServiceHours(from: "00:00", to: "23:59")
    private var selectedSlotLabel = ""

    /// DEC-058 — set by the app: whether the system lets notifications show (C32 banner).
    public var notificationsAllowed: () -> Bool = { true }
    /// C32 rows open through the app root, which knows the provider status and can switch modes.
    public var onDeepLink: (String) -> Void = { _ in }

    public init(api: any CustomerAPI, currencyLabel: String) {
        self.api = api
        self.currencyLabel = currencyLabel
    }

    public func show(screen: String, input: [String: CustomerInputValue]) {
        if screen != "SCR-C19" { stopChatPolling() }
        state = CustomerLogic.reduce(screen: screen, input: input)
    }

    public func loadHelp() async {
        state = CustomerLogic.reduce(screen: "SCR-C29", input: ["event": .text("loading")])
        do {
            let faqs = try await api.faqs()
            faqQuestions = faqs.map(\.title)
            faqAnswers = faqs.map(\.body)
            state = CustomerLogic.reduce(screen: "SCR-C29", input: helpInput(event: "loaded"))
        } catch { state = CustomerLogic.reduce(screen: "SCR-C29", input: ["event": .text("error")]) }
    }

    public func toggleFAQ(index: Int) {
        expandedFaqIndex = expandedFaqIndex == index ? -1 : index
        state = CustomerLogic.reduce(screen: "SCR-C29", input: helpInput(event: "loaded"))
    }

    public func updateHelpField(index: Int, value: String) {
        if index == 0 {
            helpSubject = String(value.prefix(120))
        } else {
            helpMessage = String(value.prefix(2_000))
        }
        state = CustomerLogic.reduce(screen: "SCR-C29", input: helpInput(event: "loaded"))
    }

    public func submitHelpMessage() async {
        guard state.canContinue else { return }
        state = CustomerLogic.reduce(screen: "SCR-C29", input: helpInput(event: "submitting"))
        do {
            try await api.sendSupportMessage(
                subject: helpSubject.trimmingCharacters(in: .whitespacesAndNewlines),
                message: helpMessage.trimmingCharacters(in: .whitespacesAndNewlines))
            state = CustomerLogic.reduce(screen: "SCR-C29", input: helpInput(event: "success"))
        } catch { state = CustomerLogic.reduce(screen: "SCR-C29", input: ["event": .text("error")]) }
    }

    public func loadNotifications() async {
        state = CustomerLogic.reduce(screen: "SCR-C32", input: ["event": .text("loading")])
        do {
            let values = try await api.notifications()
            notificationIDs = values.map(\.id)
            notificationLinks = values.map { $0.deepLink ?? "" }
            state = CustomerLogic.reduce(
                screen: "SCR-C32", input: notificationInput(values, event: "loaded"))
        } catch { state = CustomerLogic.reduce(screen: "SCR-C32", input: ["event": .text("error")]) }
    }

    public func openNotification(index: Int) async {
        guard notificationIDs.indices.contains(index) else { return }
        state.isBusy = true
        try? await api.markNotificationsRead(ids: [notificationIDs[index]])
        let link = notificationLinks[safe: index] ?? ""
        if link.isEmpty { await loadNotifications() } else { onDeepLink(link) }
    }

    /// DEC-058 — after sign-in and whenever Firebase rotates the token. Failures retry on the next launch.
    public func registerPushDevice(token: String) async {
        try? await api.registerDevice(token: token)
    }

    /// AC-NTF-07 — the device token goes first, then the Sanctum token.
    public func signOut(deviceToken: String?) async {
        if let deviceToken { try? await api.unregisterDevice(token: deviceToken) }
        try? await api.logout()
    }

    /// 17 §الروابط العميقة — customer targets only; the app root routes provider ones.
    public func openDeepLink(_ target: DeepLinkTarget) async {
        switch target.target {
        case "ORDER": await loadOrder(id: target.orderID)
        case "SCR-C06": await loadOffers(orderId: target.orderID)
        case "SCR-C19": await openChat(orderID: target.orderID)
        case "SCR-C24":
            currentOrderId = target.orderID
            openRating()
        case "SCR-C27":
            currentOrderId = target.orderID
            await openDispute()
        default: await loadNotifications()
        }
    }

    private func openChat(orderID: Int) async {
        guard let conversation = try? await api.conversations(page: 1).first(where: { $0.order.id == orderID })
        else {
            await loadOrder(id: orderID)
            return
        }
        currentConversationId = conversation.id
        chatDraft = ""
        await loadConversation(id: conversation.id, showLoading: true)
    }

    /// NTF-18 only; saved at once, and reverted when the server refuses (C33).
    public func setRatingReminders(_ enabled: Bool) async {
        let previous = ratingRemindersEnabled
        ratingRemindersEnabled = enabled
        state = CustomerLogic.reduce(screen: "SCR-C33", input: accountInput(event: "loaded"))
        do {
            let account = try await api.updateRatingReminders(enabled: enabled)
            ratingRemindersEnabled = account.ratingRemindersEnabled ?? enabled
        } catch {
            ratingRemindersEnabled = previous
        }
        if state.screen == "SCR-C33" {
            state = CustomerLogic.reduce(screen: "SCR-C33", input: accountInput(event: "loaded"))
        }
    }

    public func loadAccount() async {
        state = CustomerLogic.reduce(screen: "SCR-C33", input: ["event": .text("loading")])
        do {
            let account = try await api.account()
            accountValues = [account.name, account.email, account.phone]
            accountActions = account.availableActions
            ratingRemindersEnabled = account.ratingRemindersEnabled ?? true
            accountMode = "overview"
            state = CustomerLogic.reduce(screen: "SCR-C33", input: accountInput(event: "loaded"))
        } catch { state = CustomerLogic.reduce(screen: "SCR-C33", input: ["event": .text("error")]) }
    }

    public func accountAction(_ action: String) async {
        switch action {
        case "update_account" where accountMode == "edit": await submitAccountUpdate()
        case "update_account":
            accountMode = "edit"
            state = CustomerLogic.reduce(screen: "SCR-C33", input: accountInput(event: "loaded"))
        case "change_password" where accountMode == "password": await submitPasswordChange()
        case "change_password":
            accountMode = "password"
            accountValues = ["", "", ""]
            state = CustomerLogic.reduce(screen: "SCR-C33", input: accountInput(event: "loaded"))
        case "delete_account":
            state = CustomerLogic.reduce(
                screen: "SCR-C33", input: accountInput(event: "confirm_delete", mode: "delete"))
        case "dismiss_delete":
            accountMode = "overview"
            state = CustomerLogic.reduce(screen: "SCR-C33", input: accountInput(event: "loaded"))
        case "confirm_delete_account": await submitDeleteAccount()
        case "open_terms": await loadTerms()
        default: break
        }
    }

    public func updateAccountField(index: Int, value: String) {
        guard accountValues.indices.contains(index) else { return }
        accountValues[index] = value
        state = CustomerLogic.reduce(screen: "SCR-C33", input: accountInput(event: "loaded"))
    }

    public func loadTerms() async {
        state = CustomerLogic.reduce(screen: "SCR-C34", input: ["event": .text("loading")])
        do {
            let terms = try await api.terms()
            state = CustomerLogic.reduce(
                screen: "SCR-C34",
                input: [
                    "event": .text("loaded"), "body": .text(terms.body),
                    "version": .text(String(terms.version)),
                    "effective_at": .text(displayDateTime(terms.effectiveAt)),
                ])
        } catch { state = CustomerLogic.reduce(screen: "SCR-C34", input: ["event": .text("error")]) }
    }

    public func noOffersAction(_ action: String) async {
        if action == "edit_request" {
            guard let order = currentOrder else { return }
            editingOrderId = order.id
            addressId = order.customerAddressId
            categoryId = order.category?.id
            problemTypeId = order.problemType?.id
            requestDescription = order.description ?? ""
            timingType = order.timing?.type ?? ""
            slotStart = order.timing?.slotStart
            selectedMaterialIndex =
                options("materials_responsibilities").firstIndex {
                    $0.code == order.materialsResponsibility
                } ?? -1
            selectedPricingIndex =
                options("pricing_modes").firstIndex {
                    $0.code == order.pricingMode
                } ?? -1
            requestBudget = order.budgetAmount ?? ""
            state = CustomerLogic.reduce(screen: "SCR-C03", input: problemInput())
        } else if action == "republish" {
            state = CustomerLogic.reduce(
                screen: "SCR-C35",
                input: ["event": .text("submitting"), "available_actions": .strings(state.visibleActions)])
            guard let orderId = currentOrderId else { return }
            do {
                let order = try await api.republish(orderId: orderId)
                currentOrderId = order.id
                currentOrder = order
                await loadOrder(id: order.id)
            } catch { state = CustomerLogic.reduce(screen: "SCR-C35", input: ["event": .text("error")]) }
        }
    }

    public func loadConversations(page: Int = 1) async {
        stopChatPolling()
        state = CustomerLogic.reduce(screen: "SCR-C18", input: ["event": .text("loading")])
        do {
            let conversations = try await api.conversations(page: page)
            conversationIDs = conversations.map(\.id)
            state = CustomerLogic.reduce(
                screen: "SCR-C18",
                input: [
                    "event": .text("loaded"),
                    "provider_names": .strings(conversations.map { $0.provider.name ?? "" }),
                    "order_summaries": .strings(
                        conversations.map {
                            "\($0.order.category ?? "") #\($0.order.number ?? "") · \($0.order.statusLabel ?? "")"
                        }),
                    "conversation_statuses": .strings(conversations.map(\.status)),
                    "last_messages": .strings(conversations.map { $0.lastMessage?.body ?? "" }),
                    "last_message_times": .strings(
                        conversations.map { displayDateTime($0.lastMessage?.createdAt) }),
                    "provider_verified": .strings(
                        conversations.map { $0.provider.isVerified ? "verified" : "" }),
                ])
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C18", input: ["event": .text("error")])
        }
    }

    public func openConversation(index: Int) async {
        guard conversationIDs.indices.contains(index) else { return }
        currentConversationId = conversationIDs[index]
        chatDraft = ""
        await loadConversation(id: conversationIDs[index], showLoading: true)
    }

    public func updateChatDraft(_ value: String) {
        chatDraft = String(value.prefix(1_000))
        state = CustomerLogic.reduce(screen: "SCR-C19", input: chatInput(event: "loaded"))
    }

    public func sendChatMessage() async {
        guard let id = currentConversationId else { return }
        let body = chatDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !body.isEmpty, state.canContinue else { return }
        state = CustomerLogic.reduce(screen: "SCR-C19", input: chatInput(event: "sending"))
        do {
            _ = try await api.sendMessage(conversationId: id, body: body, mediaIds: [])
            chatDraft = ""
            await loadConversation(id: id, showLoading: false)
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C19", input: ["event": .text("error")])
        }
    }

    public func sendChatImage(_ upload: CustomerMediaUpload) async {
        guard let id = currentConversationId else { return }
        state = CustomerLogic.reduce(screen: "SCR-C19", input: chatInput(event: "sending"))
        do {
            let media = try await api.uploadMedia(upload)
            _ = try await api.sendMessage(conversationId: id, body: nil, mediaIds: [media.id])
            await loadConversation(id: id, showLoading: false)
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C19", input: ["event": .text("error")])
        }
    }

    public func loadOrders(tabIndex: Int, page: Int = 1) async {
        stopChatPolling()
        let tab = tabIndex == 1 ? "past" : "current"
        state = CustomerLogic.reduce(screen: "SCR-C25", input: ["event": .text("loading")])
        do {
            let orders = try await api.orders(scope: tab, page: page)
            orderIDs = orders.map(\.id)
            orderStatuses = orders.map { $0.status ?? "" }
            state = CustomerLogic.reduce(
                screen: "SCR-C25",
                input: [
                    "event": .text("loaded"), "tab": .text(tab),
                    "order_titles": .strings(
                        orders.map {
                            "\($0.category?.name ?? "") — \($0.problemType?.name ?? "")"
                        }),
                    "order_subtitles": .strings(
                        orders.map {
                            "#\($0.number ?? "") · \($0.location?.area ?? "") · \(displayDateTime($0.createdAt))"
                        }),
                    "order_statuses": .strings(orders.map { $0.statusLabel ?? $0.displayStatus }),
                ])
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C25", input: ["event": .text("error")])
        }
    }

    public func selectOrder(index: Int) async {
        guard orderIDs.indices.contains(index) else { return }
        if ["CLOSED", "CANCELLED", "EXPIRED"].contains(orderStatuses[index]) {
            await loadOrderHistory(id: orderIDs[index])
        } else {
            await loadOrder(id: orderIDs[index])
        }
    }

    public func loadOrderHistory(id: Int) async {
        state = CustomerLogic.reduce(screen: "SCR-C26", input: ["event": .text("loading")])
        do {
            let order = try await api.order(id: id)
            let proposals = try await api.proposals(orderId: id).filter { $0.status == "APPROVED" }
            currentOrderId = id
            currentOrder = order
            currentOrderVersion = order.version ?? 0
            state = CustomerLogic.reduce(
                screen: "SCR-C26", input: orderHistoryInput(order, proposals: proposals))
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C26", input: ["event": .text("error")])
        }
    }

    public func loadAddresses(selectionMode: Bool = true) async {
        addressSelectionMode = selectionMode
        state = CustomerLogic.reduce(screen: "SCR-C14", input: ["event": .text("loading")])
        do {
            let addresses = try await api.addresses()
            addressPayloads = addresses
            addressIDs = addresses.map(\.id)
            let selected = addresses.firstIndex(where: \.isDefault) ?? -1
            if let preferred = addresses.first(where: \.isDefault) ?? addresses.first {
                addressId = preferred.id
                cityId = preferred.city.id
            }
            state = CustomerLogic.reduce(
                screen: "SCR-C14",
                input: [
                    "event": .text("loaded"), "selection_mode": .bool(selectionMode),
                    "address_labels": .strings(addresses.map(\.label)),
                    "address_details": .strings(addresses.map { "\($0.area.name) · \($0.addressText)" }),
                    "selected_index": .integer(selected),
                ])
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C14", input: ["event": .text("error")])
        }
    }

    public func selectAddress(index: Int) {
        addressId = addressIDs.indices.contains(index) ? addressIDs[index] : nil
        state.selectedIndex = index
        state.canContinue = addressId != nil
    }

    public func confirmAddress(index: Int) async {
        selectAddress(index: index)
        var cityID: Int?
        if addressPayloads.indices.contains(index) {
            addressLabel = customerAddressLabel(addressPayloads[index])
            cityID = addressPayloads[index].city.id
        }
        // BR-011: the chosen service must be served in the new address's city, before publish is refused.
        if let cityID, let categoryId {
            categoryAvailable = (try? await api.catalogCategoryIDs(cityID: cityID).contains(categoryId)) ?? true
        } else {
            categoryAvailable = true
        }
        openTiming()
    }

    /// C04 from the real draft: mode, address and timing (07).
    public func openTiming() {
        state = CustomerLogic.reduce(screen: "SCR-C04", input: timingInput())
    }

    public func selectTiming(index: Int) {
        guard options("timing_types").indices.contains(index) else { return }
        timingType = options("timing_types")[index].code
        if index == 0 {
            slotStart = nil
            selectedSlotLabel = ""
            openTiming()
        }
    }

    public func selectMaterial(index: Int) {
        guard options("materials_responsibilities").indices.contains(index) else { return }
        selectedMaterialIndex = index
        openTiming()
    }

    public func selectPricing(index: Int) {
        guard options("pricing_modes").indices.contains(index) else { return }
        selectedPricingIndex = index
        openTiming()
    }

    public func updateBudget(_ value: String) {
        requestBudget = String(value.prefix(12))
        openTiming()
    }

    private func timingInput() -> [String: CustomerInputValue] {
        [
            "event": .text("validate"), "operating_mode": .text(operatingMode),
            "category_available": .bool(categoryAvailable),
            "timing_type": .text(timingType.lowercased()), "now_available": .bool(nowAvailable()),
            "timing_labels": .strings(options("timing_types").map(\.label)),
            "selected_timing_index": .integer(
                options("timing_types").firstIndex { $0.code == timingType } ?? -1),
            "address_id": .text(addressId.map(String.init) ?? ""),
            "address_label": .text(addressLabel),
            "slot_id": .text(slotStart == nil ? "" : "selected"),
            "slot_label": .text(selectedSlotLabel),
            "material_labels": .strings(options("materials_responsibilities").map(\.label)),
            "selected_material_index": .integer(selectedMaterialIndex),
            "pricing_labels": .strings(options("pricing_modes").map(\.label)),
            "selected_pricing_index": .integer(selectedPricingIndex), "budget": .text(requestBudget),
        ]
    }

    private func nowAvailable() -> Bool {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "HH:mm"
        guard let from = formatter.date(from: serviceHours.from), let to = formatter.date(from: serviceHours.to),
            let now = formatter.date(from: formatter.string(from: Date()))
        else { return false }
        return from <= to ? now >= from && now <= to : now >= from || now <= to
    }

    /// C05 before the terms are accepted; the mode decides pricing copy.
    public func openReview() {
        state = CustomerLogic.reduce(
            screen: "SCR-C05", input: reviewInput(termsAccepted: false))
    }

    public func setTermsAccepted(_ accepted: Bool) {
        state = CustomerLogic.reduce(
            screen: "SCR-C05", input: reviewInput(termsAccepted: accepted))
    }

    private func reviewInput(termsAccepted: Bool) -> [String: CustomerInputValue] {
        var serviceRows: [String] = []
        if let category = selectedCategory(), let problem = selectedProblem() {
            serviceRows.append("\(category.name) — \(problem.name)")
        }
        if !requestDescription.isEmpty { serviceRows.append(requestDescription) }
        let material =
            options("materials_responsibilities").indices.contains(selectedMaterialIndex)
            ? options("materials_responsibilities")[selectedMaterialIndex].label : ""
        let timingLabel = options("timing_types").first { $0.code == timingType }?.label ?? ""
        let visitRows = [addressLabel, selectedSlotLabel.isEmpty ? timingLabel : selectedSlotLabel, material]
            .filter { !$0.isEmpty }
        var pricingRows: [String] = []
        if operatingMode == "marketplace" {
            if options("pricing_modes").indices.contains(selectedPricingIndex) {
                pricingRows.append(options("pricing_modes")[selectedPricingIndex].label)
            }
            if !requestBudget.isEmpty { pricingRows.append(requestBudget) }
        }
        return [
            "event": .text("validate"), "operating_mode": .text(operatingMode),
            "terms_accepted": .bool(termsAccepted), "service_rows": .strings(serviceRows),
            "visit_rows": .strings(visitRows), "pricing_rows": .strings(pricingRows),
        ]
    }

    public func openNewAddress() async {
        state = CustomerLogic.reduce(screen: "SCR-C15", input: ["event": .text("loading")])
        do {
            guard let city = try await api.cities().first else { throw APIClientError.invalidResponse }
            let areas = try await api.areas(cityId: city.id)
            cityId = city.id
            areaIDs = areas.map(\.id)
            addressDraft = CustomerAddressDraft(cityId: city.id)
            state = CustomerLogic.reduce(
                screen: "SCR-C15", input: addressInput(event: "validate", areaLabels: areas.map(\.name)))
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C15", input: ["event": .text("error")])
        }
    }

    public func openEditAddress(index: Int) async {
        state = CustomerLogic.reduce(screen: "SCR-C15", input: ["event": .text("loading")])
        do {
            guard addressPayloads.indices.contains(index) else { throw APIClientError.invalidResponse }
            let address = addressPayloads[index]
            let areas = try await api.areas(cityId: address.city.id)
            cityId = address.city.id
            areaIDs = areas.map(\.id)
            addressDraft = CustomerAddressDraft(
                id: address.id, cityId: address.city.id, areaId: address.area.id,
                selectedAreaIndex: areaIDs.firstIndex(of: address.area.id) ?? -1,
                label: address.label, addressText: address.addressText,
                building: address.building ?? "", floor: address.floor ?? "",
                apartment: address.apartment ?? "", landmark: address.landmark ?? "",
                latitude: Double(address.lat), longitude: Double(address.lng),
                isDefault: address.isDefault)
            state = CustomerLogic.reduce(
                screen: "SCR-C15", input: addressInput(event: "validate", areaLabels: areas.map(\.name)))
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C15", input: ["event": .text("error")])
        }
    }

    public func updateAddressField(_ field: String, value: String) {
        switch field {
        case "label": addressDraft.label = value
        case "address_text": addressDraft.addressText = value
        case "building": addressDraft.building = value
        case "floor": addressDraft.floor = value
        case "apartment": addressDraft.apartment = value
        case "landmark": addressDraft.landmark = value
        default: break
        }
        state = CustomerLogic.reduce(
            screen: "SCR-C15", input: addressInput(event: "validate", areaLabels: state.options))
    }

    public func selectArea(index: Int) {
        addressDraft.areaId = areaIDs.indices.contains(index) ? areaIDs[index] : nil
        addressDraft.selectedAreaIndex = index
        state = CustomerLogic.reduce(
            screen: "SCR-C15", input: addressInput(event: "validate", areaLabels: state.options))
    }

    public func updateAddressLocation(latitude: Double, longitude: Double) {
        addressDraft.latitude = latitude
        addressDraft.longitude = longitude
        state = CustomerLogic.reduce(
            screen: "SCR-C15", input: addressInput(event: "validate", areaLabels: state.options))
    }

    public func toggleAddressDefault() {
        addressDraft.isDefault.toggle()
        state = CustomerLogic.reduce(
            screen: "SCR-C15", input: addressInput(event: "validate", areaLabels: state.options))
    }

    public func saveAddress() async {
        guard state.canContinue, let city = addressDraft.cityId, let area = addressDraft.areaId,
            let latitude = addressDraft.latitude, let longitude = addressDraft.longitude
        else { return }
        state = CustomerLogic.reduce(
            screen: "SCR-C15", input: addressInput(event: "saving", areaLabels: state.options))
        do {
            _ = try await api.saveAddress(
                id: addressDraft.id,
                body: CustomerAddressBody(
                    label: addressDraft.label, cityId: city, areaId: area,
                    addressText: addressDraft.addressText, building: addressDraft.building.nilIfEmpty,
                    floor: addressDraft.floor.nilIfEmpty, apartment: addressDraft.apartment.nilIfEmpty,
                    landmark: addressDraft.landmark.nilIfEmpty, lat: latitude, lng: longitude,
                    isDefault: addressDraft.isDefault))
            await loadAddresses(selectionMode: addressSelectionMode)
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C15", input: ["event": .text("error")])
        }
    }

    public func requestDeleteAddress(index: Int) {
        guard addressIDs.indices.contains(index) else { return }
        state = CustomerLogic.reduce(
            screen: "SCR-C14", input: addressStateInput(event: "confirm_delete", pendingIndex: index))
    }

    public func cancelDeleteAddress() {
        state = CustomerLogic.reduce(screen: "SCR-C14", input: addressStateInput(event: "loaded"))
    }

    public func confirmDeleteAddress() async {
        let index = state.pendingIndex
        guard addressIDs.indices.contains(index) else { return }
        state = CustomerLogic.reduce(
            screen: "SCR-C14", input: addressStateInput(event: "deleting", pendingIndex: index))
        do {
            try await api.deleteAddress(id: addressIDs[index])
            await loadAddresses(selectionMode: addressSelectionMode)
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C14", input: ["event": .text("error")])
        }
    }

    private func addressStateInput(
        event: String, pendingIndex: Int = -1
    ) -> [String: CustomerInputValue] {
        [
            "event": .text(event), "selection_mode": .bool(addressSelectionMode),
            "address_labels": .strings(addressPayloads.map(\.label)),
            "address_details": .strings(addressPayloads.map { "\($0.area.name) · \($0.addressText)" }),
            "selected_index": .integer(state.selectedIndex),
            "pending_delete_index": .integer(pendingIndex),
        ]
    }

    public func loadSlots(dayLabels: [String], morning: String, evening: String) async {
        guard let cityId else {
            state = CustomerLogic.reduce(screen: "SCR-C16", input: ["event": .text("error")])
            return
        }
        dayDates = customerDayDates()
        await loadSlots(
            cityId: cityId, dayIndex: 0, dayLabels: dayLabels, morning: morning, evening: evening)
    }

    public func selectDay(index: Int, dayLabels: [String], morning: String, evening: String) async {
        guard let cityId else { return }
        await loadSlots(
            cityId: cityId, dayIndex: index, dayLabels: dayLabels, morning: morning, evening: evening)
    }

    public func selectSlot(index: Int) {
        selectedSlotIndex = index
        state.selectedIndex = index
        state.canContinue = slotStarts.indices.contains(index)
    }

    public func confirmSlot() {
        guard slotStarts.indices.contains(selectedSlotIndex) else { return }
        guard options("timing_types").indices.contains(1) else { return }
        timingType = options("timing_types")[1].code
        slotStart = slotStarts[selectedSlotIndex]
        // The chosen day and period, e.g. «غدًا · 1:00 م–3:00 م» (6ب).
        let day = state.options.indices.contains(state.selectedOptionIndex) ? state.options[state.selectedOptionIndex] : ""
        let period = state.items.indices.contains(selectedSlotIndex) ? state.items[selectedSlotIndex] : ""
        selectedSlotLabel = [day, period].filter { !$0.isEmpty }.joined(separator: " · ")
        openTiming()
    }

    public func openMedia() {
        state = CustomerLogic.reduce(screen: "SCR-C17", input: mediaInput())
    }

    public func canAddMedia(kind: String) -> Bool {
        switch kind {
        case "photo": mediaDrafts.filter { $0.kind == kind }.count < 5
        case "video", "audio": !mediaDrafts.contains { $0.kind == kind }
        default: false
        }
    }

    public func setMediaRecording(_ recording: Bool) {
        mediaRecording = recording
        state = CustomerLogic.reduce(screen: "SCR-C17", input: mediaInput())
    }

    public func completeMediaSelection() {
        state = CustomerLogic.reduce(screen: "SCR-C03", input: problemInput())
    }

    public func uploadMedia(_ upload: CustomerMediaUpload, kind: String) async {
        let draft = CustomerMediaDraft(id: nil, kind: kind, state: "uploading", upload: upload)
        mediaDrafts.append(draft)
        state = CustomerLogic.reduce(screen: "SCR-C17", input: mediaInput())
        await uploadMedia(draft)
    }

    public func retryMedia(index: Int) async {
        guard mediaDrafts.indices.contains(index), mediaDrafts[index].state == "failed",
            let upload = mediaDrafts[index].upload
        else { return }
        let draft = CustomerMediaDraft(
            token: mediaDrafts[index].token, id: nil, kind: mediaDrafts[index].kind,
            state: "uploading", upload: upload)
        mediaDrafts[index] = draft
        state = CustomerLogic.reduce(screen: "SCR-C17", input: mediaInput())
        await uploadMedia(draft)
    }

    private func uploadMedia(_ draft: CustomerMediaDraft) async {
        guard let upload = draft.upload else { return }
        do {
            let uploaded = try await api.uploadMedia(upload)
            if let index = mediaDrafts.firstIndex(where: { $0.token == draft.token }) {
                mediaDrafts[index] = CustomerMediaDraft(
                    token: draft.token, id: uploaded.id, kind: draft.kind, state: "uploaded")
            }
        } catch {
            if let index = mediaDrafts.firstIndex(where: { $0.token == draft.token }) {
                mediaDrafts[index] = CustomerMediaDraft(
                    token: draft.token, id: nil, kind: draft.kind, state: "failed", upload: upload)
            }
        }
        state = CustomerLogic.reduce(screen: "SCR-C17", input: mediaInput())
    }

    public func deleteMedia(index: Int) async {
        guard mediaDrafts.indices.contains(index) else { return }
        guard mediaDrafts[index].state != "uploading" else { return }
        if let id = mediaDrafts[index].id { try? await api.deleteMedia(id: id) }
        mediaDrafts.remove(at: index)
        state = CustomerLogic.reduce(screen: "SCR-C17", input: mediaInput())
    }

    public func refreshTracking() async {
        guard let orderId = currentOrderId else { return }
        trackingPayload = try? await api.tracking(orderId: orderId)
    }

    public var orderDestinationLatitude: Double? { currentOrder?.location?.latitude }
    public var orderDestinationLongitude: Double? { currentOrder?.location?.longitude }

    public func loadHome() async {
        state = CustomerLogic.reduce(screen: "SCR-C01", input: ["event": .text("loading")])
        do {
            let home = try await api.home()
            currentOrderId = home.firstOrderId
            orderIDs = home.orders.map(\.id)
            orderStatuses = home.orders.map { $0.status ?? "" }
            addressId = home.addressId
            categoryId = nil
            problemTypeId = nil
            catalog = home.categories
            optionLists = home.optionLists
            optionDefaults = home.optionDefaults
            timingType = optionDefaults["timing_type"] ?? ""
            selectedMaterialIndex =
                options("materials_responsibilities").firstIndex {
                    $0.code == optionDefaults["materials_responsibility"]
                } ?? -1
            selectedPricingIndex = -1
            requestBudget = ""
            serviceHours = home.serviceHours
            operatingMode = home.operatingMode
            addressLabel = home.addressLabel
            if let city = home.addressCityID { cityId = city }
            state = CustomerLogic.reduce(
                screen: "SCR-C01",
                input: [
                    "event": .text("loaded"), "operating_mode": .text(home.operatingMode),
                    "has_orders": .bool(home.orderCount > 0),
                    "customer_name": .text(home.customerName),
                    "address_label": .text(home.addressLabel),
                    "category_labels": .strings(catalog.map(\.name)),
                    "category_icons": .strings(catalog.map { $0.iconKey ?? "" }),
                    "selected_category_index": .integer(-1),
                    "order_titles": .strings(home.orders.map { orderSummary($0)[0] }),
                    "order_subtitles": .strings(home.orders.map { orderSummary($0)[1] }),
                    "order_statuses": .strings(home.orders.map { orderSummary($0)[2] }),
                ])
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C01", input: ["event": .text("error")])
        }
    }

    public func openProblem() {
        state = CustomerLogic.reduce(screen: "SCR-C03", input: problemInput())
    }

    /// C01 urgent card (DEC-063): the same request flow, with "now" chosen for the timing step.
    public func openUrgentRequest() {
        if options("timing_types").contains(where: { $0.code == Self.urgentTiming }) { timingType = Self.urgentTiming }
        openProblem()
    }

    /// The timing code the urgent card preselects.
    static let urgentTiming = "NOW"

    public func selectCategory(index: Int) {
        guard
            let choice = CustomerCatalogSelection.category(
                categoryIds: catalog.map(\.id),
                problemIdsByCategory: catalog.map { $0.problemTypes.map(\.id) },
                selectedIndex: index,
                currentProblemId: problemTypeId)
        else { return }
        categoryId = choice.categoryId
        problemTypeId = choice.problemTypeId
        state = CustomerLogic.reduce(screen: "SCR-C03", input: problemInput())
    }

    public func selectProblem(index: Int) {
        guard
            let choice = CustomerCatalogSelection.problem(
                categoryId: categoryId,
                problemIds: selectedCategory()?.problemTypes.map(\.id) ?? [],
                selectedIndex: index)
        else { return }
        categoryId = choice.categoryId
        problemTypeId = choice.problemTypeId
        state = CustomerLogic.reduce(screen: "SCR-C03", input: problemInput())
    }

    public func updateProblemDescription(_ value: String) {
        requestDescription = String(value.prefix(1_001))
        state = CustomerLogic.reduce(screen: "SCR-C03", input: problemInput())
    }

    private func selectedCategory() -> CustomerCategoryReference? {
        catalog.first { $0.id == categoryId }
    }

    private func selectedProblem() -> CustomerProblemTypeReference? {
        selectedCategory()?.problemTypes.first { $0.id == problemTypeId }
    }

    private func problemInput() -> [String: CustomerInputValue] {
        let problems = selectedCategory()?.problemTypes ?? []
        return [
            "event": .text("validate"),
            "category_labels": .strings(catalog.map(\.name)),
            "category_icons": .strings(catalog.map { $0.iconKey ?? "" }),
            "selected_category_index": .integer(catalog.firstIndex { $0.id == categoryId } ?? -1),
            "problem_labels": .strings(problems.map(\.name)),
            "problem_other": .strings(problems.map { String($0.isOther) }),
            "selected_problem_index": .integer(problems.firstIndex { $0.id == problemTypeId } ?? -1),
            "description": .text(requestDescription),
            "media_count": .integer(mediaDrafts.count { $0.id != nil }),
        ]
    }

    /// C01 current-order card: the same title, subtitle and status as the C25 row.
    private func orderSummary(_ order: CustomerOrderSummary) -> [String] {
        [
            "\(order.category?.name ?? "") — \(order.problemType?.name ?? "")",
            "#\(order.number ?? "") · \(order.location?.area ?? "") · \(displayDateTime(order.createdAt))",
            order.statusLabel ?? order.displayStatus,
        ]
    }

    /// C02 header from `GET /me`, not sample text.
    public func loadAccountSummary() async {
        state = CustomerLogic.reduce(screen: "SCR-C02", input: ["event": .text("loading")])
        do {
            let account = try await api.account()
            state = CustomerLogic.reduce(
                screen: "SCR-C02",
                input: [
                    "event": .text("loaded"), "is_verified": .bool(account.isVerified ?? false),
                    "name": .text(account.name), "phone": .text(account.phone),
                ])
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C02", input: ["event": .text("error")])
        }
    }

    public func loadOrder(id: Int, screen: String = "SCR-C09") async {
        state = CustomerLogic.reduce(screen: screen, input: ["event": .text("loading")])
        do {
            let order = try await api.order(id: id)
            currentOrderId = id
            currentOrderVersion = order.version ?? 0
            currentOrder = order
            // An OPEN order in employee mode waits for assignment on C36 (SCR-C36).
            if screen == "SCR-C09" && operatingMode == "staff" && order.status == "OPEN" {
                state = CustomerLogic.reduce(screen: "SCR-C36", input: assignmentInput(order))
                return
            }
            state = CustomerLogic.reduce(
                screen: screen,
                input: [
                    "event": .text("loaded"), "status": .text(order.status ?? ""),
                    "display_status": .text(order.displayStatus),
                    "available_actions": .strings(order.availableActions),
                    "stepper": .bool(order.stepper != nil),
                    "step_states": .strings(order.stepper?.map(\.state) ?? []),
                ])
        } catch {
            state = CustomerLogic.reduce(screen: screen, input: ["event": .text("error")])
        }
    }

    public func loadPaymentSummary() async {
        guard let orderId = currentOrderId else { return }
        state = CustomerLogic.reduce(screen: "SCR-C21", input: ["event": .text("loading")])
        do {
            let order = try await api.order(id: orderId)
            currentOrder = order
            currentOrderVersion = order.version ?? currentOrderVersion
            state = CustomerLogic.reduce(
                screen: "SCR-C21", input: paymentSummaryInput(order, event: "loaded"))
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C21", input: ["event": .text("error")])
        }
    }

    public func selectPaymentMethod(index: Int) async {
        guard let orderId = currentOrderId, let order = currentOrder else { return }
        guard options("payment_methods").indices.contains(index) else { return }
        let method = options("payment_methods")[index].code
        state = CustomerLogic.reduce(
            screen: "SCR-C21", input: paymentSummaryInput(order, event: "switching"))
        do {
            let updated = try await api.changePaymentMethod(
                orderId: orderId, method: method, expectedVersion: currentOrderVersion)
            currentOrder = updated
            currentOrderVersion = updated.version ?? currentOrderVersion
            state = CustomerLogic.reduce(
                screen: "SCR-C21", input: paymentSummaryInput(updated, event: "loaded"))
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C21", input: ["event": .text("error")])
        }
    }

    public func openElectronicPayment() {
        guard let order = currentOrder else { return }
        selectedPaymentChannel = -1
        state = CustomerLogic.reduce(
            screen: "SCR-C22", input: paymentInput(order, payment: nil, event: "loaded"))
    }

    public func selectPaymentChannel(index: Int) {
        guard let order = currentOrder else { return }
        selectedPaymentChannel = index
        state = CustomerLogic.reduce(
            screen: "SCR-C22", input: paymentInput(order, payment: nil, event: "loaded"))
    }

    public func createElectronicPayment() async {
        guard let orderId = currentOrderId, let order = currentOrder,
            options("payment_channels").indices.contains(selectedPaymentChannel)
        else { return }
        state = CustomerLogic.reduce(
            screen: "SCR-C22", input: paymentInput(order, payment: nil, event: "creating"))
        do {
            history.commit()
            let payment = try await api.createPayment(
                orderId: orderId, channel: options("payment_channels")[selectedPaymentChannel].code,
                expectedVersion: currentOrderVersion)
            state = CustomerLogic.reduce(
                screen: "SCR-C22", input: paymentInput(order, payment: payment, event: "loaded"))
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C22", input: ["event": .text("error")])
        }
    }

    public func loadCompletionSummary() async {
        guard let orderId = currentOrderId else { return }
        state = CustomerLogic.reduce(screen: "SCR-C23", input: ["event": .text("loading")])
        do {
            let order = try await api.order(id: orderId)
            currentOrder = order
            currentOrderVersion = order.version ?? currentOrderVersion
            state = CustomerLogic.reduce(
                screen: "SCR-C23", input: completionInput(order, event: "loaded"))
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C23", input: ["event": .text("error")])
        }
    }

    public func confirmCompletion() async {
        guard let orderId = currentOrderId, let order = currentOrder else { return }
        state = CustomerLogic.reduce(
            screen: "SCR-C23", input: completionInput(order, event: "submitting"))
        do {
            history.commit()
            let updated = try await api.confirmCompletion(
                orderId: orderId, expectedVersion: currentOrderVersion)
            currentOrder = updated
            currentOrderVersion = updated.version ?? currentOrderVersion
            openRating()
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C23", input: ["event": .text("error")])
        }
    }

    public func openRating() {
        ratingValues = [0, 0, 0]
        ratingComment = ""
        state = CustomerLogic.reduce(screen: "SCR-C24", input: ratingInput(event: "loaded"))
    }

    public func updateRating(dimension: Int, value: Int) {
        guard ratingValues.indices.contains(dimension), (1...5).contains(value) else { return }
        ratingValues[dimension] = value
        state = CustomerLogic.reduce(screen: "SCR-C24", input: ratingInput(event: "loaded"))
    }

    public func updateRatingComment(_ value: String) {
        ratingComment = value
        state = CustomerLogic.reduce(screen: "SCR-C24", input: ratingInput(event: "loaded"))
    }

    public func submitRating() async {
        guard let orderId = currentOrderId, state.canContinue else { return }
        state = CustomerLogic.reduce(screen: "SCR-C24", input: ratingInput(event: "submitting"))
        history.commit()
        do {
            try await api.submitReview(
                orderId: orderId, quality: ratingValues[0], punctuality: ratingValues[1],
                conduct: ratingValues[2],
                comment: ratingComment.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty)
            state = CustomerLogic.reduce(screen: "SCR-C24", input: ratingInput(event: "success"))
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C24", input: ["event": .text("error")])
        }
    }

    public func publish(_ body: PublishRequestBody, idempotencyKey: String) async {
        state = CustomerLogic.reduce(screen: "SCR-C05", input: reviewInput(termsAccepted: true).merging(["event": .text("submitting")]) { $1 })
        history.commit()
        do {
            let order = try await api.publish(body, idempotencyKey: idempotencyKey)
            currentOrderId = order.id
            currentOrder = order
            currentOrderVersion = order.version ?? 0
            if operatingMode == "staff" {
                state = CustomerLogic.reduce(screen: "SCR-C36", input: assignmentInput(order))
            } else {
                currentOffers = []
                state = CustomerLogic.reduce(screen: "SCR-C06", input: offersInput(order))
            }
        } catch {
            state = publishRefused(error)
        }
    }

    /// A refused publish stays on C05 with the server's reason (SCR-C05 §الأخطاء), never on the target screen.
    private func publishRefused(_ error: Error) -> CustomerUIState {
        let message = (error as? APIClientError)?.serverMessage ?? ""
        return CustomerLogic.reduce(
            screen: "SCR-C05",
            input: reviewInput(termsAccepted: true).merging(["error_message": .text(message)]) { $1 })
    }

    public func publishDraft() async {
        guard let addressId, let categoryId, let problemTypeId else {
            state = CustomerLogic.reduce(screen: "SCR-C05", input: ["event": .text("error")])
            return
        }
        let body = PublishRequestBody(
            customerAddressId: addressId, categoryId: categoryId, problemTypeId: problemTypeId,
            timingType: timingType,
            materialsResponsibility: options("materials_responsibilities").indices.contains(
                selectedMaterialIndex)
                ? options("materials_responsibilities")[selectedMaterialIndex].code : "",
            termsAccepted: true,
            description: requestDescription.isEmpty ? nil : requestDescription,
            mediaIds: mediaDrafts.compactMap(\.id), slotStart: slotStart,
            pricingMode: operatingMode == "marketplace"
                && options("pricing_modes").indices.contains(selectedPricingIndex)
                ? options("pricing_modes")[selectedPricingIndex].code : nil,
            budgetAmount: operatingMode == "marketplace" ? requestBudget.nilIfEmpty : nil)
        if let editingOrderId {
            state = CustomerLogic.reduce(screen: "SCR-C05", input: reviewInput(termsAccepted: true).merging(["event": .text("submitting")]) { $1 })
            history.commit()
            do {
                let order = try await api.updateOrder(id: editingOrderId, body: body)
                self.editingOrderId = nil
                currentOrderId = order.id
                currentOrder = order
                currentOrderVersion = order.version ?? 0
                if operatingMode == "staff" {
                    state = CustomerLogic.reduce(screen: "SCR-C36", input: assignmentInput(order))
                } else {
                    currentOffers = []
                    state = CustomerLogic.reduce(screen: "SCR-C06", input: offersInput(order))
                }
            } catch { state = publishRefused(error) }
        } else {
            await publish(body, idempotencyKey: UUID().uuidString)
        }
    }

    public func openCurrentOrder() async {
        guard let currentOrderId else { return }
        await loadOrder(id: currentOrderId)
    }

    public func loadOffers(orderId: Int) async {
        state = CustomerLogic.reduce(screen: "SCR-C06", input: ["event": .text("loading")])
        do {
            let order = try await api.order(id: orderId)
            currentOrder = order
            currentOrderId = orderId
            currentOrderVersion = order.version ?? 0
            currentOffers = try await api.offers(orderId: orderId, sort: offerSortCode())
            state = CustomerLogic.reduce(screen: "SCR-C06", input: offersInput(order))
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C06", input: ["event": .text("error")])
        }
    }

    public func selectOffersSort(index: Int) async {
        guard (0...2).contains(index), index != 2 || state.showEta, let order = currentOrder else {
            return
        }
        offersSortIndex = index
        switch index {
        case 1: currentOffers.sort { Decimal(string: $0.price) ?? 0 < Decimal(string: $1.price) ?? 0 }
        case 2: currentOffers.sort { $0.etaMinutes ?? Int.max < $1.etaMinutes ?? Int.max }
        default:
            currentOffers.sort {
                Decimal(string: $0.provider.ratingAverage ?? "0") ?? 0
                    > Decimal(string: $1.provider.ratingAverage ?? "0") ?? 0
            }
        }
        state = CustomerLogic.reduce(screen: "SCR-C06", input: offersInput(order))
    }

    public func openOffer(index: Int) {
        guard currentOffers.indices.contains(index) else { return }
        currentOfferId = currentOffers[index].id
        selectedPaymentChannel =
            options("payment_methods").firstIndex {
                $0.code == optionDefaults["payment_method"]
            } ?? 0
        state = CustomerLogic.reduce(
            screen: "SCR-C08", input: offerDetailsInput(currentOffers[index], event: "loaded"))
    }

    public func openOfferProvider(index: Int) async {
        guard currentOffers.indices.contains(index), let orderId = currentOrderId else { return }
        currentOfferId = currentOffers[index].id
        await loadProvider(id: currentOffers[index].provider.id, orderId: orderId)
    }

    public func openCurrentOffer() {
        guard let offer = currentOffers.first(where: { $0.id == currentOfferId }) else { return }
        selectedPaymentChannel =
            options("payment_methods").firstIndex {
                $0.code == optionDefaults["payment_method"]
            } ?? 0
        state = CustomerLogic.reduce(screen: "SCR-C08", input: offerDetailsInput(offer, event: "loaded"))
    }

    public func selectOfferPayment(index: Int) {
        guard options("payment_methods").indices.contains(index),
            let offer = currentOffers.first(where: { $0.id == currentOfferId })
        else { return }
        selectedPaymentChannel = index
        state = CustomerLogic.reduce(screen: "SCR-C08", input: offerDetailsInput(offer, event: "loaded"))
    }

    public func confirmSelectedOffer() async {
        guard let orderId = currentOrderId, let offerId = currentOfferId,
            options("payment_methods").indices.contains(selectedPaymentChannel),
            let offer = currentOffers.first(where: { $0.id == offerId })
        else { return }
        state = CustomerLogic.reduce(
            screen: "SCR-C08", input: offerDetailsInput(offer, event: "submitting"))
        do {
            history.commit()
            let updated = try await api.acceptOffer(
                orderId: orderId, offerId: offerId, expectedVersion: currentOrderVersion,
                paymentMethod: options("payment_methods")[selectedPaymentChannel].code)
            currentOrder = updated
            currentOrderVersion = updated.version ?? currentOrderVersion
            await loadOrder(id: orderId)
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C08", input: ["event": .text("error")])
        }
    }

    public func loadProvider(id: Int, orderId: Int) async {
        state = CustomerLogic.reduce(screen: "SCR-C07", input: ["event": .text("loading")])
        do {
            let provider = try await api.provider(id: id, orderId: orderId)
            currentProviderId = id
            currentOrderId = orderId
            currentProviderActions = provider.availableActions
            state = CustomerLogic.reduce(
                screen: "SCR-C07",
                input: [
                    "event": .text("loaded"), "provider_available": .bool(provider.availableNow),
                    "provider_name": .text(provider.name),
                    "provider_rating": .text(provider.ratingAverage ?? ""),
                    "provider_services": .text(provider.completedOrders.map(String.init) ?? ""),
                    "provider_experience": .text(provider.experienceYears.map(String.init) ?? ""),
                    "provider_about": .text(provider.bio ?? ""),
                    "offer_price": .text(
                        currentOffers.first { $0.provider.id == id }.map { formatAmount($0.price) } ?? ""),
                    "provider_verified": .bool(provider.isVerified),
                    "specialties": .strings(provider.specialties.map(\.name)),
                    "rating_values": .strings([
                        provider.ratingBreakdown.quality ?? "",
                        provider.ratingBreakdown.punctuality ?? "",
                        provider.ratingBreakdown.conduct ?? "",
                    ]),
                    "review_rows": .strings(
                        provider.reviews.map { review in
                            let rating = (review.quality + review.punctuality + review.conduct) / 3
                            return [
                                review.customerName, String(rating), review.comment ?? "",
                                displayDateTime(review.createdAt),
                            ].joined(separator: "|")
                        }),
                    "available_actions": .strings(provider.availableActions),
                ])
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C07", input: ["event": .text("error")])
        }
    }

    public func openDispute() async {
        guard let orderId = currentOrderId else { return }
        supportReasonIndex = -1
        supportDescription = ""
        disputeMedia = []
        state = CustomerLogic.reduce(screen: "SCR-C27", input: ["event": .text("loading")])
        do {
            let disputes = try await api.disputes(orderId: orderId)
            let existing = disputes.first
            state = CustomerLogic.reduce(
                screen: "SCR-C27", input: disputeInput(event: "loaded", dispute: existing))
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C27", input: ["event": .text("error")])
        }
    }

    public func selectSupportReason(index: Int) {
        supportReasonIndex = index
        state = CustomerLogic.reduce(
            screen: state.screen,
            input: state.screen == "SCR-C28"
                ? providerReportInput(event: "loaded") : disputeInput(event: "loaded"))
    }

    public func updateSupportDescription(_ value: String) {
        supportDescription = String(value.prefix(1_001))
        state = CustomerLogic.reduce(
            screen: state.screen,
            input: state.screen == "SCR-C28"
                ? providerReportInput(event: "loaded") : disputeInput(event: "loaded"))
    }

    public func uploadDisputePhoto(_ upload: CustomerMediaUpload) async {
        disputeMedia.append(CustomerMediaDraft(id: nil, kind: "photo", state: "uploading"))
        state = CustomerLogic.reduce(screen: "SCR-C27", input: disputeInput(event: "loaded"))
        do {
            let item = try await api.uploadMedia(upload)
            disputeMedia[disputeMedia.count - 1] = CustomerMediaDraft(
                id: item.id, kind: "photo", state: "uploaded")
        } catch {
            disputeMedia[disputeMedia.count - 1] = CustomerMediaDraft(
                id: nil, kind: "photo", state: "failed")
        }
        state = CustomerLogic.reduce(screen: "SCR-C27", input: disputeInput(event: "loaded"))
    }

    public func removeDisputePhoto(index: Int) async {
        guard disputeMedia.indices.contains(index) else { return }
        if let id = disputeMedia[index].id { try? await api.deleteMedia(id: id) }
        disputeMedia.remove(at: index)
        state = CustomerLogic.reduce(screen: "SCR-C27", input: disputeInput(event: "loaded"))
    }

    public func submitDispute() async {
        guard let orderId = currentOrderId,
            options("dispute_reasons").indices.contains(supportReasonIndex), state.canContinue
        else { return }
        state = CustomerLogic.reduce(screen: "SCR-C27", input: disputeInput(event: "submitting"))
        history.commit()
        do {
            let dispute = try await api.openDispute(
                orderId: orderId, reasonCode: options("dispute_reasons")[supportReasonIndex].code,
                description: supportDescription.trimmingCharacters(in: .whitespacesAndNewlines),
                mediaIds: disputeMedia.compactMap(\.id))
            state = CustomerLogic.reduce(
                screen: "SCR-C27", input: disputeInput(event: "success", dispute: dispute))
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C27", input: ["event": .text("error")])
        }
    }

    public func openProviderReport() {
        supportReasonIndex = -1
        supportDescription = ""
        state = CustomerLogic.reduce(screen: "SCR-C28", input: providerReportInput(event: "loaded"))
    }

    public func returnToProvider() async {
        guard let providerId = currentProviderId, let orderId = currentOrderId else { return }
        await loadProvider(id: providerId, orderId: orderId)
    }

    public func submitProviderReport() async {
        guard let providerId = currentProviderId, let orderId = currentOrderId,
            options("provider_report_reasons").indices.contains(supportReasonIndex), state.canContinue
        else { return }
        state = CustomerLogic.reduce(screen: "SCR-C28", input: providerReportInput(event: "submitting"))
        history.commit()
        do {
            try await api.reportProvider(
                providerId: providerId, orderId: orderId,
                reasonCode: options("provider_report_reasons")[supportReasonIndex].code,
                description: supportDescription.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty)
            state = CustomerLogic.reduce(screen: "SCR-C28", input: providerReportInput(event: "success"))
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C28", input: ["event": .text("error")])
        }
    }

    public func openCancellation() {
        cancellationReasonIndex = -1
        cancellationNote = ""
        state = CustomerLogic.reduce(
            screen: "SCR-C20", input: cancellationInput(event: "loaded"))
    }

    public func selectCancellationReason(index: Int) {
        cancellationReasonIndex = index
        state = CustomerLogic.reduce(
            screen: "SCR-C20", input: cancellationInput(event: "loaded"))
    }

    public func updateCancellationNote(_ value: String) {
        cancellationNote = value
        state = CustomerLogic.reduce(
            screen: "SCR-C20", input: cancellationInput(event: "loaded"))
    }

    public func submitCancellation() async {
        guard let orderId = currentOrderId,
            options("customer_cancellation_reasons").indices.contains(cancellationReasonIndex)
        else { return }
        state = CustomerLogic.reduce(
            screen: "SCR-C20", input: cancellationInput(event: "submitting"))
        do {
            history.commit()
            let order = try await api.cancelOrder(
                orderId: orderId,
                reasonCode: options("customer_cancellation_reasons")[cancellationReasonIndex].code,
                note: cancellationNote.isEmpty ? nil : cancellationNote,
                expectedVersion: currentOrderVersion)
            currentOrderVersion = order.version ?? currentOrderVersion
            state = CustomerLogic.reduce(screen: "SCR-C09", input: trackingInput(order))
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C20", input: ["event": .text("error")])
        }
    }

    public func loadPendingProposal() async {
        guard let orderId = currentOrderId else { return }
        state = CustomerLogic.reduce(screen: "SCR-C30", input: ["event": .text("loading")])
        do {
            let order = try await api.order(id: orderId)
            currentOrderVersion = order.version ?? currentOrderVersion
            guard
                let proposal = try await api.proposals(orderId: orderId).last(where: {
                    $0.status == "PENDING"
                })
            else {
                throw APIClientError.invalidResponse
            }
            currentProposalId = proposal.id
            let screen = proposal.type == "EXECUTION_QUOTE" ? "SCR-C30" : "SCR-C31"
            state = CustomerLogic.reduce(
                screen: screen, input: proposalInput(order: order, proposal: proposal, event: "loaded"))
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C30", input: ["event": .text("error")])
        }
    }

    public func decideProposal(action: String) async {
        guard let orderId = currentOrderId, let proposalId = currentProposalId else { return }
        let screen = state.screen
        var input = proposalStateInput(event: "submitting")
        state = CustomerLogic.reduce(screen: screen, input: input)
        do {
            history.commit()
            let order = try await api.decideProposal(
                orderId: orderId, proposalId: proposalId, approve: action == "approve_proposal",
                expectedVersion: currentOrderVersion)
            currentOrderVersion = order.version ?? currentOrderVersion
            input = trackingInput(order)
            state = CustomerLogic.reduce(screen: "SCR-C09", input: input)
        } catch {
            state = CustomerLogic.reduce(screen: screen, input: ["event": .text("error")])
        }
    }

    private func cancellationInput(event: String) -> [String: CustomerInputValue] {
        [
            "event": .text(event),
            "reason_labels": .strings(options("customer_cancellation_reasons").map(\.label)),
            "selected_reason_index": .integer(cancellationReasonIndex), "note": .text(cancellationNote),
            "available_actions": .strings(["cancel"]),
        ]
    }

    private func loadConversation(id: Int, showLoading: Bool) async {
        stopChatPolling()
        if showLoading {
            state = CustomerLogic.reduce(screen: "SCR-C19", input: ["event": .text("loading")])
        }
        do {
            let response = try await api.conversationMessages(id: id, page: 1)
            currentConversation = response.conversation
            chatMessages = response.messages
            state = CustomerLogic.reduce(screen: "SCR-C19", input: chatInput(event: "loaded"))
            scheduleChatPoll(id: id)
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C19", input: ["event": .text("error")])
        }
    }

    private func scheduleChatPoll(id: Int) {
        chatPollingTask = Task { [weak self] in
            try? await Task.sleep(for: .seconds(5))
            guard !Task.isCancelled, let self, self.currentConversationId == id,
                self.state.screen == "SCR-C19"
            else { return }
            await self.loadConversation(id: id, showLoading: false)
        }
    }

    private func stopChatPolling() {
        chatPollingTask?.cancel()
        chatPollingTask = nil
    }

    private func chatInput(event: String) -> [String: CustomerInputValue] {
        let conversation = currentConversation
        return [
            "event": .text(event), "conversation_status": .text(conversation?.status ?? ""),
            "provider_name": .text(conversation?.provider.name ?? ""),
            "order_summary": .text(
                "\(conversation?.order.category ?? "") #\(conversation?.order.number ?? "") · \(conversation?.order.statusLabel ?? "")"
            ),
            "message_bodies": .strings(
                chatMessages.map { message in
                    message.body ?? ""
                }),
            "message_kinds": .strings(
                chatMessages.map { message in
                    let direction = message.isMine ? "sent" : "received"
                    let kind = message.wasMasked ? "blocked" : (message.media.isEmpty ? "text" : "image")
                    return "\(direction)_\(kind)"
                }),
            "message_times": .strings(chatMessages.map { displayDateTime($0.createdAt) }),
            "draft": .text(chatDraft),
        ]
    }

    private func orderHistoryInput(
        _ order: CustomerOrderSummary, proposals: [CustomerProposal]
    ) -> [String: CustomerInputValue] {
        var summary = [
            "#\(order.number ?? "")", order.category?.name ?? "", order.problemType?.name ?? "",
            order.location?.area ?? "", displayDateTime(order.timing?.slotStart ?? order.createdAt),
        ]
        if order.status == "CLOSED" {
            summary += [
                formatAmount(order.amounts?.laborTotal ?? "0.00"),
                formatAmount(order.amounts?.materialsTotal ?? "0.00"),
                formatAmount(order.amounts?.finalAmount ?? "0.00"),
                order.amounts?.paymentMethod ?? "", order.amounts?.paymentStatus ?? "",
            ]
        }
        return [
            "event": .text("loaded"), "status": .text(order.status ?? ""),
            "display_status": .text(order.statusLabel ?? order.displayStatus),
            "summary": .strings(summary),
            "provider_name": .text(order.provider?.name ?? ""),
            "termination_reason": .text(order.termination?.note ?? order.termination?.reasonLabel ?? ""),
            "proposal_summaries": .strings(
                proposals.map {
                    "\($0.typeLabel) · \(formatAmount($0.amount))"
                }),
            "available_actions": .strings(order.availableActions),
        ]
    }

    private func assignmentInput(_ order: CustomerOrderSummary) -> [String: CustomerInputValue] {
        [
            "event": .text("loaded"),
            "order_title": .text(
                "\(order.category?.name ?? "") — \(order.problemType?.name ?? "")"),
            "order_rows": .strings(assignmentRows(order)),
            "available_actions": .strings(order.availableActions),
        ]
    }

    /// C36: number, area, and the timing (now, or the chosen slot) — the same rows as Android.
    private func assignmentRows(_ order: CustomerOrderSummary) -> [String] {
        let slot = order.timing?.slotStart ?? ""
        let timing =
            slot.isEmpty
            ? (options("timing_types").first { $0.code == order.timing?.type }?.label ?? "")
            : displayDateTime(slot)
        return ["#\(order.number ?? "")", order.location?.area ?? "", timing].filter { !$0.isEmpty && $0 != "#" }
    }

    private func offerSortCode() -> String {
        ["rating", "price", "eta"][safe: offersSortIndex] ?? "rating"
    }

    private func offersInput(_ order: CustomerOrderSummary) -> [String: CustomerInputValue] {
        let pricingMode = (order.pricingMode ?? "").lowercased()
        let timingType = (order.timing?.type ?? "").lowercased()
        return [
            "event": .text("loaded"),
            "order_title": .text(
                "\(order.category?.name ?? "") — \(order.problemType?.name ?? "") · #\(order.number ?? "")"),
            "display_status": .text(order.displayStatus),
            "countdown_seconds": .integer(deadlineSeconds(order.deadlines)),
            "sort_index": .integer(offersSortIndex),
            "offer_rows": .strings(
                currentOffers.map { offerRow($0, order: order) }),
            "pricing_mode": .text(pricingMode), "timing_type": .text(timingType),
            "available_actions": .strings(order.availableActions),
        ]
    }

    private func offerRow(_ offer: CustomerOffer, order: CustomerOrderSummary) -> String {
        let detail =
            order.timing?.type == "NOW"
            ? offer.etaMinutes.map(String.init) ?? ""
            : displayDateTime(order.timing?.slotStart)
        return [
            String(offer.id), offer.provider.name, offer.provider.ratingAverage ?? "",
            offer.provider.completedOrders.map(String.init) ?? "", formatAmount(offer.price), detail,
            "", String(offer.provider.isVerified), String(offer.provider.id),
        ].joined(separator: "|")
    }

    private func offerDetailsInput(
        _ offer: CustomerOffer, event: String
    ) -> [String: CustomerInputValue] {
        guard let order = currentOrder else { return ["event": .text("error")] }
        let eta = offer.etaMinutes.map(String.init) ?? ""
        let timingType = (order.timing?.type ?? "").lowercased()
        let detail = !eta.isEmpty ? eta : displayDateTime(order.timing?.slotStart)
        let included = offer.includesText ?? (offer.inspectionFeeDeductible ? "deductible" : offer.note ?? "")
        return [
            "event": .text(event), "provider_name": .text(offer.provider.name),
            "provider_rating": .text(offer.provider.ratingAverage ?? ""),
            "provider_services": .text(offer.provider.completedOrders.map(String.init) ?? ""),
            "provider_verified": .bool(offer.provider.isVerified),
            "order_rows": .strings([
                "\(order.category?.name ?? "") — \(order.problemType?.name ?? "")",
                [order.location?.area ?? "", order.location?.city ?? ""].filter { !$0.isEmpty }
                    .joined(separator: " · "),
            ]),
            "payment_labels": .strings(options("payment_methods").map(\.label)),
            "selected_payment_index": .integer(selectedPaymentChannel),
            "available_actions": .strings(order.availableActions.filter { $0 == "accept_offer" }),
            "pricing_mode": .text((order.pricingMode ?? "").lowercased()),
            "timing_type": .text(timingType), "eta_minutes": .text(eta),
            "offer_rows": .strings([formatAmount(offer.price), detail, included]),
        ]
    }

    private func deadlineSeconds(_ deadlines: CustomerOrderDeadlines?) -> Int {
        guard let value = deadlines?.offersCloseAt ?? deadlines?.selectionDeadlineAt,
            let date = ISO8601DateFormatter().date(from: value)
        else { return 0 }
        return max(0, Int(date.timeIntervalSinceNow))
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

    private func trackingInput(_ order: CustomerOrderSummary) -> [String: CustomerInputValue] {
        [
            "event": .text("loaded"), "status": .text(order.status ?? ""),
            "display_status": .text(order.displayStatus), "stepper": .bool(order.stepper != nil),
            "available_actions": .strings(order.availableActions),
            "step_states": .strings(order.stepper?.map(\.state) ?? []),
            "summary": .strings(["#\(order.number ?? "")", order.location?.area ?? ""]),
        ]
    }

    private func paymentSummaryInput(
        _ order: CustomerOrderSummary, event: String
    ) -> [String: CustomerInputValue] {
        let amounts = order.amounts
        return [
            "event": .text(event),
            "amounts": .strings([
                formatAmount(amounts?.laborTotal ?? "0.00"),
                formatAmount(amounts?.materialsTotal ?? "0.00"),
                formatAmount(amounts?.finalAmount ?? "0.00"),
            ]),
            "method_labels": .strings(options("payment_methods").map(\.label)),
            "selected_method_index": .integer(
                options("payment_methods").firstIndex { $0.code == amounts?.paymentMethod } ?? -1),
            "payment_method": .text(amounts?.paymentMethod ?? ""),
            "available_actions": .strings(order.availableActions),
        ]
    }

    private func disputeInput(
        event: String, dispute: CustomerDispute? = nil
    ) -> [String: CustomerInputValue] {
        [
            "event": .text(event), "existing": .bool(dispute != nil),
            "reason_labels": .strings(options("dispute_reasons").map(\.label)),
            "selected_reason_index": .integer(supportReasonIndex),
            "description": .text(dispute?.description ?? supportDescription),
            "photo_count": .integer(dispute?.attachments.count ?? disputeMedia.count),
            "details": .strings(dispute.map { [$0.reasonLabel, $0.description] } ?? []),
            "status_details": .strings(dispute.map { [$0.statusLabel, $0.resolutionNote ?? ""] } ?? []),
            "available_actions": .strings(dispute == nil ? currentOrder?.availableActions ?? [] : []),
        ]
    }

    private func providerReportInput(event: String) -> [String: CustomerInputValue] {
        [
            "event": .text(event),
            "reason_labels": .strings(options("provider_report_reasons").map(\.label)),
            "selected_reason_index": .integer(supportReasonIndex),
            "description": .text(supportDescription),
            "available_actions": .strings(event == "success" ? [] : currentProviderActions),
        ]
    }

    private func helpInput(event: String) -> [String: CustomerInputValue] {
        [
            "event": .text(event), "faq_questions": .strings(faqQuestions),
            "faq_answers": .strings(faqAnswers),
            "expanded_index": .integer(expandedFaqIndex), "subject": .text(helpSubject),
            "message": .text(helpMessage),
        ]
    }

    private func notificationInput(
        _ values: [CustomerNotification], event: String
    ) -> [String: CustomerInputValue] {
        [
            "event": .text(event), "notification_titles": .strings(values.map(\.title)),
            "notification_bodies": .strings(values.map(\.body)),
            "notification_states": .strings(values.map { $0.readAt == nil ? "unread" : "read" }),
            "deep_links": .strings(notificationLinks),
            "notification_times": .strings(values.map { displayDateTime($0.createdAt) }),
            "notifications_denied": .bool(!notificationsAllowed()),
        ]
    }

    private func accountInput(event: String, mode: String? = nil) -> [String: CustomerInputValue] {
        [
            "event": .text(event), "mode": .text(mode ?? accountMode),
            "field_values": .strings(accountValues),
            "available_actions": .strings(accountActions),
            "rating_reminders_enabled": .bool(ratingRemindersEnabled),
        ]
    }

    private func submitAccountUpdate() async {
        guard state.canContinue else { return }
        state = CustomerLogic.reduce(screen: "SCR-C33", input: accountInput(event: "submitting"))
        do {
            let account = try await api.updateAccount(name: accountValues[0], phone: accountValues[2])
            accountValues = [account.name, account.email, account.phone]
            accountMode = "overview"
            state = CustomerLogic.reduce(screen: "SCR-C33", input: accountInput(event: "success"))
        } catch { state = CustomerLogic.reduce(screen: "SCR-C33", input: ["event": .text("error")]) }
    }

    private func submitPasswordChange() async {
        guard state.canContinue else { return }
        state = CustomerLogic.reduce(screen: "SCR-C33", input: accountInput(event: "submitting"))
        do {
            try await api.changePassword(
                current: accountValues[0], password: accountValues[1], confirmation: accountValues[2])
            accountMode = "overview"
            accountValues = ["", "", ""]
            state = CustomerLogic.reduce(screen: "SCR-C33", input: accountInput(event: "success"))
        } catch { state = CustomerLogic.reduce(screen: "SCR-C33", input: ["event": .text("error")]) }
    }

    private func submitDeleteAccount() async {
        state = CustomerLogic.reduce(
            screen: "SCR-C33", input: accountInput(event: "submitting", mode: "delete"))
        do {
            try await api.deleteAccount()
            accountDeleted = true
            state = CustomerLogic.reduce(
                screen: "SCR-C33", input: accountInput(event: "success", mode: "overview"))
        } catch APIClientError.httpStatus(409) {
            accountMode = "overview"
            state = CustomerLogic.reduce(screen: "SCR-C33", input: accountInput(event: "active_order"))
        } catch { state = CustomerLogic.reduce(screen: "SCR-C33", input: ["event": .text("error")]) }
    }

    private func options(_ key: String) -> [CustomerOption] { optionLists[key] ?? [] }

    private func paymentInput(
        _ order: CustomerOrderSummary, payment: CustomerPayment?, event: String
    ) -> [String: CustomerInputValue] {
        let seconds =
            payment?.expiresAt.flatMap { ISO8601DateFormatter().date(from: $0) }
            .map { max(0, Int($0.timeIntervalSinceNow)) } ?? 0
        return [
            "event": .text(event),
            "total": .text(formatAmount(order.amounts?.finalAmount ?? "0.00")),
            "channel_labels": .strings(options("payment_channels").map(\.label)),
            "selected_channel_index": .integer(selectedPaymentChannel),
            "payment_status": .text(payment?.status ?? ""),
            "reference_number": .text(payment?.referenceNumber ?? ""),
            "checkout_url": .text(payment?.checkoutURL ?? ""),
            "countdown_seconds": .integer(seconds),
            "available_actions": .strings(order.availableActions),
        ]
    }

    private func completionInput(
        _ order: CustomerOrderSummary, event: String
    ) -> [String: CustomerInputValue] {
        let amounts = order.amounts
        return [
            "event": .text(event),
            "summary": .strings([
                order.provider?.name ?? "", formatAmount(amounts?.laborTotal ?? "0.00"),
                formatAmount(amounts?.materialsTotal ?? "0.00"),
                formatAmount(amounts?.finalAmount ?? "0.00"), amounts?.paymentMethod ?? "",
            ]),
            "available_actions": .strings(order.availableActions),
        ]
    }

    private func ratingInput(event: String) -> [String: CustomerInputValue] {
        [
            "event": .text(event), "ratings": .strings(ratingValues.map(String.init)),
            "comment": .text(ratingComment), "comment_length": .integer(ratingComment.count),
            "available_actions": .strings(event == "success" ? [] : ["rate"]),
        ]
    }

    private func proposalInput(
        order: CustomerOrderSummary, proposal: CustomerProposal, event: String
    ) -> [String: CustomerInputValue] {
        let seconds =
            proposal.expiresAt.flatMap { ISO8601DateFormatter().date(from: $0) }
            .map { max(0, Int($0.timeIntervalSinceNow)) } ?? 0
        return [
            "event": .text(event), "type_label": .text(proposal.typeLabel),
            "amount": .text(formatAmount(proposal.amount)),
            "reason": .text(proposal.reason),
            "total": .text(formatAmount(proposal.projectedTotal)),
            "countdown_seconds": .integer(seconds), "expired": .bool(seconds == 0),
            "has_photo": .bool(proposal.photoPath != nil),
            "available_actions": .strings(order.availableActions),
        ]
    }

    private func formatAmount(_ value: String) -> String {
        "\(value.split(separator: ".").first ?? "0") \(currencyLabel)"
    }

    private func proposalStateInput(event: String) -> [String: CustomerInputValue] {
        [
            "event": .text(event), "type_label": .text(state.items[safe: 0] ?? ""),
            "amount": .text(state.items[safe: 1] ?? ""), "reason": .text(state.items[safe: 2] ?? ""),
            "total": .text(state.items[safe: 3] ?? ""),
            "countdown_seconds": .integer(state.countdownSeconds),
            "has_photo": .bool(state.hasPhoto), "available_actions": .strings(state.visibleActions),
        ]
    }

    private func loadSlots(
        cityId: Int, dayIndex: Int, dayLabels: [String], morning: String, evening: String
    ) async {
        state = CustomerLogic.reduce(screen: "SCR-C16", input: ["event": .text("changing_day")])
        do {
            let date = dayDates[dayIndex]
            let slots = try await api.slots(cityId: cityId, date: date)
            slotStarts = slots.map(\.start)
            state = CustomerLogic.reduce(
                screen: "SCR-C16",
                input: [
                    "event": .text("loaded"),
                    "day_labels": .strings(CustomerLogic.slotDayLabels(isoDates: dayDates, names: dayLabels)),
                    "slot_labels": .strings(
                        slots.map { customerSlotLabel($0, morning: morning, evening: evening) }),
                    "selected_day_index": .integer(dayIndex), "selected_slot_index": .integer(-1),
                ])
        } catch {
            state = CustomerLogic.reduce(screen: "SCR-C16", input: ["event": .text("error")])
        }
    }

    private func addressInput(event: String, areaLabels: [String]) -> [String: CustomerInputValue] {
        [
            "event": .text(event), "label": .text(addressDraft.label),
            "area_id": .text(addressDraft.areaId.map { String($0) } ?? ""),
            "area_served": .bool(addressDraft.areaId != nil),
            "address_text": .text(addressDraft.addressText),
            "building": .text(addressDraft.building), "floor": .text(addressDraft.floor),
            "apartment": .text(addressDraft.apartment), "landmark": .text(addressDraft.landmark),
            "lat": .text(addressDraft.latitude.map { String($0) } ?? ""),
            "lng": .text(addressDraft.longitude.map { String($0) } ?? ""),
            "is_default": .bool(addressDraft.isDefault), "area_labels": .strings(areaLabels),
            "editing": .bool(addressDraft.id != nil),
            "selected_area_index": .integer(addressDraft.selectedAreaIndex),
        ]
    }

    private func mediaInput() -> [String: CustomerInputValue] {
        [
            "event": .text("validate"), "media_kinds": .strings(mediaDrafts.map(\.kind)),
            "media_states": .strings(mediaDrafts.map(\.state)),
            "photo_count": .integer(mediaDrafts.count { $0.kind == "photo" }),
            "video_count": .integer(mediaDrafts.count { $0.kind == "video" }),
            "audio_count": .integer(mediaDrafts.count { $0.kind == "audio" }),
            "recording": .bool(mediaRecording),
        ]
    }
}

private struct CustomerAddressDraft {
    var id: Int?
    var cityId: Int?
    var areaId: Int?
    var selectedAreaIndex = -1
    var label = ""
    var addressText = ""
    var building = ""
    var floor = ""
    var apartment = ""
    var landmark = ""
    var latitude: Double?
    var longitude: Double?
    var isDefault = false
}

private struct CustomerMediaDraft {
    let token: UUID
    let id: Int?
    let kind: String
    let state: String
    let upload: CustomerMediaUpload?

    init(
        token: UUID = UUID(), id: Int?, kind: String, state: String,
        upload: CustomerMediaUpload? = nil
    ) {
        self.token = token
        self.id = id
        self.kind = kind
        self.state = state
        self.upload = upload
    }
}

extension String {
    fileprivate var nilIfEmpty: String? { isEmpty ? nil : self }
}

extension Array {
    fileprivate subscript(safe index: Index) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}

private func customerDayDates() -> [String] {
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "en_US_POSIX")
    formatter.dateFormat = "yyyy-MM-dd"
    return (0...7).compactMap {
        Calendar.current.date(byAdding: .day, value: $0, to: Date()).map(formatter.string)
    }
}

private func customerSlotLabel(_ slot: CustomerSlot, morning: String, evening: String) -> String {
    let parser = ISO8601DateFormatter()
    guard let start = parser.date(from: slot.start), let end = parser.date(from: slot.end) else {
        return "\(slot.start)–\(slot.end)"
    }
    let zone = TimeZone(identifier: "Africa/Cairo") ?? .current
    return
        "\(BzrFormatter.time(start, timeZone: zone, morning: morning, evening: evening))–\(BzrFormatter.time(end, timeZone: zone, morning: morning, evening: evening))"
}

/// C04 address row: the saved label and area, as the C14 row shows them.
func customerAddressLabel(_ address: CustomerAddress) -> String {
    [address.label, address.area.name].filter { !$0.isEmpty }.joined(separator: " · ")
}
