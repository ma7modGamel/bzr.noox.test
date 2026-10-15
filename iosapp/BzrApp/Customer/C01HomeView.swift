import BzrCore
import DesignSystem
import SwiftUI

/// SCR-C01 — Figma «Primo App» home 3002:270, component for component (DEC-073): pinned header (avatar, location,
/// notifications), greeting, search, hero banner, four services, the current request and the emergency row. Every
/// figure comes from the API; the hero photo is a design asset. The bottom navigation lives in the journey root.
public struct C01HomeView: View {
    let state: CustomerUIState
    let onOpen: (String) -> Void
    let onCategorySelect: (Int) -> Void
    let onOrderSelect: (Int) -> Void
    let onUrgent: () -> Void

    private static let homeTiles = 4
    private static let minutesPerHour = 60
    private static let minutesPerDay = 1_440

    public init(
        state: CustomerUIState, onOpen: @escaping (String) -> Void = { _ in },
        onCategorySelect: @escaping (Int) -> Void = { _ in },
        onOrderSelect: @escaping (Int) -> Void = { _ in },
        onUrgent: @escaping () -> Void = {}
    ) {
        self.state = state
        self.onOpen = onOpen
        self.onCategorySelect = onCategorySelect
        self.onOrderSelect = onOrderSelect
        self.onUrgent = onUrgent
    }

    private var name: String { state.fieldValues.first ?? "" }

    private var locationLabel: String {
        let label = state.fieldValues.count > 1 ? state.fieldValues[1] : ""
        return label.isEmpty ? bzrString("customer.home.location.empty") : label
    }

    private var greeting: String {
        name.isEmpty ? bzrString("customer.greeting.plain") : String(format: bzrString("customer.greeting"), name)
    }

    public var body: some View {
        CustomerScreen(title: bzrString("nav.home"), state: state, showsTopBar: false, header: AnyView(header)) {
            VStack(alignment: .leading, spacing: DesignSpace.xl) {
                VStack(alignment: .leading, spacing: DesignSpace.xs) {
                    BzrText(greeting, style: DesignType.screenTitle)
                    BzrText(bzrString("customer.home.subtitle"), style: DesignType.subtitle)
                }
                search
                hero
                services
                if !state.items.isEmpty { current }
                emergency
            }
        }
    }

    /// Avatar · location (C14) · notifications (C32, dot while any is unread).
    private var header: some View {
        HStack(spacing: DesignSpace.s) {
            // The app loads no remote images yet, so the avatar shows the name's initial (REPORT figma-b §ثابت).
            BzrText(String(name.trimmingCharacters(in: .whitespaces).prefix(1)), style: DesignType.homeSection.colored(DesignColors.primary600))
                .frame(width: DesignSize.avatarHeader, height: DesignSize.avatarHeader)
                .background(Circle().fill(DesignColors.primary50))
                .overlay(Circle().strokeBorder(DesignColors.avatarRing, lineWidth: DesignSize.controlStroke))
                .accessibilityLabel(name)
            Button {
                onOpen("SCR-C14")
            } label: {
                HStack(spacing: DesignSpace.xs) {
                    BzrIcon(.location, tint: DesignColors.primary600, size: DesignSize.iconSmall)
                    BzrText(locationLabel, style: DesignType.caption.colored(DesignColors.navy900), lineLimit: 1)
                    BzrIcon(.expandMore, tint: DesignColors.slate400, size: DesignSize.iconSmall)
                }
                .frame(maxWidth: .infinity, minHeight: DesignSize.touchTargetMin)
            }
            .buttonStyle(BzrPressStyle())
            Button {
                onOpen("SCR-C32")
            } label: {
                BzrIcon(.bell, tint: DesignColors.navy900)
                    .frame(width: DesignSize.touchTargetMin, height: DesignSize.touchTargetMin)
                    .overlay(alignment: .topLeading) {
                        if state.unreadNotifications > 0 {
                            Circle().fill(DesignColors.amber500)
                                .overlay(Circle().strokeBorder(DesignColors.surfaceAlt, lineWidth: DesignSize.controlStroke))
                                .frame(width: DesignSize.notificationDot, height: DesignSize.notificationDot)
                                .padding(DesignSpace.m)
                        }
                    }
            }
            .buttonStyle(BzrPressStyle())
            .accessibilityLabel(bzrString("notifications.title"))
        }
        .padding(.horizontal, DesignSpace.screenHorizontal)
        .frame(height: DesignSize.homeHeaderHeight)
        .background(DesignColors.surfaceAlt.bzrShadow(DesignShadows.card))
    }

