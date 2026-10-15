import BzrCore
import DesignSystem
import SwiftUI

/// SCR-C25 — Figma «طلباتي» 3002:3889 (DEC-076): «الحالية / السابقة / الملغاة» segments, then one card per order —
/// status and number, the problem with its category well and place, the technician strip (call) or, while open, the
/// offer figures, then the approved total with chat and «متابعة الخدمة», or «عرض العروض». `options` rows are
/// "number|place|provider|verified|rating|amount|chat|phone".
public struct C25OrdersView: View {
    let state: CustomerUIState
    let onTabSelect: (Int) -> Void
    let onOrderSelect: (Int) -> Void
    let onChat: (Int) -> Void

    public init(
        state: CustomerUIState,
        onTabSelect: @escaping (Int) -> Void = { _ in },
        onOrderSelect: @escaping (Int) -> Void = { _ in },
        onChat: @escaping (Int) -> Void = { _ in }
    ) {
        self.state = state
        self.onTabSelect = onTabSelect
        self.onOrderSelect = onOrderSelect
        self.onChat = onChat
    }

    private static let ended: Set<String> = ["CLOSED", "CANCELLED", "EXPIRED"]
    private static let tabs = ["orders.current", "orders.past", "orders.cancelled"]
    private var tab: Int { min(max(state.selectedOptionIndex, 0), 2) }

    public var body: some View {
        CustomerScreen(title: bzrString("orders.title"), state: state) {
            segments
            if state.phase == .empty {
                EmptyState(
                    title: bzrString(["orders.current.empty.title", "orders.past.empty.title", "orders.cancelled.empty.title"][tab]),
                    body: bzrString(["orders.current.empty.body", "orders.past.empty.body", "orders.cancelled.empty.body"][tab]))
            }
            ForEach(state.items.indices, id: \.self) { index in
                orderCard(index)
            }
            if state.isBusy {
                BzrText(bzrString("orders.loading_more"), style: DesignType.secondary)
            }
        }
    }

    private var segments: some View {
        HStack(spacing: 0) {
            ForEach(Array(Self.tabs.enumerated()), id: \.offset) { index, key in
                let count = index == tab && !state.items.isEmpty ? " (\(BzrFormat.number(state.items.count)))" : ""
                Button(action: { if index != tab { onTabSelect(index) } }) {
                    BzrText(
                        bzrString(key) + count,
                        style: DesignType.tileTitle.colored(index == tab ? DesignColors.primary600 : DesignColors.slate500),
                        alignment: .center
                    )
                    .frame(maxWidth: .infinity, minHeight: DesignSize.chipHeight)
                    .background(Capsule().fill(index == tab ? DesignColors.surface : .clear))
                }
                .buttonStyle(BzrPressStyle())
                .accessibilityAddTraits(index == tab ? .isSelected : [])
            }
        }
        .padding(DesignSpace.xs)
        .background(Capsule().fill(DesignColors.surfaceContainer))
    }

