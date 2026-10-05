import BzrCore
import CoreLocation
import DesignSystem
import PhotosUI
import SwiftUI
import UIKit

private let providerStatusKeys = [
    "order.status.provider.OPEN": "order.status.provider.OPEN",
    "order.status.provider.CONFIRMED": "order.status.provider.CONFIRMED",
    "order.status.provider.ON_THE_WAY": "order.status.provider.ON_THE_WAY",
    "order.status.provider.ARRIVED": "order.status.provider.ARRIVED",
    "order.status.provider.AWAITING_QUOTE_APPROVAL": "order.status.provider.AWAITING_QUOTE_APPROVAL",
    "order.status.provider.IN_PROGRESS": "order.status.provider.IN_PROGRESS",
    "order.status.provider.AWAITING_PAYMENT": "order.status.provider.AWAITING_PAYMENT",
    "order.status.provider.AWAITING_TRANSFER_VERIFICATION":
        "order.status.provider.AWAITING_TRANSFER_VERIFICATION",
    "order.status.provider.AWAITING_CONFIRMATION": "order.status.provider.AWAITING_CONFIRMATION",
    "order.status.provider.DISPUTED": "order.status.provider.DISPUTED",
    "order.status.provider.CLOSED": "order.status.provider.CLOSED",
    "order.status.provider.CANCELLED": "order.status.provider.CANCELLED",
    "order.status.provider.EXPIRED": "order.status.provider.EXPIRED",
]

private struct ProviderScreen<Content: View>: View {
    let title: String
    let state: ProviderUIState
    let onBack: () -> Void
    let content: Content

    init(
        title: String, state: ProviderUIState, onBack: @escaping () -> Void = {},
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.state = state
        self.onBack = onBack
        self.content = content()
    }

    var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: title, onBack: onBack)
            ScrollView {
                VStack(alignment: .leading, spacing: DesignSpace.l) {
                    switch state.phase {
                    case .loading:
                        BremoBrandLoader()
                        ForEach(0..<2, id: \.self) { _ in LoadingSkeleton().accessibilityHidden(true) }
                    case .error:
                        ErrorState(
                            title: bzrString("error.title"), body: bzrString("error.body"),
                            retry: bzrString("common.cancel"), onRetry: onBack)
                    default: content
                    }
                }
                .frame(maxWidth: DesignSize.contentMaxWidth, alignment: .leading)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, DesignSpace.screenHorizontal)
                .padding(.top, DesignSpace.screenVertical)
                .padding(.bottom, DesignSpace.contentBottom)
            }
        }
        .background(DesignColors.surfaceAlt)
        .scrollDismissesKeyboard(.interactively)
    }
}

public struct P01IntroView: View {
    let state: ProviderUIState
    let onAction: (String) -> Void
    let onBack: () -> Void

    public var body: some View {
        ProviderScreen(
            title: bzrString("provider.onboarding.intro.title"), state: state, onBack: onBack
        ) {
            EmptyState(
                title: bzrString("provider.onboarding.intro.title"),
                body: bzrString("provider.onboarding.intro.body"))
            CheckRow(text: bzrString("provider.onboarding.benefit.jobs"), checked: true)
            CheckRow(text: bzrString("provider.onboarding.benefit.schedule"), checked: true)
            CheckRow(text: bzrString("provider.onboarding.benefit.support"), checked: true)
            if state.visibleActions.contains("start_provider_application") {
                PrimaryButton(text: bzrString("action.start_provider_application")) {
                    onAction("start_provider_application")
                }
            }
        }
    }
}

public struct P02ProfileView: View {
    let state: ProviderUIState
    let onExperienceChange: (String) -> Void
    let onBioChange: (String) -> Void
    let onUpload: (CustomerMediaUpload) async -> Void
    let onNext: () -> Void
    let onBack: () -> Void
    @State private var photo: PhotosPickerItem?
    @State private var showPhotoPicker = false

    public var body: some View {
        ProviderScreen(title: bzrString("provider.onboarding.data.title"), state: state, onBack: onBack) {
            BzrText(bzrString("provider.onboarding.step.1"), style: DesignType.caption)
            InfoBanner(
                text: bzrString(
                    state.hasProfilePhoto
                        ? "provider.onboarding.profile_photo.uploaded"
                        : "provider.onboarding.profile_photo.required"))
            SecondaryButton(
                text: bzrString("provider.onboarding.profile_photo.add"),
                onClick: { showPhotoPicker = true }
            )
            .photosPicker(isPresented: $showPhotoPicker, selection: $photo, matching: .images)
            .onChange(of: photo) { _, item in upload(item, name: "provider-profile.jpg") }
            AppTextField(
                label: bzrString("provider.onboarding.experience.label"), placeholder: "0",
                value: state.fieldValues.first ?? "",
                state: fieldState("provider.onboarding.experience.invalid"),
                error: error("provider.onboarding.experience.invalid"), onValueChange: onExperienceChange)
            TextAreaField(
                label: bzrString("provider.onboarding.bio.label"),
                placeholder: bzrString("provider.onboarding.bio.placeholder"),
                value: state.fieldValues.indices.contains(1) ? state.fieldValues[1] : "",
                state: fieldState("provider.onboarding.bio.max"),
                error: error("provider.onboarding.bio.max"), onValueChange: onBioChange)
            PrimaryButton(text: bzrString("common.next"), state: buttonState, onClick: onNext)
        }
    }

    private var buttonState: ButtonVisualState {
        state.isBusy ? .loading : (state.canContinue ? .normal : .disabled)
    }
    private func fieldState(_ key: String) -> FieldVisualState {
        state.fieldErrors.contains(key) ? .error : .filled
    }
    private func error(_ key: String) -> String? {
        state.fieldErrors.contains(key) ? bzrString(key) : nil
    }
    private func upload(_ item: PhotosPickerItem?, name: String) {
        Task {
            guard let source = try? await item?.loadTransferable(type: Data.self),
                let image = UIImage(data: source),
                let data = image.jpegData(compressionQuality: 0.9),
                data.count <= 10 * 1024 * 1024
            else { return }
            await onUpload(CustomerMediaUpload(fileName: name, mimeType: "image/jpeg", data: data))
        }
    }
}

public struct P03IdentityView: View {
    let state: ProviderUIState
    let onUpload: (Bool, CustomerMediaUpload) async -> Void
    let onNext: () -> Void
    let onBack: () -> Void
    @State private var front: PhotosPickerItem?
    @State private var back: PhotosPickerItem?
    @State private var showFrontPicker = false
    @State private var showBackPicker = false

    public var body: some View {
        ProviderScreen(
            title: bzrString("provider.onboarding.identity.title"), state: state, onBack: onBack
        ) {
            BzrText(bzrString("provider.onboarding.step.2"), style: DesignType.caption)
            InfoBanner(text: bzrString("provider.onboarding.identity.private"))
            document(
                title: "provider.onboarding.identity.front", uploaded: state.hasIDFront,
                selection: $front, isPresented: $showFrontPicker, isFront: true)
            document(
                title: "provider.onboarding.identity.back", uploaded: state.hasIDBack,
                selection: $back, isPresented: $showBackPicker, isFront: false)
            if state.messageKey != nil || !state.fieldErrors.isEmpty {
                WarningBox(text: bzrString(state.messageKey ?? "provider.onboarding.identity.required"))
            }
            PrimaryButton(
                text: bzrString("common.next"),
                state: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled), onClick: onNext)
        }
    }

    private func document(
        title: String, uploaded: Bool, selection: Binding<PhotosPickerItem?>,
        isPresented: Binding<Bool>, isFront: Bool
    ) -> some View {
        VStack(alignment: .leading, spacing: DesignSpace.s) {
            SummaryCard(
                title: bzrString(title),
                rows: uploaded ? [(.check, bzrString("provider.onboarding.identity.uploaded"))] : [],
                editText: nil)
            SecondaryButton(
                text: bzrString("provider.onboarding.identity.add"),
                onClick: { isPresented.wrappedValue = true }
            )
            .photosPicker(isPresented: isPresented, selection: selection, matching: .images)
            .onChange(of: selection.wrappedValue) { _, item in
                Task {
                    guard let source = try? await item?.loadTransferable(type: Data.self),
                        let image = UIImage(data: source),
                        let data = image.jpegData(compressionQuality: 0.9),
                        data.count <= 10 * 1024 * 1024
                    else { return }
                    await onUpload(
                        isFront,
                        CustomerMediaUpload(
                            fileName: isFront ? "id-front.jpg" : "id-back.jpg",
                            mimeType: "image/jpeg", data: data))
                }
            }
        }
    }
}

