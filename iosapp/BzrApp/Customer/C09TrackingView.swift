import BzrCore
import CoreLocation
import DesignSystem
import SwiftUI

/// SCR-C09 — Figma «Primo App» tracking (DEC-076), one layout per status: CONFIRMED 3002:1631, ON_THE_WAY 3002:1903,
/// ARRIVED 3002:2055, IN_PROGRESS 3002:2540; other statuses keep the status card, stepper and actions.
/// `fieldValues`: number, address, problem, agreed price, materials, provider name, rating, completed, category,
/// approved total, slot; `optionCodes`: status, timing type.
public struct C09TrackingView: View {
    let state: CustomerUIState
    let onAction: (String) -> Void
    let tracking: CustomerTrackingPayload?
    let destinationLatitude: Double?
    let destinationLongitude: Double?
    let onRefreshTracking: () async -> Void

    private static let number = 0, address = 1, title = 2, price = 3, materials = 4, name = 5, rating = 6
    private static let completed = 7, category = 8, total = 9, slot = 10
    private static let metersPerKm = 1000.0

    public init(
        state: CustomerUIState,
        tracking: CustomerTrackingPayload? = nil,
        destinationLatitude: Double? = nil,
        destinationLongitude: Double? = nil,
        onRefreshTracking: @escaping () async -> Void = {},
        onAction: @escaping (String) -> Void = { _ in }
    ) {
        self.state = state
        self.tracking = tracking
        self.destinationLatitude = destinationLatitude
        self.destinationLongitude = destinationLongitude
        self.onRefreshTracking = onRefreshTracking
        self.onAction = onAction
    }

    private var status: String { state.optionCodes.first ?? "" }
    private var isNow: Bool { state.optionCodes.count > 1 && state.optionCodes[1] == "NOW" }
    private func value(_ index: Int) -> String { state.fieldValues.indices.contains(index) ? state.fieldValues[index] : "" }
    private var firstName: String { value(Self.name).components(separatedBy: " ").first ?? "" }
    private func amount(_ text: String) -> String {
        BzrFormatter.fill(bzrString("customer.home.offers.amount"), values: [("amount", text)])
    }

    private var screenTitle: String {
        switch status {
        case "CONFIRMED": bzrString("tracking.title.confirmed")
        case "ON_THE_WAY": bzrString("tracking.step.on_the_way")
        case "ARRIVED": bzrString("tracking.title.arrived")
        case "IN_PROGRESS": bzrString("tracking.step.in_progress")
        default: bzrString("tracking.title")
        }
    }

    /// Actions each layout shows in a card or the dock, so the action list does not repeat them.
    private var shown: Set<String> {
        switch status {
        case "CONFIRMED": ["call", "chat"]
        case "ON_THE_WAY": ["call", "chat", "share_visit"]
        case "ARRIVED": ["call", "chat", "report_provider", "open_dispute"]
        case "IN_PROGRESS": ["call", "chat", "report_provider", "open_dispute"]
        default: []
        }
    }

    public var body: some View {
        CustomerScreen(title: screenTitle, state: state, bottomBar: AnyView(dock)) {
            if state.arrivalCodeRequired {
                VStack(alignment: .leading, spacing: DesignSpace.m) {
                    BzrText(bzrString("arrival.code.title"), style: DesignType.homeSection)
                    BzrText(bzrString("arrival.code.body"), style: DesignType.secondary)
                    if !state.arrivalCode.isEmpty {
                        BzrText(state.arrivalCode, style: DesignType.amountDisplay).environment(\.layoutDirection, .leftToRight)
                        BzrText(BzrFormatter.fill(bzrString("arrival.code.until"), values: [("time", state.arrivalCodeUntil)]), style: DesignType.caption)
                    }
                    if state.arrivalCodeFailed { WarningBox(text: bzrString("arrival.code.failed")) }
                    SecondaryButton(text: bzrString("arrival.code.refresh"), onClick: { onAction("refresh_arrival_code") })
                    if state.arrivalExceptionPending { InfoBanner(text: bzrString("arrival.exception.pending")) }
                }
                .padding(DesignSpace.l)
                .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
            }

            if state.isBusy { InfoBanner(text: bzrString("share.loading")) }
            if state.messageKey == "share.error" { InfoBanner(text: bzrString("share.error"), isError: true) }
            if !state.warrantyCoverage.isEmpty { InfoBanner(text: state.warrantyCoverage) }
            if state.phase == .offline { OfflineBanner(text: bzrString("offline.message")) }
            switch status {
            case "CONFIRMED": confirmed
            case "ON_THE_WAY": onTheWay
            case "ARRIVED": arrived
            case "IN_PROGRESS": inProgress
            default: generic
            }
            ActionButtons(actions: state.visibleActions.filter { !shown.contains($0) }, onAction: onAction)
        }
        .task(id: state.showMap) {
            while state.showMap && !Task.isCancelled {
                await onRefreshTracking()
                try? await Task.sleep(for: .seconds(30))
            }
        }
    }