    private func orderCard(_ index: Int) -> some View {
        let row = (state.options.indices.contains(index) ? state.options[index] : "").components(separatedBy: "|")
        func field(_ at: Int) -> String { row.indices.contains(at) ? row[at] : "" }
        let status = state.optionCodes.indices.contains(index) ? state.optionCodes[index] : ""
        let offers = state.orderOfferCounts.indices.contains(index) ? state.orderOfferCounts[index] : 0
        let lowest = state.orderLowestOffers.indices.contains(index) ? state.orderLowestOffers[index] : ""
        let icon = state.orderIcons.indices.contains(index) ? state.orderIcons[index] : ""
        return VStack(alignment: .leading, spacing: DesignSpace.m) {
            HStack {
                let badgeKind = BadgeKind.forOrderStatus(status)
                Badge(
                    text: state.itemStates.indices.contains(index) ? state.itemStates[index] : "",
                    kind: badgeKind, icon: badgeKind.statusIcon)
                Spacer(minLength: DesignSpace.s)
                if !field(0).isEmpty {
                    BzrText(BzrFormatter.fill(bzrString("orders.number"), values: [("number", field(0))]), style: DesignType.hint)
                }
            }
            HStack(spacing: DesignSpace.m) {
                VStack(alignment: .leading, spacing: DesignSpace.xs) {
                    BzrText(state.items[index], style: DesignType.homeSection)
                    if !field(1).isEmpty {
                        HStack(spacing: DesignSpace.xs) {
                            BzrIcon(.location, tint: DesignColors.slate400, size: DesignSize.iconSmall)
                            BzrText(field(1), style: DesignType.hint)
                        }
                    }
                }
                Spacer(minLength: 0)
                Group {
                    if icon.isEmpty { BzrIcon(.wrench, tint: DesignColors.primary600) } else { CategoryGlyph(icon, size: DesignSize.icon) }
                }
                .frame(width: DesignSize.homeTileWell, height: DesignSize.homeTileWell)
                .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.surfaceAlt2))
            }
            if !field(2).isEmpty {
                providerStrip(name: field(2), verified: field(3) == "verified", rating: field(4))
            }
            if status == "OPEN" && offers > 0 {
                offersStrip(count: offers, lowest: lowest)
            }
            if !field(5).isEmpty {
                HStack(spacing: DesignSpace.s) {
                    VStack(alignment: .leading, spacing: 0) {
                        BzrText(bzrString("orders.total"), style: DesignType.hint)
                        BzrText(
                            BzrFormatter.fill(bzrString("customer.home.offers.amount"), values: [("amount", field(5))]),
                            style: DesignType.amountSmall)
                    }
                    Spacer(minLength: DesignSpace.s)
                    if field(6) == "chat" {
                        IconSquareButton(label: bzrString("button.chat"), icon: .messages) { onChat(index) }
                    }
                    if !Self.ended.contains(status) {
                        PrimaryButton(text: bzrString("orders.follow"), icon: .arrowForward, compact: true) { onOrderSelect(index) }
                            .fixedSize()
                    }
                }
            } else if status == "OPEN" {
                HStack {
                    SecondaryButton(text: bzrString("customer.home.offers.action"), icon: .arrowForward) { onOrderSelect(index) }
                        .fixedSize()
                    Spacer(minLength: 0)
                }
            }
        }
        .padding(DesignSpace.l)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
        .bzrShadow(DesignShadows.card)
        .contentShape(Rectangle())
        .onTapGesture { onOrderSelect(index) }
        .accessibilityAddTraits(.isButton)
    }

    private func providerStrip(name: String, verified: Bool, rating: String) -> some View {
        HStack(spacing: DesignSpace.s) {
            VStack(alignment: .leading, spacing: DesignSpace.xs) {
                HStack(spacing: DesignSpace.xs) {
                    BzrText(name, style: DesignType.tileTitle)
                    if verified { BzrIcon(.verified, tint: DesignColors.primary600, size: DesignSize.iconSmall) }
                }
                if !rating.isEmpty {
                    HStack(spacing: DesignSpace.xs) {
                        BzrIcon(.starFilled, tint: DesignColors.star, size: DesignSize.iconSmall)
                        BzrText(rating, style: DesignType.detailStrong)
                    }
                }
            }
            Spacer(minLength: 0)

        }
        .padding(DesignSpace.m)
        .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.surfaceAlt2))
    }

    private func offersStrip(count: Int, lowest: String) -> some View {
        HStack(spacing: DesignSpace.s) {
            BzrIcon(.tag, tint: DesignColors.primary600)
            VStack(alignment: .leading, spacing: 0) {
                BzrText(
                    bzrPlural("customer.home.offers.new", count: count),
                    style: DesignType.tileTitle)
                if !lowest.isEmpty {
                    BzrText(BzrFormatter.fill(bzrString("orders.offers_from"), values: [("amount", lowest)]), style: DesignType.hint)
                }
            }
            Spacer(minLength: 0)
            // BACKEND-NOTES B-16: no offer photos are loaded, so the count stands where Figma stacks the avatars.
            BzrText(BzrFormat.number(count), style: DesignType.tileTitle.colored(DesignColors.onPrimary))
                .frame(minWidth: DesignSize.nearbyAvatar, minHeight: DesignSize.nearbyAvatar)
                .background(Capsule().fill(DesignColors.primary600))
                .accessibilityHidden(true)
        }
        .padding(DesignSpace.m)
        .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.surfaceAlt2))
    }
}