    /// Opens step 1 (C03), where the catalog is searched.
    private var search: some View {
        Button {
            onOpen("SCR-C03")
        } label: {
            HStack(spacing: DesignSpace.s) {
                BzrIcon(.search, tint: DesignColors.slate500)
                BzrText(bzrString("customer.home.search"), style: DesignType.placeholder, lineLimit: 1)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, DesignSpace.l)
            .frame(height: DesignSize.homeSearchHeight)
            .background(RoundedRectangle(cornerRadius: DesignRadius.homeSearch).fill(DesignColors.surfaceAlt2))
        }
        .buttonStyle(BzrPressStyle())
    }

    private var hero: some View {
        ZStack(alignment: .bottomLeading) {
            DesignPhoto("img_home_hero")
                .frame(maxWidth: .infinity)
                .frame(height: DesignSize.heroBannerHeight)
                .clipped()
            LinearGradient(
                stops: [
                    .init(color: DesignColors.heroScrim, location: 0),
                    .init(color: DesignColors.heroScrimMid, location: 0.5),
                    .init(color: DesignColors.heroScrim.opacity(0), location: 1),
                ], startPoint: .bottom, endPoint: .top)
            VStack(alignment: .leading, spacing: DesignSpace.xs) {
                BzrText(bzrString("customer.home.hero.badge"), style: DesignType.statusPill.colored(DesignColors.heroBadgeText))
                    .padding(.horizontal, DesignSpace.s)
                    .background(Capsule().fill(DesignColors.heroBadgeBg))
                BzrText(bzrString("customer.home.hero.title"), style: DesignType.heroOnImage)
                BzrText(
                    bzrString(state.showPricing ? "customer.home.hero.body.marketplace" : "customer.home.hero.body.staff"),
                    style: DesignType.heroBody)
                PrimaryButton(
                    text: bzrString("customer.home.hero.action"), state: .normal, icon: .arrowForward, compact: true,
                    onClick: { onOpen("SCR-C03") }
                )
                .fixedSize()
                .padding(.top, DesignSpace.s)
            }
            .padding(DesignSpace.xl)
        }
        .frame(height: DesignSize.heroBannerHeight)
        .clipShape(RoundedRectangle(cornerRadius: DesignRadius.heroCard))
        .bzrShadow(DesignShadows.card)
    }

    private var services: some View {
        VStack(alignment: .leading, spacing: DesignSpace.m) {
            HStack {
                BzrText(bzrString("customer.home.services"), style: DesignType.homeSection)
                Spacer(minLength: DesignSpace.s)
                LinkButton(text: bzrString("customer.home.services.all"), onClick: { onOpen("SCR-C03") })
            }
            HStack(alignment: .top, spacing: DesignSpace.s) {
                ForEach(Array(state.options.prefix(Self.homeTiles).enumerated()), id: \.offset) { index, label in
                    Button {
                        onCategorySelect(index)
                    } label: {
                        VStack(spacing: DesignSpace.s) {
                            CategoryGlyph(state.optionIcons.indices.contains(index) ? state.optionIcons[index] : nil, size: DesignSize.icon)
                                .frame(width: DesignSize.homeTileWell, height: DesignSize.homeTileWell)
                                .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
                                .bzrShadow(DesignShadows.card)
                            BzrText(label, style: DesignType.tileLabel, alignment: .center, lineLimit: 2)
                        }
                        .padding(DesignSpace.m)
                        .frame(maxWidth: .infinity)
                        .background(RoundedRectangle(cornerRadius: DesignRadius.homeTile).fill(DesignColors.surfaceAlt2))
                    }
                    .buttonStyle(BzrPressStyle())
                    .accessibilityLabel(label)
                }
            }
        }
    }