public struct P04CatalogView: View {
    let state: ProviderUIState
    let onCategory: (Int) -> Void
    let onSpecialty: (Int) -> Void
    let onNext: () -> Void
    let onBack: () -> Void

    public var body: some View {
        ProviderScreen(
            title: bzrString("provider.onboarding.categories.title"), state: state, onBack: onBack
        ) {
            BzrText(bzrString("provider.onboarding.step.3"), style: DesignType.caption)
            ForEach(Array(state.options.enumerated()), id: \.offset) { index, label in
                Button(
                    action: { onCategory(index) },
                    label: {
                        SelectableChip(
                            text: label,
                            state: state.selectedPrimaryIndices.contains(index) ? .selected : .unselected)
                    }
                ).buttonStyle(BzrPressStyle())
            }
            BzrText(bzrString("provider.onboarding.specialties.title"), style: DesignType.sectionTitle)
            ForEach(Array(state.secondaryOptions.enumerated()), id: \.offset) { index, label in
                Button(
                    action: { onSpecialty(index) },
                    label: {
                        CheckRow(text: label, checked: state.selectedSecondaryIndices.contains(index))
                    }
                ).buttonStyle(BzrPressStyle())
            }
            if state.phase == .empty || !state.fieldErrors.isEmpty {
                WarningBox(
                    text: bzrString(
                        state.phase == .empty
                            ? "provider.onboarding.catalog.empty"
                            : state.fieldErrors.first ?? "provider.onboarding.categories.required"))
            }
            PrimaryButton(
                text: bzrString("common.next"), state: state.canContinue ? .normal : .disabled,
                onClick: onNext)
        }
    }
}

public struct P05AreasView: View {
    let state: ProviderUIState
    let onCity: (Int) async -> Void
    let onArea: (Int) -> Void
    let onNext: () async -> Void
    let onBack: () -> Void

    public var body: some View {
        ProviderScreen(
            title: bzrString("provider.onboarding.areas.title"), state: state, onBack: onBack
        ) {
            BzrText(bzrString("provider.onboarding.step.4"), style: DesignType.caption)
            BzrText(bzrString("provider.onboarding.city.label"), style: DesignType.sectionTitle)
            ForEach(Array(state.options.enumerated()), id: \.offset) { index, label in
                Button(
                    action: { Task { await onCity(index) } },
                    label: {
                        RadioCard(
                            title: label, body: "", selected: state.selectedPrimaryIndices.contains(index))
                    }
                ).buttonStyle(BzrPressStyle())
            }
            ForEach(Array(state.secondaryOptions.enumerated()), id: \.offset) { index, label in
                Button(
                    action: { onArea(index) },
                    label: {
                        CheckRow(text: label, checked: state.selectedSecondaryIndices.contains(index))
                    }
                ).buttonStyle(BzrPressStyle())
            }
            if state.phase == .empty || !state.fieldErrors.isEmpty {
                WarningBox(
                    text: bzrString(
                        state.phase == .empty
                            ? "provider.onboarding.areas.empty"
                            : "provider.onboarding.areas.required"))
            }
            InfoBanner(text: bzrString("provider.onboarding.availability.note"))
            PrimaryButton(
                text: bzrString("common.next"), state: state.canContinue ? .normal : .disabled,
                onClick: { Task { await onNext() } })
        }
    }
}

public struct P06PayoutView: View {
    let state: ProviderUIState
    let onMethod: (Int) -> Void
    let onDetailsChange: (String) -> Void
    let onSubmit: () async -> Void
    let onBack: () -> Void

    public var body: some View {
        ProviderScreen(
            title: bzrString("provider.onboarding.payout.title"), state: state, onBack: onBack
        ) {
            BzrText(bzrString("provider.onboarding.step.5"), style: DesignType.caption)
            ForEach(Array(state.options.enumerated()), id: \.offset) { index, label in
                Button(
                    action: { onMethod(index) },
                    label: {
                        RadioCard(
                            title: label, body: "", selected: state.selectedPrimaryIndices.contains(index))
                    }
                ).buttonStyle(BzrPressStyle())
            }
            AppTextField(
                label: payoutLabel, placeholder: payoutLabel, value: state.fieldValues.first ?? "",
                state: detailsError ? .error : .filled,
                error: detailsError ? bzrString("provider.onboarding.payout.details.required") : nil,
                imeAction: .done, onValueChange: onDetailsChange)
            SummaryCard(title: bzrString("provider.onboarding.summary"), rows: summaryRows, editText: nil)
            InfoBanner(text: bzrString("provider.onboarding.review.note"))
            if state.messageKey != nil
                || state.fieldErrors.contains("provider.onboarding.payout.method.required")
            {
                WarningBox(
                    text: bzrString(state.messageKey ?? "provider.onboarding.payout.method.required"))
            }
            if state.visibleActions.contains("submit_provider_application") {
                PrimaryButton(
                    text: bzrString("action.submit_provider_application"),
                    state: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled),
                    onClick: { Task { await onSubmit() } })
            }
        }
    }

    private var detailsError: Bool {
        state.fieldErrors.contains("provider.onboarding.payout.details.required")
    }
    private var payoutLabel: String {
        guard let index = state.selectedPrimaryIndices.first,
            state.secondaryOptions.indices.contains(index)
        else { return "" }
        return state.secondaryOptions[index]
    }
    private var summaryRows: [(BzrIconKey, String)] {
        let keys = [
            "provider.onboarding.summary.categories", "provider.onboarding.summary.specialties",
            "provider.onboarding.summary.areas", "provider.onboarding.summary.documents",
        ]
        let icons: [BzrIconKey] = [.orders, .check, .location, .camera]
        return state.items.enumerated().map { index, value in
            (icons[index], String(format: bzrString(keys[index]), value))
        }
    }
}

public struct P07StatusView: View {
    let state: ProviderUIState
    let onAction: (String) -> Void
    let onBack: () -> Void

    public var body: some View {
        ProviderScreen(
            title: bzrString("provider.application.status.title"), state: state, onBack: onBack
        ) {
            EmptyState(title: bzrString(statusKey), body: bzrString(statusBodyKey))
            if let reason = state.items.first { WarningBox(text: reason) }
            ForEach(state.visibleActions.filter { $0 != "submit_provider_application" }, id: \.self) { action in
                PrimaryButton(text: actionLabel(action)) { onAction(action) }
            }
        }
    }

    private var statusKey: String { state.messageKey ?? "provider.application.pending" }
    private var statusBodyKey: String {
        switch statusKey {
        case "provider.application.rejected": "provider.application.rejected.body"
        case "provider.application.active": "provider.application.active.body"
        case "provider.application.suspended": "provider.application.suspended.body"
        default: "provider.application.pending.body"
        }
    }
    private func actionLabel(_ action: String) -> String {
        bzrString(
            action == "open_provider_home"
                ? "action.open_provider_home" : "action.resubmit_provider_application")
    }
}

public struct P08HomeView: View {
    let state: ProviderUIState
    let onAvailabilityChange: (Bool) async -> Void
    let onAction: (String) -> Void
    let onRequest: (Int) -> Void
    let onBack: () -> Void

