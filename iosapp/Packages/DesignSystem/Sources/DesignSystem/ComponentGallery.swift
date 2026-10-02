import SwiftUI

// Mirror of ComponentGallery.kt: same pages, same order, same keys, same fixture data.

public var galleryPageCount: Int { GalleryFixtures.snapshotPages.count }

private func s(_ key: String) -> String { bzrString(key) }

public struct ComponentGalleryApp: View {
    @State private var page = 0

    public init() {}

    public var body: some View {
        BzrTheme {
            VStack(spacing: 0) {
                GalleryPage(page: page)
                    .frame(maxHeight: .infinity)
                HStack(spacing: DesignSpace.s) {
                    SecondaryButton(text: s("gallery.back"), enabled: page > 0) { if page > 0 { page -= 1 } }
                    PrimaryButton(
                        text: s("gallery.next"), state: page == galleryPageCount - 1 ? .disabled : .normal
                    ) {
                        if page < galleryPageCount - 1 { page += 1 }
                    }
                }
                .padding(DesignSpace.screenHorizontal)
            }
            .background(DesignColors.surface)
        }
    }
}

/// `snapshot` replaces the ScrollView with a fixed viewport that shows the top or the bottom of the page,
/// the same frames Compose shows with the scroll value at 0 or clamped at its maximum.
public struct GalleryPage: View {
    private let page: Int
    private let anchorBottom: Bool
    private let snapshot: Bool

    public init(page: Int, anchorBottom: Bool = false, snapshot: Bool = false) {
        self.page = page
        self.anchorBottom = anchorBottom
        self.snapshot = snapshot
    }

    public var body: some View {
        BzrTheme {
            Group {
                if snapshot {
                    AnchoredViewport(anchorBottom: anchorBottom) { content }.clipped()
                } else {
                    ScrollView { content }
                }
            }
            .background(DesignColors.surface)
        }
    }

    private var content: some View {
        VStack(alignment: .leading, spacing: DesignSpace.m) {
            AppTopBar(title: s("gallery.title"), actionIcon: .more, actionLabel: s("a11y.more"))
            VStack(alignment: .leading, spacing: DesignSpace.m) {
                pageContent
            }
            .padding(.horizontal, DesignSpace.screenHorizontal)
        }
        .padding(.bottom, DesignSpace.sectionGap)
    }

    @ViewBuilder private var pageContent: some View {
        switch page {
        case 0: GalleryButtonsPage()
        case 1: GalleryNavigationPage()
        case 2: GallerySelectionPage()
        case 3: GalleryFieldsPage()
        case 4: GalleryCardsPage()
        case 5: GalleryProviderPage()
        case 6: GalleryTrackingPage()
        case 7: GalleryFeedbackPage()
        case 8: GalleryChatPage()
        case 9: GalleryMediaAndSheetPage()
        default: GalleryTextAreaPage()
        }
    }
}

/// Lays its single child out at its ideal height and pins it to the top or bottom edge.
private struct AnchoredViewport: Layout {
    let anchorBottom: Bool

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        proposal.replacingUnspecifiedDimensions()
    }

    func placeSubviews(
        in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()
    ) {
        guard let child = subviews.first else { return }
        let size = child.sizeThatFits(ProposedViewSize(width: bounds.width, height: nil))
        let y = anchorBottom ? bounds.minY + min(0, bounds.height - size.height) : bounds.minY
        child.place(
            at: CGPoint(x: bounds.minX, y: y), anchor: .topLeading,
            proposal: ProposedViewSize(width: bounds.width, height: size.height))
    }
}

private struct ComponentTitle: View {
    let key: String
    var body: some View { BzrText(s(key), style: DesignType.sectionTitle) }
}