    // ── CONFIRMED (3002:1631) ──
    @ViewBuilder private var confirmed: some View {
        hero(
            .check, pill: nil, heading: BzrFormatter.fill(bzrString("tracking.confirmed.heading"), values: [("name", firstName)]),
            body: bzrString("tracking.confirmed.body"))
        card {
            providerHeader()
            let eta = isNow ? "" : value(Self.slot)
            if !eta.isEmpty { inset(.clock, BzrFormatter.fill(bzrString("tracking.expected"), values: [("eta", eta)])) }
            contactRow(callFirst: false)
        }
        serviceCard
        stepperCard(
            bzrString("tracking.steps"), badge: nil,
            labels: ["tracking.step.confirmed", "tracking.step.on_the_way", "tracking.step.arrived", "tracking.step.in_progress"].map(bzrString),
            subtitles: ["tracking.step.confirmed.body", "tracking.step.next", "tracking.step.arrived.body", "tracking.step.in_progress.body"].map(bzrString),
            states: [.done, .pending, .pending, .pending])
    }

    // ── ON_THE_WAY (3002:1903) ──
    @ViewBuilder private var onTheWay: some View {
        ZStack(alignment: .top) {
            NativeMapView(
                providerCoordinate: coordinate(tracking?.lastLocation?.latitude, tracking?.lastLocation?.longitude),
                destinationCoordinate: coordinate(destinationLatitude, destinationLongitude)
            )
            .frame(height: DesignSize.mapCardHeight)
            .clipShape(RoundedRectangle(cornerRadius: DesignRadius.card))
            .accessibilityLabel(bzrString("map.title"))
            VStack(alignment: .leading, spacing: 0) {
                BzrText(bzrString("tracking.eta.title"), style: DesignType.hint)
                BzrText(
                    tracking?.etaMinutes.map { bzrPlural("offer.eta", count: $0) } ?? "—",
                    style: DesignType.screenTitle.colored(DesignColors.primary600))
                if let km = distanceKm {
                    BzrText(BzrFormatter.fill(bzrString("tracking.distance"), values: [("km", BzrFormat.number(km))]), style: DesignType.hint)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(DesignSpace.m)
            .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
            .bzrShadow(DesignShadows.card)
            .padding(DesignSpace.m)
        }
        card {
            providerHeader()
            contactRow(callFirst: true)
            inset(
                .navigation, BzrFormatter.fill(bzrString("tracking.moving.title"), values: [("name", firstName)]),
                body: bzrString("tracking.moving.body"))
            labeledRow(.location, "tracking.visit_location", value(Self.address))
            labeledRow(.wrench, "tracking.requested_service", value(Self.title))
        }
    }

    // ── ARRIVED (3002:2055) ──
    @ViewBuilder private var arrived: some View {
        hero(
            .account, pill: bzrString("tracking.arrived.pill"),
            heading: BzrFormatter.fill(bzrString("tracking.arrived.heading"), values: [("name", value(Self.name))]),
            body: bzrString("tracking.arrived.body"))
        // The Figma arrival code and «تأكيد وصول الفني» have no API counterpart (BACKEND-NOTES B-22); the card keeps the identity.
        card { providerHeader() }
        card {
            HStack {
                BzrText(bzrString("tracking.order_details"), style: DesignType.caption)
                Spacer(minLength: DesignSpace.s)
                pill(BzrFormatter.fill(bzrString(isNow ? "tracking.kind.now" : "tracking.kind.scheduled"), values: [("number", value(Self.number))]))
            }
            iconTitle(.wrench, value(Self.title))
            priceInset
        }
        HStack(spacing: DesignSpace.s) {
            if state.visibleActions.contains("chat") {
                SecondaryButton(text: bzrString("tracking.quick_chat"), icon: .messages, onClick: { onAction("chat") })
            }
            if state.visibleActions.contains("call") {
                SecondaryButton(text: bzrString("tracking.call_provider"), icon: .phone, onClick: { onAction("call") })
            }
        }
    }

    // ── IN_PROGRESS (3002:2540) ──
    @ViewBuilder private var inProgress: some View {
        HStack(spacing: DesignSpace.s) {
            Circle().fill(DesignColors.primary600).frame(width: DesignSize.statusDot, height: DesignSize.statusDot)
            BzrText(BzrFormatter.fill(bzrString("tracking.working.pill"), values: [("name", firstName)]), style: DesignType.statusPill)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, DesignSpace.m)
        .padding(.vertical, DesignSpace.s)
        .background(Capsule().fill(DesignColors.statusPillBg))
        card {
            providerHeader(compact: true)
            contactRow(callFirst: false)
        }
        if state.fieldValues.indices.contains(11), !state.fieldValues[11].isEmpty {
            inset(
                .clock, BzrFormatter.fill(bzrString("tracking.finish.estimated_at"), values: [("time", state.fieldValues[11])]),
                body: bzrString(state.fieldValues.indices.contains(12) && state.fieldValues[12] == "overdue" ? "tracking.finish.overdue" : "tracking.finish.from_technician"))
        }
        if !state.itemDetails.isEmpty {
            let badge =
                state.itemStates.firstIndex(of: "active").map {
                    BzrFormatter.fill(bzrString("tracking.stage"), values: [("current", BzrFormat.number($0 + 1)), ("total", BzrFormat.number(state.itemDetails.count))])
                } ?? ""
            stepperCard(
                bzrString("tracking.exec_steps"), badge: badge, labels: state.itemDetails, subtitles: state.options,
                states: state.itemStates.map {
                    switch $0 {
                    case "done": .done;
                    case "active": .active;
                    default: .pending
                    }
                })
        }
        card {
            BzrText(bzrString("tracking.current_details"), style: DesignType.homeSection)
            inset(nil, bzrString("tracking.requested_service"), body: value(Self.title))
            let approved = value(Self.total).isEmpty ? value(Self.price) : value(Self.total)
            if !approved.isEmpty {
                HStack {
                    VStack(alignment: .leading, spacing: 0) {
                        BzrText(bzrString("tracking.total"), style: DesignType.tileTitle)
                        BzrText(bzrString("tracking.total_note"), style: DesignType.hint)
                    }
                    Spacer(minLength: DesignSpace.s)
                    BzrText(amount(approved), style: DesignType.offerPrice)
                }
            }
        }
    }

    // ── Other statuses ──
    @ViewBuilder private var generic: some View {
        if !state.displayStatus.isEmpty { pill(localizedCustomerDisplayStatus(state.displayStatus)) }
        if state.showStatusCard { InfoBanner(text: bzrString(state.messageKey ?? "tracking.status.cancelled")) }
        if state.messageKey == "status.reviewing" { WarningBox(text: bzrString("status.reviewing")) }
        if state.showStepper {
            let keys = ["status.confirmed", "status.on_the_way", "status.arrived", "status.in_progress", "status.payment", "status.closed"]
            let fallback = ["done", "active", "pending", "pending", "pending", "pending"]
            let states = state.stepStates.isEmpty ? fallback : state.stepStates
            stepperCard(
                bzrString("tracking.steps"), badge: nil, labels: keys.map(bzrString), subtitles: [],
                states: keys.indices.map { states.indices.contains($0) ? stepState(states[$0]) : .pending })
        }
        if !value(Self.title).isEmpty { serviceCard }
    }

    // ── Dock ──
    @ViewBuilder private var dock: some View {
        VStack(spacing: DesignSpace.xs) {
            switch status {
            case "CONFIRMED":
                PrimaryButton(text: bzrString("tracking.follow"), state: .normal, onClick: { onAction("refresh") })
            case "ON_THE_WAY":
                PrimaryButton(text: bzrString("tracking.follow_trip"), state: .normal, onClick: { onAction("refresh") })
                HStack(spacing: DesignSpace.m) {
                    if state.visibleActions.contains("share_visit") { link(.share, bzrString("action.share_visit"), "share_visit") }
                    link(.info, bzrString("tracking.help"), "help")
                }
            case "ARRIVED":
                reportLink(bzrString("tracking.report_mismatch"))
            case "IN_PROGRESS":
                PrimaryButton(text: bzrString("tracking.follow_service"), state: .normal, onClick: { onAction("refresh") })
                reportLink(bzrString("tracking.report"))
            default:
                EmptyView()
            }
        }
    }

    // ── Building blocks ──
    private var distanceKm: Double? {
        guard let latitude = tracking?.lastLocation?.latitude, let longitude = tracking?.lastLocation?.longitude,
            let destinationLatitude, let destinationLongitude
        else { return nil }
        // DEC-076: computed from the technician's last position and the address.
        let meters = CLLocation(latitude: latitude, longitude: longitude)
            .distance(from: CLLocation(latitude: destinationLatitude, longitude: destinationLongitude))
        return (meters / Self.metersPerKm * 10).rounded() / 10
    }

    private func hero(_ icon: BzrIconKey, pill pillText: String?, heading: String, body: String) -> some View {
        VStack(spacing: DesignSpace.s) {
            ZStack {
                Circle().fill(DesignColors.successTint)
                Circle().fill(DesignColors.success).frame(width: DesignSize.successCore, height: DesignSize.successCore)
                BzrIcon(icon, tint: DesignColors.onPrimary)
            }
            .frame(width: DesignSize.successRing, height: DesignSize.successRing)
            if let pillText { pill(pillText) }
            BzrText(heading, style: DesignType.screenTitle, alignment: .center)
            BzrText(body, style: DesignType.subtitle, alignment: .center)
        }
        .frame(maxWidth: .infinity)
    }

    private func providerHeader(compact: Bool = false) -> some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 0) {
                HStack(spacing: DesignSpace.xs) {
                    BzrText(value(Self.name), style: DesignType.offerName)
                    if state.providerVerified { BzrIcon(.verified, tint: DesignColors.verifiedText, size: DesignSize.iconSmall) }
                }
                let meta = [
                    state.providerVerified && !value(Self.category).isEmpty
                        ? BzrFormatter.fill(bzrString("provider.verified_title"), values: [("category", value(Self.category))]) : "",
                    compact || value(Self.completed).isEmpty
                        ? ""
                        : bzrPlural("confirm.completed", count: Int(value(Self.completed)) ?? 0),
                ].filter { !$0.isEmpty }.joined(separator: " • ")
                if !meta.isEmpty { BzrText(meta, style: DesignType.hint.colored(DesignColors.accentText)) }
            }
            Spacer(minLength: DesignSpace.s)
            if !value(Self.rating).isEmpty {
                HStack(spacing: DesignSpace.xs) {
                    BzrIcon(.starFilled, tint: DesignColors.star, size: DesignSize.iconSmall)
                    BzrText(value(Self.rating), style: DesignType.detailStrong)
                }
                .padding(.horizontal, DesignSpace.s)
                .background(Capsule().fill(DesignColors.surfaceAlt2))
            }
        }
    }

    @ViewBuilder private func contactRow(callFirst: Bool) -> some View {
        HStack(spacing: DesignSpace.s) {
            ForEach(callFirst ? ["call", "chat"] : ["chat", "call"], id: \.self) { action in
                if state.visibleActions.contains(action) {
                    let label = bzrString(action == "call" ? "tracking.call" : "tracking.message")
                    if callFirst && action == "call" {
                        PrimaryButton(
                            text: label, state: .normal, height: DesignSize.secondaryButtonHeight, icon: .phone, compact: true,
                            onClick: { onAction(action) })
                    } else {
                        SecondaryButton(text: label, icon: action == "call" ? .phone : .chat, onClick: { onAction(action) })
                    }
                }
            }
        }
    }

    private var serviceCard: some View {
        card {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 0) {
                    BzrText(bzrString("tracking.service_details"), style: DesignType.hint)
                    BzrText(value(Self.title), style: DesignType.tileTitle)
                }
                Spacer(minLength: DesignSpace.s)
                if !value(Self.price).isEmpty {
                    VStack(alignment: .trailing, spacing: 0) {
                        BzrText(amount(value(Self.price)), style: DesignType.offerPrice)
                        if !value(Self.materials).isEmpty { BzrText(value(Self.materials), style: DesignType.hint) }
                    }
                }
            }
            if !value(Self.address).isEmpty {
                HStack(spacing: DesignSpace.xs) {
                    BzrIcon(.location, tint: DesignColors.primary600, size: DesignSize.iconSmall)
                    BzrText(value(Self.address), style: DesignType.caption)
                }
            }
        }
    }

    private func stepperCard(_ title: String, badge: String?, labels: [String], subtitles: [String], states: [StepState]) -> some View {
        card {
            HStack {
                BzrText(title, style: DesignType.homeSection)
                Spacer(minLength: DesignSpace.s)
                if let badge { pill(badge) }
            }
            StatusStepper(steps: Array(zip(labels, states)), subtitles: subtitles, vertical: true)
        }
    }

    private var priceInset: some View {
        HStack(spacing: DesignSpace.s) {
            BzrText(bzrString("tracking.agreed"), style: DesignType.caption)
            Spacer(minLength: 0)
            BzrText(amount(value(Self.price)), style: DesignType.amountSmall.colored(DesignColors.amountText))
            if !value(Self.materials).isEmpty { BzrText(value(Self.materials), style: DesignType.hint) }
        }
        .padding(.horizontal, DesignSpace.m)
        .padding(.vertical, DesignSpace.s)
        .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.surfaceAlt2))
    }

    @ViewBuilder private func reportLink(_ label: String) -> some View {
        if let action = ["report_provider", "open_dispute"].first(where: state.visibleActions.contains) {
            link(.warning, label, action, danger: true)
        }
    }

    private func link(_ icon: BzrIconKey, _ label: String, _ action: String, danger: Bool = false) -> some View {
        Button(action: { onAction(action) }) {
            HStack(spacing: DesignSpace.xs) {
                BzrIcon(icon, tint: danger ? DesignColors.danger : DesignColors.slate500, size: DesignSize.iconSmall)
                BzrText(label, style: DesignType.caption.colored(danger ? DesignColors.danger : DesignColors.slate500))
            }
            .frame(minHeight: DesignSize.touchTargetMin)
        }
        .buttonStyle(BzrPressStyle())
    }

    private func inset(_ icon: BzrIconKey?, _ title: String, body: String = "") -> some View {
        HStack(spacing: DesignSpace.s) {
            VStack(alignment: .leading, spacing: 0) {
                BzrText(title, style: body.isEmpty && icon != nil ? DesignType.detailStrong : DesignType.hint)
                if !body.isEmpty { BzrText(body, style: DesignType.tileTitle) }
            }
            Spacer(minLength: 0)
            if let icon { BzrIcon(icon, tint: DesignColors.decorativeIcon) }
        }
        .padding(DesignSpace.m)
        .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.surfaceAlt2))
    }

    private func labeledRow(_ icon: BzrIconKey, _ label: String, _ text: String) -> some View {
        HStack(spacing: DesignSpace.s) {
            VStack(alignment: .leading, spacing: 0) {
                BzrText(bzrString(label), style: DesignType.hint)
                BzrText(text, style: DesignType.tileTitle)
            }
            Spacer(minLength: 0)
            BzrIcon(icon, tint: DesignColors.decorativeIcon, size: DesignSize.iconSmall)
                .frame(width: DesignSize.orderWell, height: DesignSize.orderWell)
                .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.surfaceContainer))
        }
    }

    private func iconTitle(_ icon: BzrIconKey, _ text: String) -> some View {
        HStack {
            BzrText(text, style: DesignType.homeSection)
            Spacer(minLength: DesignSpace.s)
            BzrIcon(icon, tint: DesignColors.decorativeIcon, size: DesignSize.iconSmall)
                .frame(width: DesignSize.orderWell, height: DesignSize.orderWell)
                .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.surfaceContainer))
        }
    }

    private func pill(_ text: String) -> some View {
        BzrText(text, style: DesignType.statusPill)
            .padding(.horizontal, DesignSpace.s)
            .background(Capsule().fill(DesignColors.statusPillBg))
    }

    private func card<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: DesignSpace.m) { content() }
            .padding(DesignSpace.l)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
            .bzrShadow(DesignShadows.card)
    }

    private func coordinate(_ latitude: Double?, _ longitude: Double?) -> CLLocationCoordinate2D? {
        guard let latitude, let longitude else { return nil }
        return CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }
}

private func stepState(_ value: String) -> StepState {
    switch value {
    case "done": .done
    case "active": .active
    case "on_hold": .onHold
    default: .pending
    }
}

func localizedCustomerDisplayStatus(_ key: String) -> String {
    switch key {
    case "order.status.customer.OPEN",
        "order.status.customer.CONFIRMED",
        "order.status.customer.ON_THE_WAY",
        "order.status.customer.ARRIVED",
        "order.status.customer.AWAITING_QUOTE_APPROVAL",
        "order.status.customer.IN_PROGRESS",
        "order.status.customer.AWAITING_PAYMENT",
        "order.status.customer.AWAITING_TRANSFER_VERIFICATION",
        "order.status.customer.AWAITING_CONFIRMATION",
        "order.status.customer.DISPUTED",
        "order.status.customer.CLOSED",
        "order.status.customer.CANCELLED",
        "order.status.customer.EXPIRED":
        bzrString(key)
    default:
        // A status the app has no key for is shown as the server sent it, never as sample text.
        key
    }
}