    public var body: some View {
        ProviderScreen(title: bzrString("provider.home.title"), state: state, onBack: onBack) {
            availabilityCard
            if state.messageKey == "provider.home.availability.saved" {
                InfoBanner(text: bzrString("provider.home.availability.saved"))
            }

            BzrText(bzrString("provider.home.assigned.title"), style: DesignType.sectionTitle)
            if state.activeOrderID != nil, state.items.count >= 4 {
                OrderCard(
                    title: String(format: bzrString("provider.home.order_number"), state.items[0]),
                    subtitle: state.items[1] + "\n"
                        + [state.items[2], timingLabel(state.items[3])]
                        .filter { !$0.isEmpty }.joined(separator: " — "),
                    status: providerStatus(state.displayStatus),
                    onClick: { onAction("open_assigned_order") })
                if state.visibleActions.contains("open_assigned_order") {
                    PrimaryButton(text: bzrString("action.open_assigned_order")) {
                        onAction("open_assigned_order")
                    }
                }
            } else {
                EmptyState(
                    title: bzrString("provider.home.no_assignment.title"),
                    body: bzrString("provider.home.no_assignment.body"))
            }
            if state.visibleActions.contains("open_my_offers") {
                BzrText(bzrString("provider.market.requests.title"), style: DesignType.sectionTitle)
                if state.options.isEmpty {
                    EmptyState(title: bzrString("provider.market.requests.empty"), body: "")
                } else {
                    ForEach(state.options.indices, id: \.self) { index in
                        let title = state.options[index].split(separator: "|", maxSplits: 1).map(String.init)
                        let detail = value(state.secondaryOptions, index).replacingOccurrences(of: "|", with: " · ")
                        OrderCard(
                            title: title.count > 1
                                ? String(format: bzrString("provider.offers.card.title"), title[0], title[1])
                                : state.options[index],
                            subtitle: detail, status: bzrString("order.status.provider.OPEN"),
                            onClick: { onRequest(index) })
                    }
                }
                SecondaryButton(text: bzrString("action.open_my_offers")) {
                    onAction("open_my_offers")
                }
            }
            if !shortcuts.isEmpty {
                BzrText(bzrString("provider.home.shortcuts.title"), style: DesignType.sectionTitle)
                // DEC-063: shortcuts as gradient-icon tiles in equal columns.
                BzrServiceGrid {
                    ForEach(shortcuts, id: \.action) { shortcut in
                        Button {
                            onAction(shortcut.action)
                        } label: {
                            SelectableTile(text: shortcut.label, icon: shortcut.icon, state: .unselected)
                        }
                        .buttonStyle(BzrPressStyle())
                    }
                }
            }
        }
    }

    /// DEC-063: on = the brand gradient card with white text; off = a white card. Tapping toggles.
    private var availabilityCard: some View {
        let available = state.availableNow
        return Button {
            Task { await onAvailabilityChange(!available) }
        } label: {
            HStack(spacing: DesignSpace.m) {
                Group {
                    if available {
                        BzrIcon(.check, tint: DesignColors.onPrimary)
                    } else {
                        BzrGradientIcon(.check)
                    }
                }
                .frame(width: DesignSize.menuIconBox, height: DesignSize.menuIconBox)
                .background {
                    RoundedRectangle(cornerRadius: DesignRadius.menuIconBox)
                        .fill(available ? AnyShapeStyle(DesignColors.onHeroWell) : AnyShapeStyle(DesignGradients.iconWell))
                }
                VStack(alignment: .leading, spacing: DesignSpace.xxs) {
                    BzrText(
                        bzrString("provider.home.available.title"),
                        style: DesignType.cardTitle.colored(available ? DesignColors.onPrimary : DesignColors.navy900))
                    BzrText(
                        bzrString("provider.home.available.body"),
                        style: DesignType.caption.colored(available ? DesignColors.onHeroBody : DesignColors.slate500))
                }
                Spacer(minLength: 0)
                BzrText(
                    bzrString(available ? "provider.home.available.on_state" : "provider.home.available.off_state"),
                    style: DesignType.body.weighted(DesignFont.bold)
                        .colored(available ? DesignColors.primary700 : DesignColors.onPrimary), lineLimit: 1
                )
                .padding(.horizontal, DesignSpace.l)
                .frame(minHeight: DesignSize.touchTargetMin)
                .background {
                    if available {
                        Capsule().fill(DesignGradients.iconWell)
                    } else {
                        RoundedRectangle(cornerRadius: DesignRadius.button).fill(DesignGradients.brand)
                    }
                }
            }
            .padding(DesignSpace.l)
            .background {
                if available {
                    RoundedRectangle(cornerRadius: DesignRadius.heroCard).fill(DesignGradients.hero)
                } else {
                    RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface)
                }
            }
            .bzrShadow(available ? DesignShadows.brand : DesignShadows.card)
        }
        .buttonStyle(BzrPressStyle())
        .disabled(!state.canContinue)
        .opacity(state.canContinue ? 1 : DesignOpacity.disabled)
        .accessibilityElement(children: .combine)
        .accessibilityValue(
            bzrString(available ? "provider.home.available.on_state" : "provider.home.available.off_state"))
    }

    private struct Shortcut {
        let action: String
        let icon: BzrIconKey
        let label: String
    }

    /// The shortcuts the state allows (offers stay with the market list).
    private var shortcuts: [Shortcut] {
        [
            Shortcut(
                action: "open_notifications", icon: .bell,
                label: state.unreadCount > 0
                    ? String(format: bzrString("action.open_notifications.count"), state.unreadCount)
                    : bzrString("action.open_notifications")),
            Shortcut(action: "open_messages", icon: .messages, label: bzrString("action.open_messages")),
            Shortcut(action: "open_earnings", icon: .orders, label: bzrString("action.open_earnings")),
            Shortcut(action: "open_provider_profile", icon: .account, label: bzrString("action.open_provider_profile")),
        ].filter { state.visibleActions.contains($0.action) }
    }

    private func providerStatus(_ key: String) -> String {
        bzrString(providerStatusKeys[key] ?? "order.status")
    }

    private func timingLabel(_ value: String) -> String {
        value == "provider.home.timing.now" ? bzrString("provider.home.timing.now") : value
    }
    private func value(_ values: [String], _ index: Int) -> String {
        values.indices.contains(index) ? values[index] : ""
    }
}

public struct P09AvailableRequestView: View {
    let state: ProviderUIState
    let onAction: (String) -> Void
    let onBack: () -> Void

    public var body: some View {
        ProviderScreen(title: bzrString("provider.market.request.title"), state: state, onBack: onBack) {
            ProviderHeader(
                name: state.customerName, rating: state.customerRating,
                services: nil, size: .medium, verifiedLabel: nil)
            if !state.displayStatus.isEmpty {
                Badge(text: bzrString(providerStatusKeys[state.displayStatus] ?? "order.status"), kind: .status)
            }
            SummaryCard(
                title: bzrString("provider.market.request.details"),
                rows: state.items.prefix(3).map { (.info, $0) }, editText: nil)
            BzrText(bzrString("provider.market.request.media"), style: DesignType.sectionTitle)
            if state.options.isEmpty {
                EmptyState(title: bzrString("provider.market.request.no_media"), body: "")
            } else {
                ForEach(state.options.indices, id: \.self) { index in
                    let media = mediaValue(state.options[index])
                    MediaThumb(
                        title: media.title, stateLabel: "",
                        kind: media.kind, state: .uploaded, readOnly: true)
                }
            }
            if state.items.count > 3 {
                SummaryCard(
                    title: bzrString("provider.market.request.conditions"),
                    rows: state.items.dropFirst(3).map { (.clock, timing($0)) }, editText: nil)
            }
            if state.secondaryOptions.count >= 3 {
                InfoBanner(
                    text: [
                        "\(bzrString("offer.price")): \(state.secondaryOptions[0]) \(bzrString("common.currency.egp"))",
                        String(format: bzrString("provider.offer.net"), state.secondaryOptions[1]),
                        state.secondaryOptions[2],
                    ].joined(separator: "\n"))
            }
            if let message = state.messageKey {
                WarningBox(text: bzrString(message))
            }
            ForEach(state.visibleActions, id: \.self) { action in
                if action == "submit_offer" {
                    PrimaryButton(
                        text: bzrString("provider.offer.submit"),
                        state: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled)
                    ) { onAction(action) }
                } else {
                    SecondaryButton(text: actionLabel(action)) { onAction(action) }
                }
            }
        }
    }

    private func timing(_ value: String) -> String {
        value == "provider.home.timing.now" ? bzrString(value) : value
    }
    private func actionLabel(_ action: String) -> String {
        switch action {
        case "withdraw_offer": return bzrString("action.withdraw_offer")
        case "chat": return bzrString("action.chat")
        default: return bzrString("action.open_available_request")
        }
    }
    private func mediaValue(_ raw: String) -> (title: String, kind: MediaKind) {
        let parts = raw.split(separator: ":", maxSplits: 1).map(String.init)
        let index = Int(parts.count > 1 ? parts[1] : "1") ?? 1
        switch parts.first {
        case "VIDEO": return (String(format: bzrString("provider.market.request.media.video"), index), .video)
        case "AUDIO": return (String(format: bzrString("provider.market.request.media.audio"), index), .audio)
        default: return (String(format: bzrString("provider.market.request.media.photo"), index), .photo)
        }
    }
}