private struct GalleryButtonsPage: View {
    var body: some View {
        ComponentTitle(key: "gallery.component.primary_button")
        PrimaryButton(text: s("gallery.state.normal"))
        PrimaryButton(text: s("gallery.state.pressed"), state: .pressed)
        HStack(spacing: DesignSpace.s) {
            PrimaryButton(text: s("gallery.state.disabled"), state: .disabled)
            PrimaryButton(text: s("gallery.state.loading"), state: .loading)
        }
        ComponentTitle(key: "gallery.component.secondary_button")
        SecondaryButton(text: s("button.secondary"))
        SecondaryButton(text: s("gallery.state.disabled"), enabled: false)
        ComponentTitle(key: "gallery.component.danger_text_button")
        HStack(spacing: DesignSpace.m) {
            DangerTextButton(text: s("button.danger"))
            IconSquareButton(label: s("button.chat"))
        }
    }
}

private struct GalleryNavigationPage: View {
    var body: some View {
        ComponentTitle(key: "gallery.component.app_top_bar")
        AppTopBar(title: s("summary.title"), actionIcon: .edit, actionLabel: s("a11y.edit"))
        ComponentTitle(key: "gallery.component.navigation")
        BottomNav(
            items: [
                (.home, s("nav.home")), (.orders, s("nav.orders")), (.messages, s("nav.messages")),
                (.account, s("nav.account")),
            ],
            selectedIndex: 0
        )
        DrawerMenu(
            name: s("drawer.name"),
            rating: BzrFormat.number(GalleryFixtures.offerRating),
            verifiedLabel: s("drawer.verified"),
            rows: [
                (.home, s("nav.home")), (.orders, s("nav.orders")), (.messages, s("nav.messages")),
                (.info, s("menu.help")),
            ],
            action: s("menu.provider_mode")
        )
    }
}

private struct GallerySelectionPage: View {
    var body: some View {
        ComponentTitle(key: "gallery.component.step_indicator")
        StepIndicator(
            current: GalleryFixtures.stepCurrent,
            total: GalleryFixtures.stepTotal,
            label: BzrFormat.fill(
                s("format.step"),
                [
                    ("current", BzrFormat.number(GalleryFixtures.stepCurrent)),
                    ("total", BzrFormat.number(GalleryFixtures.stepTotal)),
                ])
        )
        ComponentTitle(key: "gallery.component.selectable_tile")
        HStack(spacing: DesignSpace.cardGap) {
            SelectableTile(text: s("tile.plumbing"), icon: .home, state: .selected)
            SelectableTile(text: s("tile.electricity"), icon: .warning, state: .unselected)
            SelectableTile(text: s("tile.disabled"), icon: .info, state: .disabled)
        }
        ComponentTitle(key: "gallery.component.selectable_chip")
        HStack(spacing: DesignSpace.cardGap) {
            SelectableChip(text: s("chip.today"), state: .selected).frame(maxWidth: .infinity)
            SelectableChip(text: s("chip.tomorrow"), state: .unselected).frame(maxWidth: .infinity)
            SelectableChip(text: s("chip.unavailable"), state: .disabled).frame(maxWidth: .infinity)
        }
        Group {
            ComponentTitle(key: "gallery.component.radio_card")
            RadioCard(title: s("radio.now.title"), body: s("radio.now.body"), selected: true)
            RadioCard(title: s("radio.scheduled.title"), body: s("radio.scheduled.body"), selected: false)
            ComponentTitle(key: "gallery.component.check_row")
            CheckRow(text: s("check.materials"), checked: true)
            CheckRow(text: s("check.materials"), checked: false)
        }
    }
}

private func hasValue(_ state: FieldVisualState) -> Bool { state != .empty && state != .error }
private func errorText(_ state: FieldVisualState) -> String? {
    state == .error ? s("field.error") : nil
}

