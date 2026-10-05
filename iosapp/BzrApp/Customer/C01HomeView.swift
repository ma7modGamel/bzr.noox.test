import BzrCore
import DesignSystem
import SwiftUI

public struct C01HomeView: View {
    let state: CustomerUIState
    let onOpen: (String) -> Void
    let onCategorySelect: (Int) -> Void
    let onOrderSelect: (Int) -> Void
    let onUrgent: () -> Void

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
        let name = state.fieldValues.first ?? ""
        return name.isEmpty
            ? bzrString("customer.greeting.plain") : String(format: bzrString("customer.greeting"), name)
    }

    public var body: some View {
        CustomerScreen(title: bzrString("nav.home"), state: state, showsTopBar: false) {
            header
            heroCard
            urgentCard
            ScreenHeading(bzrString("customer.categories.title"))
            BzrServiceGrid {
                ForEach(Array(state.options.enumerated()), id: \.offset) { index, label in
                    Button(
                        action: { onCategorySelect(index) },
                        label: {
                            SelectableTile(
                                text: label, icon: .home,
                                state: index == state.selectedIndex ? .selected : .unselected,
                                categoryIcon: state.optionIcons.indices.contains(index)
                                    ? state.optionIcons[index] : nil)
                        }
                    )
                    .buttonStyle(BzrPressStyle())
                }
            }
            ScreenHeading(bzrString("customer.current_orders.title"))
            if state.phase == .empty {
                EmptyState(title: bzrString("empty.title"), body: bzrString("empty.body"))
            } else {
                ForEach(Array(state.items.enumerated()), id: \.offset) { index, title in
                    OrderCard(
                        title: title,
                        subtitle: state.itemDetails.indices.contains(index) ? state.itemDetails[index] : "",
                        status: state.itemStates.indices.contains(index) ? state.itemStates[index] : "",
                        onClick: { onOrderSelect(index) })
                }
            }
        }
    }

    /// DEC-063: location (opens C14) and notifications (C32), then the avatar and greeting.
    private var header: some View {
        VStack(alignment: .leading, spacing: DesignSpace.l) {
            HStack(spacing: DesignSpace.s) {
                Button {
                    onOpen("SCR-C14")
                } label: {
                    HStack(spacing: DesignSpace.xs) {
                        BzrGradientIcon(.location)
                        BzrText(locationLabel, style: DesignType.body.weighted(DesignFont.bold), lineLimit: 1)
                    }
                    .frame(minHeight: DesignSize.touchTargetMin)
                }
                .buttonStyle(BzrPressStyle())
                Spacer(minLength: DesignSpace.s)
                Button {
                    onOpen("SCR-C32")
                } label: {
                    BzrGradientIcon(.bell)
                        .frame(width: DesignSize.touchTargetMin, height: DesignSize.touchTargetMin)
                        .background(Circle().fill(DesignGradients.iconWell))
                }
                .buttonStyle(BzrPressStyle())
                .accessibilityLabel(bzrString("notifications.title"))
            }
            HStack(spacing: DesignSpace.m) {
                if !name.isEmpty {
                    BzrText(String(name.prefix(1)), style: DesignType.screenTitle.colored(DesignColors.onPrimary))
                        .frame(width: DesignSize.heroIcon, height: DesignSize.heroIcon)
                        .background(Circle().fill(DesignGradients.iconBrand))
                        .accessibilityHidden(true)
                }
                VStack(alignment: .leading, spacing: DesignSpace.xs) {
                    BzrText(greeting, style: DesignType.screenTitle).accessibilityAddTraits(.isHeader)
                    BzrText(bzrString("customer.home.subtitle"), style: DesignType.secondary)
                }
            }
        }
    }

    /// The gradient request card (C03); its body follows the operating mode (39).
    private var heroCard: some View {
        Button {
            onOpen("SCR-C03")
        } label: {
            HStack(spacing: DesignSpace.m) {
                BzrIcon(.clipboardPlus, tint: DesignColors.onPrimary, size: DesignSize.tileIcon)
                    .frame(width: DesignSize.heroIcon, height: DesignSize.heroIcon)
                    .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.onHeroWell))
                VStack(alignment: .leading, spacing: DesignSpace.xs) {
                    BzrText(
                        bzrString("customer.home.hero.title"), style: DesignType.screenTitle.colored(DesignColors.onPrimary))
                    BzrText(
                        bzrString(
                            state.showPricing ? "customer.home.hero.body.marketplace" : "customer.home.hero.body.staff"),
                        style: DesignType.secondary.colored(DesignColors.onHeroBody))
                }
                Spacer(minLength: 0)
                BzrIcon(.chevron, tint: DesignColors.onPrimary)
                    .flipsForRightToLeftLayoutDirection(true)
            }
            .padding(DesignSpace.l)
            .background(RoundedRectangle(cornerRadius: DesignRadius.heroCard).fill(DesignGradients.hero))
            .bzrShadow(DesignShadows.brand)
        }
        .buttonStyle(BzrPressStyle())
        .accessibilityElement(children: .combine)
    }

    /// Urgent card (DEC-063): the request flow with "now" preselected.
    private var urgentCard: some View {
        Button(
            action: onUrgent,
            label: {
                HStack(spacing: DesignSpace.m) {
                    BzrGradientIcon(.siren, gradient: DesignGradients.urgentButton, size: DesignSize.tileIcon)
                    VStack(alignment: .leading, spacing: DesignSpace.xxs) {
                        BzrText(
                            bzrString("customer.home.urgent.title"), style: DesignType.cardTitle.colored(DesignColors.urgentText))
                        BzrText(
                            bzrString("customer.home.urgent.body"), style: DesignType.caption.colored(DesignColors.urgentText))
                    }
                    Spacer(minLength: 0)
                    BzrText(
                        bzrString("customer.home.urgent.action"),
                        style: DesignType.body.weighted(DesignFont.bold).colored(DesignColors.urgentText), lineLimit: 1
                    )
                    .padding(.horizontal, DesignSpace.l)
                    .frame(minHeight: DesignSize.touchTargetMin)
                    .background(RoundedRectangle(cornerRadius: DesignRadius.button).fill(DesignGradients.urgentButton))
                }
                .padding(DesignSpace.l)
                .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignGradients.urgent))
            }
        )
        .buttonStyle(BzrPressStyle())
        .accessibilityElement(children: .combine)
    }
}