public struct P10OfferView: View {
    let state: ProviderUIState
    let onEtaSelect: (Int) -> Void
    let onDeductibleSelect: (Int) -> Void
    let onPriceChange: (String) -> Void
    let onIncludesChange: (String) -> Void
    let onNoteChange: (String) -> Void
    let onSubmit: () async -> Void
    let onBack: () -> Void

    public var body: some View {
        ProviderScreen(title: bzrString("provider.offer.title"), state: state, onBack: onBack) {
            if !state.items.isEmpty {
                SummaryCard(
                    title: bzrString("provider.offer.summary"),
                    rows: state.items.map { (.info, timing($0)) }, editText: nil)
            }
            if state.messageKey == "provider.offer.success" {
                InfoBanner(text: bzrString("provider.offer.success"))
            } else if let message = state.messageKey {
                WarningBox(text: bzrString(message))
            }
            AmountField(
                label: bzrString("provider.offer.price"), placeholder: bzrString("provider.offer.price"),
                value: value(state.fieldValues, 0), currency: bzrString("common.currency.egp"),
                state: fieldState("provider.offer.price.invalid"),
                error: fieldError("provider.offer.price.invalid"), onValueChange: onPriceChange)
            if state.showPriceGuide {
                BzrText(bzrString("provider.offer.eta"), style: DesignType.sectionTitle)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: DesignSpace.s) {
                        ForEach(state.options.indices, id: \.self) { index in
                            Button(
                                action: { onEtaSelect(index) },
                                label: {
                                    SelectableChip(
                                        text: bzrString("format.minutes").replacingOccurrences(
                                            of: "{n}", with: state.options[index]),
                                        state: index == state.selectedOptionIndex ? .selected : .unselected)
                                }
                            ).buttonStyle(BzrPressStyle())
                        }
                    }
                }
                if state.fieldErrors.contains("provider.offer.eta.required") {
                    WarningBox(text: bzrString("provider.offer.eta.required"))
                }
            }
            if state.showOutsideReason {
                BzrText(bzrString("provider.offer.deductible"), style: DesignType.sectionTitle)
                ForEach(state.itemDetails.indices, id: \.self) { index in
                    Button(
                        action: { onDeductibleSelect(index) },
                        label: {
                            RadioCard(
                                title: state.itemDetails[index], body: "",
                                selected: state.selectedSecondaryIndices.contains(index))
                        }
                    ).buttonStyle(BzrPressStyle())
                }
                if state.fieldErrors.contains("provider.offer.deductible.required") {
                    WarningBox(text: bzrString("provider.offer.deductible.required"))
                }
            }
            AppTextField(
                label: bzrString("provider.offer.includes"), placeholder: bzrString("provider.offer.includes"),
                value: value(state.fieldValues, 1), state: fieldState("provider.offer.includes.invalid"),
                error: fieldError("provider.offer.includes.invalid"), onValueChange: onIncludesChange)
            TextAreaField(
                label: bzrString("provider.offer.note"), placeholder: bzrString("provider.offer.note"),
                value: value(state.fieldValues, 2), state: fieldState("provider.offer.note.invalid"),
                error: fieldError("provider.offer.note.invalid"), onValueChange: onNoteChange)
            InfoBanner(text: bzrString("provider.offer.note.masking"))
            InfoBanner(
                text: String(format: bzrString("provider.offer.net"), value(state.secondaryOptions, 0)))
            if state.visibleActions.contains("submit_offer") {
                PrimaryButton(
                    text: bzrString("provider.offer.submit"),
                    state: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled)
                ) { Task { await onSubmit() } }
            }
        }
    }

    private func value(_ values: [String], _ index: Int) -> String {
        values.indices.contains(index) ? values[index] : ""
    }
    private func timing(_ value: String) -> String {
        value == "provider.home.timing.now" ? bzrString(value) : value
    }
    private func fieldState(_ key: String) -> FieldVisualState {
        state.isBusy ? .disabled : (state.fieldErrors.contains(key) ? .error : .filled)
    }
    private func fieldError(_ key: String) -> String? {
        state.fieldErrors.contains(key) ? bzrString(key) : nil
    }
}

public struct P11OffersView: View {
    let state: ProviderUIState
    let onOfferAction: (Int, String) -> Void
    let onConfirmWithdraw: () async -> Void
    let onCancelWithdraw: () -> Void
    let onBack: () -> Void

    public var body: some View {
        ProviderScreen(title: bzrString("provider.offers.title"), state: state, onBack: onBack) {
            if state.phase == .empty {
                EmptyState(
                    title: bzrString("provider.offers.empty.title"),
                    body: bzrString("provider.offers.empty.body"))
            }
            if let message = state.messageKey { InfoBanner(text: bzrString(message)) }
            ForEach(state.items.indices, id: \.self) { index in
                let actions = value(state.secondaryOptions, index).split(separator: ",").map(String.init)
                OrderCard(
                    title: offerTitle(value(state.items, index)),
                    subtitle: offerDetail(value(state.itemDetails, index)),
                    status: value(state.itemStates, index),
                    onClick: { if let action = actions.first { onOfferAction(index, action) } })
                if let action = preferredAction(actions) {
                    SecondaryButton(text: actionLabel(action)) {
                        onOfferAction(index, action)
                    }
                }
            }
            if state.currentAction == "withdraw_offer" {
                AppBottomSheet(
                    title: bzrString("provider.offers.withdraw.title"),
                    body: bzrString("provider.offers.withdraw.body"),
                    action: bzrString("provider.offers.withdraw.action"),
                    actionState: state.isBusy ? .loading : .normal,
                    secondaryAction: bzrString("common.cancel"),
                    onAction: { Task { await onConfirmWithdraw() } },
                    onSecondary: onCancelWithdraw)
            }
        }
    }

    private func value(_ values: [String], _ index: Int) -> String {
        values.indices.contains(index) ? values[index] : ""
    }
    private func preferredAction(_ actions: [String]) -> String? {
        if actions.contains("withdraw_offer") { return "withdraw_offer" }
        if actions.contains("open_available_request") { return "open_available_request" }
        return actions.contains("open_assigned_order") ? "open_assigned_order" : nil
    }
    private func actionLabel(_ action: String) -> String {
        switch action {
        case "withdraw_offer": return bzrString("action.withdraw_offer")
        case "open_assigned_order": return bzrString("action.open_assigned_order")
        default: return bzrString("action.open_available_request")
        }
    }
    private func offerTitle(_ raw: String) -> String {
        let values = raw.split(separator: "|", maxSplits: 1).map(String.init)
        return values.count > 1
            ? String(format: bzrString("provider.offers.card.title"), values[0], values[1]) : raw
    }
    private func offerDetail(_ raw: String) -> String {
        let values = raw.split(separator: "|").map(String.init)
        return values.count > 2
            ? String(format: bzrString("provider.offers.card.detail"), values[0], values[1], values[2])
            : raw
    }
}