private struct GalleryFieldsPage: View {
    var body: some View {
        ComponentTitle(key: "gallery.component.fields")
        ForEach(FieldVisualState.allCases, id: \.self) { state in
            AppTextField(
                label: s("field.label"), placeholder: s("field.placeholder"),
                value: hasValue(state) ? s("field.value") : "", state: state, error: errorText(state))
        }
        ForEach(FieldVisualState.allCases, id: \.self) { state in
            AmountField(
                label: s("field.amount.label"), placeholder: s("field.amount.placeholder"),
                value: hasValue(state) ? BzrFormat.number(GalleryFixtures.offerPrice) : "",
                currency: s("common.currency.egp"), state: state, error: errorText(state),
                optionalLabel: s("common.optional")
            )
        }
    }
}

private struct GalleryTextAreaPage: View {
    var body: some View {
        ComponentTitle(key: "gallery.component.text_area")
        ForEach(FieldVisualState.allCases, id: \.self) { state in
            TextAreaField(
                label: s("field.label"), placeholder: s("field.placeholder"),
                value: hasValue(state) ? s("field.value") : "", state: state, error: errorText(state))
        }
    }
}

private struct GalleryCardsPage: View {
    private let rating = BzrFormat.number(GalleryFixtures.offerRating)
    private let services = BzrFormat.fill(s("format.services"), n: GalleryFixtures.offerServices)
    private let price = BzrFormat.fill(s("format.amount"), n: GalleryFixtures.offerPrice)
    private let eta = BzrFormat.fill(s("format.eta"), n: GalleryFixtures.offerEtaMinutes)

    private func offer(_ variant: OfferVariant, detail: String, priceCaption: String? = nil)
        -> OfferCard
    {
        OfferCard(
            provider: s("offer.provider"), rating: rating, services: services, price: price,
            detail: detail,
            badge: s("badge.top_rated"), button: s("offer.select"), chatLabel: s("button.chat"),
            variant: variant, priceCaption: priceCaption
        )
    }

    var body: some View {
        ComponentTitle(key: "gallery.component.summary_card")
        SummaryCard(
            title: s("summary.title"),
            rows: [(.orders, s("summary.service")), (.clock, s("summary.visit"))],
            editText: s("common.edit"))
        ComponentTitle(key: "gallery.component.offer_card")
        offer(.execution, detail: eta)
        offer(.inspection, detail: eta, priceCaption: s("offer.inspection_fee"))
        offer(.scheduled, detail: s("offer.scheduled"))
    }
}

private struct GalleryProviderPage: View {
    private let rating = BzrFormat.number(GalleryFixtures.offerRating)
    private let services = BzrFormat.fill(s("format.services"), n: GalleryFixtures.offerServices)

    var body: some View {
        ComponentTitle(key: "gallery.component.sort_chips")
        SortChips(
            title: s("sort.title"),
            labels: [s("sort.top_rated"), s("sort.lowest_price"), s("sort.fastest")], selectedIndex: 0)
        HStack(spacing: DesignSpace.s) {
            Badge(text: s("badge.top_rated"), kind: .highlight, icon: .starFilled)
            Badge(text: s("badge.best_price"), kind: .highlight)
            Badge(text: s("badge.fastest"), kind: .highlight)
            Badge(text: s("order.status"), kind: .status)
        }
        ComponentTitle(key: "gallery.component.provider_header")
        ProviderHeader(name: s("offer.provider"), rating: rating, services: services, size: .small)
        ProviderHeader(
            name: s("offer.provider"), rating: rating, services: services, size: .medium,
            verifiedLabel: s("provider.verified"))
        ProviderHeader(
            name: s("offer.provider"), rating: rating, services: services, size: .large,
            verifiedLabel: s("provider.verified"))
        Group {
            ComponentTitle(key: "gallery.component.stats")
            StatRow(items: [
                StatItem(icon: .star, value: rating, label: s("stat.rating")),
                StatItem(
                    icon: .orders, value: BzrFormat.number(GalleryFixtures.offerServices),
                    label: s("stat.services")),
                StatItem(
                    icon: .clock, value: BzrFormat.number(GalleryFixtures.providerYears),
                    label: s("stat.years")),
            ])
            RatingBars(
                items:
                    Array(
                        zip(
                            [s("rating.quality"), s("rating.commitment"), s("rating.communication")],
                            GalleryFixtures.ratingBars)),
                max: GalleryFixtures.ratingMaxStars
            )
            ReviewCard(
                name: s("review.name"), body: s("review.body"), verified: s("review.verified"),
                time: s("review.time"), tag: s("review.tag"), rating: GalleryFixtures.ratingMaxStars
            )
            ComponentTitle(key: "gallery.component.sticky_action_bar")
            StickyActionBar(
                priceLabel: s("sticky.price_label"),
                price: BzrFormat.fill(s("format.amount"), n: GalleryFixtures.offerPrice),
                action: s("offer.select"),
                chatLabel: s("button.chat"))
        }
    }
}