    /// The first order is the current request; offers show while it is open (DEC-073).
    private var current: some View {
        let open = state.optionCodes.first == "OPEN"
        let offers = state.orderOfferCounts.first ?? 0
        let lowest = state.orderLowestOffers.first ?? ""
        return VStack(alignment: .leading, spacing: DesignSpace.s) {
            BzrText(bzrString("customer.home.current"), style: DesignType.homeSection)
            VStack(alignment: .leading, spacing: DesignSpace.s) {
                HStack(alignment: .center, spacing: DesignSpace.m) {
                    CategoryGlyph(state.orderIcons.first, size: DesignSize.icon)
                        .frame(width: DesignSize.orderWell, height: DesignSize.orderWell)
                        .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.surfaceContainer))
                    VStack(alignment: .leading, spacing: 0) {
                        BzrText(state.items.first ?? "", style: DesignType.homeSection)
                        let published = age(state.orderAgeMinutes.first ?? -1)
                        if !published.isEmpty { BzrText(published, style: DesignType.hint) }
                    }
                    Spacer(minLength: 0)
                    if let status = state.itemStates.first, !status.isEmpty {
                        BzrText(status, style: DesignType.statusPill)
                            .padding(.horizontal, DesignSpace.s)
                            .background(Capsule().fill(DesignColors.statusPillBg))
                            .frame(maxHeight: .infinity, alignment: .top)
                    }
                }
                .fixedSize(horizontal: false, vertical: true)
                if open && offers > 0 {
                    HStack(spacing: DesignSpace.s) {
                        BzrIcon(.tag, tint: DesignColors.primary600, size: DesignSize.iconSmall)
                        BzrText(
                            bzrPlural("customer.home.offers.new", count: offers),
                            style: DesignType.linkSmall.colored(DesignColors.slate400))
                        Spacer(minLength: DesignSpace.s)
                        if !lowest.isEmpty {
                            BzrText(bzrString("customer.home.offers.lowest"), style: DesignType.hint)
                            BzrText(
                                BzrFormatter.fill(bzrString("customer.home.offers.amount"), values: [("amount", lowest)]),
                                style: DesignType.amountSmall)
                        }
                    }
                    .padding(.horizontal, DesignSpace.m)
                    .padding(.vertical, DesignSpace.xs)
                    .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.surfaceAlt2))
                }
                PrimaryButton(
                    text: bzrString(open ? "customer.home.offers.action" : "customer.home.order.action"),
                    state: .normal, icon: .arrowForward, compact: true, onClick: { onOrderSelect(0) })
            }
            .padding(DesignSpace.m)
            .background(RoundedRectangle(cornerRadius: DesignRadius.currentCard).fill(DesignColors.surface))
            .bzrShadow(DesignShadows.card)
        }
    }

    private var emergency: some View {
        Button(action: onUrgent) {
            HStack(spacing: DesignSpace.s) {
                BzrIcon(.siren, tint: DesignColors.amber500, size: DesignSize.iconSmall)
                    .frame(width: DesignSize.emergencyIcon, height: DesignSize.emergencyIcon)
                    .background(Circle().fill(DesignColors.amber50))
                BzrText(bzrString("customer.home.urgent.title"), style: DesignType.tileTitle)
                Spacer(minLength: DesignSpace.s)
                BzrText(bzrString("customer.home.urgent.action"), style: DesignType.urgentLink)
                BzrIcon(.chevron, tint: DesignColors.warningText, size: DesignSize.iconSmall)
            }
            .padding(.horizontal, DesignSpace.l)
            .padding(.vertical, DesignSpace.m)
            .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surfaceAlt2))
        }
        .buttonStyle(BzrPressStyle())
    }

    private func age(_ minutes: Int) -> String {
        switch minutes {
        case ..<0: ""
        case ..<1: bzrString("customer.home.published.now")
        case ..<Self.minutesPerHour:
            bzrPlural("customer.home.published.minutes", count: minutes)
        case ..<Self.minutesPerDay:
            bzrPlural("customer.home.published.hours", count: minutes / Self.minutesPerHour)
        default:
            bzrPlural("customer.home.published.days", count: minutes / Self.minutesPerDay)
        }
    }
}