private struct ProviderActiveOrderContent: View {
    let state: ProviderUIState
    let screen: String
    let destination: (Double, Double)?
    let onAction: (String) -> Void
    let onBack: () -> Void

    var body: some View {
        ProviderScreen(title: bzrString("provider.order.title"), state: state, onBack: onBack) {
            Badge(text: providerStatus(state.displayStatus), kind: .status)
            ProviderHeader(
                name: state.customerName, rating: state.customerRating,
                services: nil, size: .medium)
            if screen == "SCR-P13", let destination {
                NativeMapView(
                    destinationCoordinate: CLLocationCoordinate2D(
                        latitude: destination.0, longitude: destination.1)
                )
                .frame(height: DesignSize.mapCardHeight)
                .clipShape(RoundedRectangle(cornerRadius: DesignRadius.card))
                .accessibilityLabel(bzrString("map.title"))
            }
            SummaryCard(
                title: bzrString("provider.order.summary.title"),
                rows: [
                    (.orders, state.items.indices.contains(0) ? state.items[0] : ""),
                    (.info, state.items.indices.contains(1) ? state.items[1] : ""),
                    (.location, state.items.indices.contains(2) ? state.items[2] : ""),
                    (.clock, state.items.indices.contains(3) ? timing(state.items[3]) : ""),
                ], editText: nil)
            if state.showPriceGuide {
                InfoBanner(
                    text: String(
                        format: bzrString("provider.price_guide.range"),
                        state.priceGuideMinimum, state.priceGuideMaximum))
            }
            if let message = state.messageKey { WarningBox(text: bzrString(providerWarningKey(message))) }
            if screen == "SCR-P16", state.secondaryOptions.count >= 4 {
                SummaryCard(
                    title: bzrString("provider.payment.summary"),
                    rows: [
                        (.orders, state.secondaryOptions[0]), (.info, state.secondaryOptions[1]),
                        (.check, state.secondaryOptions[2]), (.account, state.secondaryOptions[3]),
                    ], editText: nil)
            }
            StatusStepper(
                steps: zip(stepLabels, state.stepStates).map { (bzrString($0), providerStep($1)) })
            ForEach(state.visibleActions, id: \.self) { action in
                providerActionButton(action, state: state) { onAction(action) }
            }
        }
    }

    private var stepLabels: [String] {
        [
            "status.confirmed", "status.on_the_way", "status.arrived", "status.in_progress",
            "status.payment", "status.closed",
        ]
    }

    private func timing(_ value: String) -> String {
        value == "provider.home.timing.now" ? bzrString(value) : value
    }

    private func providerStatus(_ key: String) -> String {
        bzrString(providerStatusKeys[key] ?? "order.status")
    }
}

public struct P12ConfirmedOrderView: View {
    let state: ProviderUIState
    let destination: (Double, Double)?
    let onAction: (String) -> Void
    let onBack: () -> Void
    public var body: some View {
        ProviderActiveOrderContent(
            state: state, screen: "SCR-P12", destination: destination,
            onAction: onAction, onBack: onBack)
    }
}

public struct P13OnTheWayView: View {
    let state: ProviderUIState
    let destination: (Double, Double)?
    let onAction: (String) -> Void
    let onBack: () -> Void
    public var body: some View {
        ProviderActiveOrderContent(
            state: state, screen: "SCR-P13", destination: destination,
            onAction: onAction, onBack: onBack)
    }
}

public struct P14ArrivedView: View {
    let state: ProviderUIState
    let destination: (Double, Double)?
    let onAction: (String) -> Void
    let onBack: () -> Void
    public var body: some View {
        ProviderActiveOrderContent(
            state: state, screen: "SCR-P14", destination: destination,
            onAction: onAction, onBack: onBack)
    }
}

public struct P15InProgressView: View {
    let state: ProviderUIState
    let destination: (Double, Double)?
    let onAction: (String) -> Void
    let onBack: () -> Void
    public var body: some View {
        ProviderActiveOrderContent(
            state: state, screen: "SCR-P15", destination: destination,
            onAction: onAction, onBack: onBack)
    }
}

public struct P16AwaitingPaymentView: View {
    let state: ProviderUIState
    let destination: (Double, Double)?
    let onAction: (String) -> Void
    let onBack: () -> Void
    public var body: some View {
        ProviderActiveOrderContent(
            state: state, screen: "SCR-P16", destination: destination,
            onAction: onAction, onBack: onBack)
    }
}

public struct P17CustomerRatingView: View {
    let state: ProviderUIState
    let onRate: (Int) -> Void
    let onSubmit: () async -> Void
    let onBack: () -> Void

    public var body: some View {
        ProviderScreen(title: bzrString("provider.rating.title"), state: state, onBack: onBack) {
            SummaryCard(
                title: bzrString("provider.rating.title"),
                rows: [
                    (.orders, value(state.items, 0)), (.account, value(state.items, 1)),
                    (.info, value(state.items, 2)),
                ], editText: nil)
            BzrText(bzrString("provider.rating.label"), style: DesignType.sectionTitle)
            HStack(spacing: DesignSpace.s) {
                ForEach(0..<5, id: \.self) { index in
                    IconSquareButton(
                        label: "\(bzrString("provider.rating.label")) \(index + 1)",
                        icon: index <= state.selectedOptionIndex ? .starFilled : .star
                    ) { onRate(index) }
                }
            }
            InfoBanner(text: bzrString("provider.rating.notice"))
            if let message = state.messageKey {
                if message == "provider.rating.success" {
                    InfoBanner(text: bzrString("provider.rating.success"))
                } else if message == "provider.rating.unavailable" {
                    WarningBox(text: bzrString("provider.rating.unavailable"))
                } else {
                    WarningBox(text: bzrString(message))
                }
            } else if let error = state.fieldErrors.first {
                WarningBox(text: bzrString(error))
            }
            if state.visibleActions.contains("rate_customer") {
                PrimaryButton(
                    text: bzrString("provider.rating.submit"),
                    state: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled)
                ) { Task { await onSubmit() } }
            }
        }
    }

    private func value(_ values: [String], _ index: Int) -> String {
        values.indices.contains(index) ? values[index] : ""
    }
}

public struct P18EarningsView: View {
    let state: ProviderUIState
    let onBack: () -> Void

    public var body: some View {
        ProviderScreen(title: bzrString("provider.earnings.title"), state: state, onBack: onBack) {
            StatRow(items: [
                StatItem(
                    icon: .orders, value: value(state.secondaryOptions, 0),
                    label: bzrString("provider.earnings.balance")),
                StatItem(
                    icon: .check, value: value(state.secondaryOptions, 1),
                    label: bzrString("provider.earnings.available")),
                StatItem(
                    icon: .clock, value: value(state.secondaryOptions, 2),
                    label: bzrString("provider.earnings.pending")),
            ])
            if state.messageKey == "provider.earnings.employee.info" {
                InfoBanner(text: bzrString("provider.earnings.employee.info"))
            }
            if state.phase == .empty {
                EmptyState(title: bzrString("provider.earnings.empty"), body: "")
            }
            ForEach(state.items.indices, id: \.self) { index in
                SummaryCard(
                    title: state.items[index],
                    rows: [
                        (transactionIcon(value(state.itemStates, index)), value(state.itemDetails, index))
                    ],
                    editText: nil)
            }
            if state.isBusy { BzrText(bzrString("orders.loading_more"), style: DesignType.secondary) }
        }
    }