private struct GalleryTrackingPage: View {
    var body: some View {
        ComponentTitle(key: "gallery.component.tracking")
        EtaCard(
            value: BzrFormat.number(GalleryFixtures.etaMinutes), unit: s("unit.minute"),
            title: s("eta.title"), subtitle: s("eta.subtitle"))
        MapCard(title: s("map.title"))
        StatusStepper(steps: [
            (s("status.confirmed"), .done), (s("status.on_the_way"), .done),
            (s("status.arrived"), .onHold),
            (s("status.in_progress"), .pending), (s("status.payment"), .pending),
            (s("status.closed"), .pending),
        ])
        WarningBox(text: s("status.reviewing"))
        ComponentTitle(key: "gallery.component.order_card")
        OrderCard(title: s("order.title"), subtitle: s("order.subtitle"), status: s("order.status"))
    }
}

private struct GalleryFeedbackPage: View {
    var body: some View {
        ComponentTitle(key: "gallery.component.banners")
        InfoBanner(text: s("banner.info"))
        WarningBox(text: s("banner.warning"))
        OfflineBanner(text: s("offline.message"))
        ComponentTitle(key: "gallery.component.countdown")
        HStack(spacing: 0) {
            Countdown(seconds: GalleryFixtures.countdownNormalSeconds, label: s("countdown.label")).frame(
                maxWidth: .infinity)
            Countdown(seconds: GalleryFixtures.countdownUrgentSeconds, label: s("countdown.label")).frame(
                maxWidth: .infinity)
        }
        ComponentTitle(key: "gallery.component.feedback")
        EmptyState(title: s("empty.title"), body: s("empty.body"), action: s("action.republish"))
        LoadingSkeleton()
        ErrorState(title: s("error.title"), body: s("error.body"), retry: s("common.retry"))
    }
}

private struct GalleryChatPage: View {
    var body: some View {
        ComponentTitle(key: "gallery.component.chat")
        ChatBubble(text: s("chat.sent"), sent: true)
        ChatBubble(text: s("chat.received"), sent: false)
        ChatBubble(text: s("media.photo"), sent: false, kind: .image)
        ChatBubble(text: s("chat.blocked"), sent: false, kind: .blocked)
        ChatInput(placeholder: s("chat.placeholder"))
    }
}

private struct GalleryMediaAndSheetPage: View {
    var body: some View {
        ComponentTitle(key: "gallery.component.media")
        MediaThumb(
            title: s("media.photo"), stateLabel: s("media.uploading"), kind: .photo, state: .uploading)
        MediaThumb(
            title: s("media.video"), stateLabel: s("media.uploaded"), kind: .video, state: .uploaded)
        MediaThumb(title: s("media.audio"), stateLabel: s("media.failed"), kind: .audio, state: .failed)
        ComponentTitle(key: "gallery.component.bottom_sheet")
        AppBottomSheet(title: s("sheet.title"), body: s("sheet.body"), action: s("button.primary"))
            .padding(.top, DesignSpace.xxl)
            .background(DesignColors.scrim)
    }
}