    private func value(_ values: [String], _ index: Int) -> String {
        values.indices.contains(index) ? values[index] : ""
    }
    private func transactionIcon(_ type: String) -> BzrIconKey {
        ["PAYOUT", "REMITTANCE"].contains(type) ? .check : .orders
    }
}

public struct P19ProfileView: View {
    let state: ProviderUIState
    let onExperienceChange: (String) -> Void
    let onBioChange: (String) -> Void
    let onSpecialty: (Int) -> Void
    let onArea: (Int) -> Void
    let onCaptionChange: (String) -> Void
    let onAvatar: (CustomerMediaUpload) async -> Void
    let onPortfolio: (CustomerMediaUpload) async -> Void
    let onDeletePortfolio: (Int) async -> Void
    let onSupport: () -> Void
    let onSave: () async -> Void
    let onBack: () -> Void
    @State private var avatar: PhotosPickerItem?
    @State private var portfolio: PhotosPickerItem?
    @State private var avatarPickerPresented = false
    @State private var portfolioPickerPresented = false
    @State private var pendingDeleteIndex: Int?

    public var body: some View {
        ProviderScreen(title: bzrString("provider.profile.title"), state: state, onBack: onBack) {
            ProviderHeader(
                name: state.customerName, rating: state.customerRating,
                services: nil, size: .medium, verifiedLabel: nil)
            StatRow(items: [
                StatItem(icon: .star, value: value(state.itemStates, 0), label: bzrString("stat.rating")),
                StatItem(
                    icon: .orders, value: value(state.itemStates, 1), label: bzrString("stat.services")),
                StatItem(
                    icon: .clock,
                    value: bzrString("format.minutes").replacingOccurrences(
                        of: "{n}", with: value(state.itemStates, 2)),
                    label: bzrString("stat.response_speed")),
            ])
            SecondaryButton(
                text: bzrString("provider.onboarding.profile_photo.add"),
                onClick: { avatarPickerPresented = true }
            )
            .photosPicker(isPresented: $avatarPickerPresented, selection: $avatar, matching: .images)
            .onChange(of: avatar) { _, item in upload(item, name: "provider-avatar.jpg", action: onAvatar)
            }
            if let message = state.messageKey {
                if message == "provider.profile.saved" {
                    InfoBanner(text: bzrString(message))
                } else {
                    WarningBox(text: bzrString(message))
                }
            }
            AppTextField(
                label: bzrString("provider.profile.experience"), placeholder: "0",
                value: value(state.fieldValues, 0),
                state: fieldState("provider.onboarding.experience.invalid"),
                error: fieldError("provider.onboarding.experience.invalid"),
                onValueChange: onExperienceChange)
            TextAreaField(
                label: bzrString("provider.profile.bio"), placeholder: bzrString("provider.profile.bio"),
                value: value(state.fieldValues, 1), state: fieldState("provider.onboarding.bio.max"),
                error: fieldError("provider.onboarding.bio.max"), onValueChange: onBioChange)
            SummaryCard(
                title: bzrString("provider.profile.categories"),
                rows: state.options.map { (.info, $0) }, editText: nil)
            InfoBanner(text: bzrString("provider.profile.read_only"))
            BzrText(bzrString("provider.profile.specialties"), style: DesignType.sectionTitle)
            ForEach(state.secondaryOptions.indices, id: \.self) { index in
                Button(
                    action: { onSpecialty(index) },
                    label: {
                        CheckRow(
                            text: state.secondaryOptions[index],
                            checked: state.selectedPrimaryIndices.contains(index))
                    }
                ).buttonStyle(BzrPressStyle())
            }
            BzrText(bzrString("provider.profile.areas"), style: DesignType.sectionTitle)
            ForEach(state.areaOptions.indices, id: \.self) { index in
                Button(
                    action: { onArea(index) },
                    label: {
                        CheckRow(
                            text: state.areaOptions[index],
                            checked: state.selectedSecondaryIndices.contains(index))
                    }
                ).buttonStyle(BzrPressStyle())
            }
            if let error = state.fieldErrors.first { WarningBox(text: bzrString(error)) }
            ForEach(state.items.indices, id: \.self) { index in
                MediaThumb(
                    title: state.items[index], stateLabel: bzrString("media.uploaded"),
                    kind: .photo, state: .uploaded, onDelete: { pendingDeleteIndex = index })
            }
            TextAreaField(
                label: bzrString("provider.profile.portfolio.caption"),
                placeholder: bzrString("provider.profile.portfolio.caption"),
                value: value(state.fieldValues, 2), state: state.isBusy ? .disabled : .filled,
                error: nil, onValueChange: onCaptionChange)
            if state.visibleActions.contains("add_portfolio_item") {
                SecondaryButton(
                    text: bzrString("provider.profile.portfolio.add"),
                    onClick: { portfolioPickerPresented = true }
                )
                .photosPicker(
                    isPresented: $portfolioPickerPresented, selection: $portfolio, matching: .images
                )
                .onChange(of: portfolio) { _, item in
                    upload(item, name: "provider-portfolio.jpg", action: onPortfolio)
                }
            }
            if state.visibleActions.contains("contact_support") {
                SecondaryButton(text: bzrString("action.contact_support"), onClick: onSupport)
            }
            if state.visibleActions.contains("update_provider_profile") {
                PrimaryButton(
                    text: bzrString("common.save"),
                    state: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled)
                ) { Task { await onSave() } }
            }
            if let index = pendingDeleteIndex {
                AppBottomSheet(
                    title: bzrString("provider.profile.portfolio.delete.title"),
                    body: bzrString("provider.profile.portfolio.delete.body"),
                    action: bzrString("common.delete"), secondaryAction: bzrString("common.cancel"),
                    onAction: {
                        Task {
                            await onDeletePortfolio(index)
                            pendingDeleteIndex = nil
                        }
                    },
                    onSecondary: { pendingDeleteIndex = nil })
            }
        }
    }

    private func value(_ values: [String], _ index: Int) -> String {
        values.indices.contains(index) ? values[index] : ""
    }
    private func fieldState(_ key: String) -> FieldVisualState {
        state.isBusy ? .disabled : (state.fieldErrors.contains(key) ? .error : .filled)
    }
    private func fieldError(_ key: String) -> String? {
        state.fieldErrors.contains(key) ? bzrString(key) : nil
    }
    private func upload(
        _ item: PhotosPickerItem?, name: String,
        action: @escaping (CustomerMediaUpload) async -> Void
    ) {
        Task {
            guard let source = try? await item?.loadTransferable(type: Data.self),
                let image = UIImage(data: source),
                let data = image.jpegData(compressionQuality: 0.9), data.count <= 10 * 1024 * 1024
            else { return }
            await action(CustomerMediaUpload(fileName: name, mimeType: "image/jpeg", data: data))
        }
    }
}

public struct P20ProposalView: View {
    let state: ProviderUIState
    let onTypeSelect: (Int) -> Void
    let onAmountChange: (String) -> Void
    let onReasonChange: (String) -> Void
    let onOutsideReasonChange: (String) -> Void
    let onPickPhoto: (CustomerMediaUpload) async -> Void
    let onDeletePhoto: () -> Void
    let onSubmit: () async -> Void
    let onBack: () -> Void
    @State private var photo: PhotosPickerItem?
    @State private var showPhotoPicker = false

    public var body: some View {
        ProviderScreen(title: bzrString("provider.proposal.title"), state: state, onBack: onBack) {
            ForEach(Array(state.options.enumerated()), id: \.offset) { index, label in
                Button(
                    action: { onTypeSelect(index) },
                    label: { RadioCard(title: label, body: "", selected: state.selectedOptionIndex == index) }
                ).buttonStyle(BzrPressStyle())
            }
            if state.showPriceGuide {
                InfoBanner(
                    text: String(
                        format: bzrString("provider.price_guide.range"),
                        state.priceGuideMinimum, state.priceGuideMaximum))
            }
            if let message = state.messageKey { WarningBox(text: bzrString(message)) }
            AmountField(
                label: bzrString("provider.proposal.amount"),
                placeholder: bzrString("provider.proposal.amount"),
                value: state.secondaryOptions.first ?? "", currency: bzrString("common.currency.egp"),
                state: fieldState("provider.proposal.amount.invalid"),
                error: fieldError("provider.proposal.amount.invalid"), onValueChange: onAmountChange)
            TextAreaField(
                label: bzrString("provider.proposal.reason"),
                placeholder: bzrString("provider.proposal.reason"),
                value: state.secondaryOptions.indices.contains(1) ? state.secondaryOptions[1] : "",
                state: fieldState("provider.proposal.reason.invalid"),
                error: fieldError("provider.proposal.reason.invalid"), onValueChange: onReasonChange)
            if state.showOutsideReason {
                TextAreaField(
                    label: bzrString("provider.proposal.outside_reason"),
                    placeholder: bzrString("provider.proposal.outside_reason"),
                    value: state.secondaryOptions.indices.contains(2) ? state.secondaryOptions[2] : "",
                    state: fieldState("provider.proposal.outside_reason.invalid"),
                    error: fieldError("provider.proposal.outside_reason.invalid"),
                    onValueChange: onOutsideReasonChange)
            }
            if state.hasPhoto {
                MediaThumb(
                    title: bzrString("provider.proposal.photo"), stateLabel: bzrString("media.uploaded"),
                    kind: .photo, state: .uploaded, onDelete: onDeletePhoto)
            } else {
                SecondaryButton(
                    text: bzrString("provider.proposal.photo.add"), onClick: { showPhotoPicker = true }
                )
                .photosPicker(isPresented: $showPhotoPicker, selection: $photo, matching: .images)
                .onChange(of: photo) { _, item in upload(item) }
            }
            AppBottomSheet(
                title: bzrString("provider.proposal.title"), body: "",
                action: bzrString(providerActionKey(state.currentAction)),
                actionState: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled),
                secondaryAction: bzrString("common.cancel"),
                onAction: { Task { await onSubmit() } }, onSecondary: onBack)
        }
    }

    private func fieldState(_ key: String) -> FieldVisualState {
        state.fieldErrors.contains(key) ? .error : .filled
    }
    private func fieldError(_ key: String) -> String? {
        state.fieldErrors.contains(key) ? bzrString(key) : nil
    }
    private func upload(_ item: PhotosPickerItem?) {
        Task {
            guard let source = try? await item?.loadTransferable(type: Data.self),
                let image = UIImage(data: source),
                let data = image.jpegData(compressionQuality: 0.9), data.count <= 10 * 1024 * 1024
            else { return }
            await onPickPhoto(
                CustomerMediaUpload(fileName: "proposal.jpg", mimeType: "image/jpeg", data: data))
        }
    }
}

public struct P21UnableView: View {
    let state: ProviderUIState
    let onReasonSelect: (Int) -> Void
    let onNoteChange: (String) -> Void
    let onConfirm: () async -> Void
    let onBack: () -> Void

    public var body: some View {
        ProviderScreen(title: title, state: state, onBack: onBack) {
            if state.currentAction != "report_no_show" {
                ForEach(Array(state.options.enumerated()), id: \.offset) { index, label in
                    Button(
                        action: { onReasonSelect(index) },
                        label: {
                            RadioCard(title: label, body: "", selected: state.selectedOptionIndex == index)
                        }
                    ).buttonStyle(BzrPressStyle())
                }
            }
            if state.currentAction == "report_unable" {
                TextAreaField(
                    label: bzrString("provider.unable.note"), placeholder: bzrString("provider.unable.note"),
                    value: state.fieldValues.first ?? "",
                    state: state.fieldErrors.contains("provider.unable.note.invalid") ? .error : .filled,
                    error: state.fieldErrors.contains("provider.unable.note.invalid")
                        ? bzrString("provider.unable.note.invalid") : nil,
                    onValueChange: onNoteChange)
            }
            if let message = state.messageKey {
                WarningBox(text: bzrString(providerWarningKey(message)))
            } else if let error = state.fieldErrors.first {
                WarningBox(text: bzrString(error))
            }
            AppBottomSheet(
                title: title,
                body: bzrString(
                    state.currentAction == "report_no_show"
                        ? "provider.no_show.confirm" : "provider.unable.reason"),
                action: bzrString("provider.unable.confirm"),
                actionState: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled),
                secondaryAction: bzrString("common.cancel"),
                onAction: { Task { await onConfirm() } }, onSecondary: onBack)
        }
    }

    private var title: String {
        bzrString(
            state.currentAction == "back_out"
                ? "provider.back_out.title"
                : (state.currentAction == "report_no_show"
                    ? "provider.no_show.title" : "provider.unable.title"))
    }
}

@ViewBuilder
private func providerActionButton(
    _ action: String, state: ProviderUIState, onClick: @escaping () -> Void
) -> some View {
    let label = bzrString(providerActionKey(action))
    if [
        "start_trip", "mark_arrived", "start_work", "submit_execution_quote", "complete_work",
        "confirm_cash",
    ].contains(action) {
        PrimaryButton(
            text: label,
            state: state.isBusy
                ? (state.currentAction == action ? .loading : .disabled) : .normal,
            onClick: onClick)
    } else if ["back_out", "report_unable", "report_no_show"].contains(action) {
        DangerTextButton(text: label, onClick: onClick).disabled(state.isBusy)
    } else {
        SecondaryButton(text: label, enabled: !state.isBusy, onClick: onClick)
    }
}

// swiftlint:disable:next cyclomatic_complexity
private func providerActionKey(_ action: String) -> String {
    switch action {
    case "start_trip": "action.start_trip"
    case "back_out": "action.back_out"
    case "mark_arrived": "action.mark_arrived"
    case "start_work": "action.start_work"
    case "submit_execution_quote": "action.submit_execution_quote"
    case "complete_inspection_only": "action.complete_inspection_only.free"
    case "submit_proposal": "action.submit_proposal"
    case "complete_work": "action.complete_work"
    case "report_unable": "action.report_unable"
    case "report_no_show": "action.report_no_show"
    case "confirm_cash": "action.confirm_cash"
    case "navigate": "action.navigate"
    case "call": "action.call"
    case "chat": "action.chat"
    default: "common.close"
    }
}

private func providerWarningKey(_ key: String) -> String {
    switch key {
    case "provider.order.trip.not_ready": "provider.order.trip.not_ready"
    case "provider.order.location.required": "provider.order.location.required"
    case "provider.price_guide.other_review": "provider.price_guide.other_review"
    case "provider.proposal.pending": "provider.proposal.pending"
    case "provider.proposal.success": "provider.proposal.success"
    case "provider.payment.waiting": "provider.payment.waiting"
    case "provider.no_show.confirm": "provider.no_show.confirm"
    case "provider.no_show.not_ready": "provider.no_show.not_ready"
    default: "error.body"
    }
}

private func providerStep(_ value: String) -> StepState {
    switch value {
    case "done": .done
    case "active": .active
    case "on_hold": .onHold
    default: .pending
    }
}

private final class ProviderLocationSource: NSObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private var completion: ((Double, Double) -> Void)?

    override init() {
        super.init()
        manager.delegate = self
    }

    func resolve(_ completion: @escaping (Double, Double) -> Void) {
        self.completion = completion
        if manager.authorizationStatus == .notDetermined { manager.requestWhenInUseAuthorization() }
        manager.requestLocation()
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let coordinate = locations.last?.coordinate else { return }
        completion?(coordinate.latitude, coordinate.longitude)
        completion = nil
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        completion = nil
    }
}

struct ProviderJourneyRoot: View {
    @Bindable var viewModel: ProviderViewModel
    let onClose: () -> Void
    let onOpenSupport: () -> Void
    @State private var locationSource = ProviderLocationSource()

    var body: some View {
        switch viewModel.state.screen {
        case "SCR-P02":
            P02ProfileView(
                state: viewModel.state, onExperienceChange: viewModel.updateExperience,
                onBioChange: viewModel.updateBio, onUpload: viewModel.uploadProfilePhoto,
                onNext: viewModel.openIdentity, onBack: onClose)
        case "SCR-P03":
            P03IdentityView(
                state: viewModel.state, onUpload: viewModel.uploadIdentity,
                onNext: { Task { await viewModel.loadCatalog() } }, onBack: onClose)
        case "SCR-P04":
            P04CatalogView(
                state: viewModel.state, onCategory: viewModel.toggleCategory,
                onSpecialty: viewModel.toggleSpecialty,
                onNext: { Task { await viewModel.loadCities() } }, onBack: onClose)
        case "SCR-P05":
            P05AreasView(
                state: viewModel.state, onCity: viewModel.selectCity, onArea: viewModel.toggleArea,
                onNext: viewModel.loadPayout, onBack: onClose)
        case "SCR-P06":
            P06PayoutView(
                state: viewModel.state, onMethod: viewModel.selectPayout,
                onDetailsChange: viewModel.updatePayoutDetails, onSubmit: viewModel.submit,
                onBack: onClose)
        case "SCR-P07":
            P07StatusView(state: viewModel.state, onAction: viewModel.handleAction, onBack: onClose)
        case "SCR-P08":
            P08HomeView(
                state: viewModel.state, onAvailabilityChange: viewModel.toggleAvailability,
                onAction: viewModel.handleAction,
                onRequest: { index in Task { await viewModel.openMarketRequest(index) } },
                onBack: onClose)
        case "SCR-P09":
            P09AvailableRequestView(
                state: viewModel.state,
                onAction: { action in Task { await viewModel.handleMarketRequestAction(action) } },
                onBack: { Task { await viewModel.loadHome() } })
        case "SCR-P10":
            P10OfferView(
                state: viewModel.state, onEtaSelect: viewModel.selectOfferETA,
                onDeductibleSelect: viewModel.selectOfferDeductible,
                onPriceChange: viewModel.updateOfferPrice,
                onIncludesChange: viewModel.updateOfferIncludes,
                onNoteChange: viewModel.updateOfferNote,
                onSubmit: viewModel.submitMarketOffer,
                onBack: viewModel.backToMarketRequest)
        case "SCR-P11":
            P11OffersView(
                state: viewModel.state,
                onOfferAction: { index, action in
                    Task { await viewModel.handleOfferListAction(index, action: action) }
                },
                onConfirmWithdraw: viewModel.confirmOfferWithdrawal,
                onCancelWithdraw: viewModel.cancelOfferWithdrawal,
                onBack: { Task { await viewModel.loadHome() } })
        case "SCR-P12":
            P12ConfirmedOrderView(
                state: viewModel.state, destination: viewModel.activeDestination,
                onAction: handleActiveAction, onBack: { Task { await viewModel.loadHome() } })
        case "SCR-P13":
            P13OnTheWayView(
                state: viewModel.state, destination: viewModel.activeDestination,
                onAction: handleActiveAction, onBack: { Task { await viewModel.loadHome() } })
        case "SCR-P14":
            P14ArrivedView(
                state: viewModel.state, destination: viewModel.activeDestination,
                onAction: handleActiveAction, onBack: { Task { await viewModel.loadHome() } })
        case "SCR-P15":
            P15InProgressView(
                state: viewModel.state, destination: viewModel.activeDestination,
                onAction: handleActiveAction, onBack: { Task { await viewModel.loadHome() } })
        case "SCR-P16":
            P16AwaitingPaymentView(
                state: viewModel.state, destination: viewModel.activeDestination,
                onAction: handleActiveAction, onBack: { Task { await viewModel.loadHome() } })
        case "SCR-P17":
            P17CustomerRatingView(
                state: viewModel.state, onRate: viewModel.selectCustomerRating,
                onSubmit: viewModel.submitCustomerRating,
                onBack: { Task { await viewModel.loadHome() } })
        case "SCR-P18":
            P18EarningsView(state: viewModel.state, onBack: { Task { await viewModel.loadHome() } })
        case "SCR-P19":
            P19ProfileView(
                state: viewModel.state,
                onExperienceChange: viewModel.updateWorkspaceExperience,
                onBioChange: viewModel.updateWorkspaceBio,
                onSpecialty: viewModel.toggleWorkspaceSpecialty,
                onArea: viewModel.toggleWorkspaceArea,
                onCaptionChange: viewModel.updatePortfolioCaption,
                onAvatar: viewModel.uploadWorkspaceAvatar,
                onPortfolio: viewModel.addWorkspacePortfolio,
                onDeletePortfolio: viewModel.deleteWorkspacePortfolio,
                onSupport: onOpenSupport,
                onSave: viewModel.saveWorkspaceProfile,
                onBack: { Task { await viewModel.loadHome() } })
        case "SCR-C32":
            // DEC-058 — the customer screen fed by provider-mode data, as C19 is.
            C32NotificationsView(
                state: viewModel.notificationsState,
                onOpen: { index in Task { await viewModel.openNotification(index: index) } },
                onOpenSettings: openNotificationSettings,
                onBack: { Task { await viewModel.loadHome() } })
        case "SCR-C19":
            C19ChatView(
                state: customerChatState(viewModel.state),
                onDraftChange: viewModel.updateChatDraft,
                onSend: { Task { await viewModel.sendChatMessage() } },
                onPhotoSelected: { upload in Task { await viewModel.sendChatPhoto(upload) } },
                onBack: { Task { await viewModel.leaveChat() } })
        case "SCR-P20":
            P20ProposalView(
                state: viewModel.state, onTypeSelect: viewModel.selectProposalType,
                onAmountChange: viewModel.updateProposalAmount,
                onReasonChange: viewModel.updateProposalReason,
                onOutsideReasonChange: viewModel.updateOutsidePriceGuideReason,
                onPickPhoto: viewModel.uploadProposalPhoto,
                onDeletePhoto: viewModel.deleteProposalPhoto,
                onSubmit: viewModel.submitProposal, onBack: viewModel.backToActiveOrder)
        case "SCR-P21":
            P21UnableView(
                state: viewModel.state, onReasonSelect: viewModel.selectUnableReason,
                onNoteChange: viewModel.updateUnableNote, onConfirm: viewModel.confirmUnable,
                onBack: viewModel.backToActiveOrder)
        default:
            P01IntroView(state: viewModel.state, onAction: viewModel.handleAction, onBack: onClose)
        }
    }

    private func handleActiveAction(_ action: String) {
        switch action {
        case "start_trip", "mark_arrived":
            locationSource.resolve { latitude, longitude in
                Task {
                    await viewModel.performLocationAction(action, latitude: latitude, longitude: longitude)
                }
            }
        // swiftlint:disable opening_brace
        case "call":
            if let phone = viewModel.activeCustomerPhone,
                let url = URL(string: "tel:\(phone)")
            {
                UIApplication.shared.open(url)
            }
        case "navigate":
            if let destination = viewModel.activeDestination,
                let url = URL(string: "http://maps.apple.com/?daddr=\(destination.0),\(destination.1)")
            {
                UIApplication.shared.open(url)
            }
        // swiftlint:enable opening_brace
        default:
            Task { await viewModel.handleOrderAction(action) }
        }
    }
}

private func customerChatState(_ state: ProviderUIState) -> CustomerUIState {
    CustomerUIState(
        screen: state.screen,
        phase: {
            switch state.phase {
            case .empty: return .empty
            case .loading: return .loading
            case .content: return .content
            case .error: return .error
            }
        }(),
        canContinue: state.canContinue, messageKey: state.messageKey,
        items: state.items, itemDetails: state.itemDetails, itemStates: state.itemStates,
        options: state.options, fieldValues: state.fieldValues, isBusy: state.isBusy)
}
