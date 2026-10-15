import BzrCore
import DesignSystem
import SwiftUI

/// SCR-C06 — Figma «Primo App» offers 3002:446 (DEC-073): count and request pill, headline and payment note, sort chips,
/// then one card per offer. Rows: id|name|rating|reviews|amount|includes|eta or slot|materials|note|verified|provider
/// id|subtitle.
public struct C06OffersView: View {
    let state: CustomerUIState
    let onAction: (String) -> Void
    let onSort: (Int) -> Void
    let onSelect: (Int) -> Void
    let onProvider: (Int) -> Void

    private static let offerFields = 12
    private static let sortRating = 0
    private static let sortPrice = 1
    private static let sortEta = 2
    private static let sortBest = 3
    private static let minutesPerHour = 60

    public init(
        state: CustomerUIState, onAction: @escaping (String) -> Void = { _ in },
        onSort: @escaping (Int) -> Void = { _ in }, onSelect: @escaping (Int) -> Void = { _ in },
        onProvider: @escaping (Int) -> Void = { _ in }
    ) {
        self.state = state
        self.onAction = onAction
        self.onSort = onSort
        self.onSelect = onSelect
        self.onProvider = onProvider
    }

    public var body: some View {
        CustomerScreen(title: bzrString("offers.title"), state: state) {
            if !state.waitingInfo.isEmpty { InfoBanner(text: state.waitingInfo) }
            if state.phase == .empty {
                EmptyState(title: bzrString("offers.empty.title"), body: bzrString("offers.empty.body"))
            } else {
                header
                if !state.options.isEmpty { SortChips(title: nil, labels: state.options, selectedIndex: state.selectedIndex, onSelect: onSort) }
                VStack(spacing: DesignSpace.xl) {
                    ForEach(Array(state.items.enumerated()), id: \.offset) { index, row in
                        let values = row.components(separatedBy: "|")
                        if values.count >= Self.offerFields { card(values, index: index) }
                    }
                }
            }
            ActionButtons(
                actions: state.phase == .empty ? state.visibleActions : state.visibleActions.filter { $0 != "accept_offer" },
                onAction: onAction)
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: DesignSpace.s) {
            HStack(spacing: DesignSpace.s) {
                BzrText(
                    bzrPlural("offers.available", count: state.items.count),
                    style: DesignType.detailStrong)
                Circle().fill(DesignColors.primary400).frame(width: DesignSize.statusDot, height: DesignSize.statusDot)
                Spacer(minLength: DesignSpace.s)
                if let problem = state.itemDetails.first, !problem.isEmpty {
                    HStack(spacing: DesignSpace.xs) {
                        BzrIcon(.wrench, tint: DesignColors.primary600, size: DesignSize.iconXs)
                        BzrText(BzrFormatter.fill(bzrString("offers.request"), values: [("title", problem)]), style: DesignType.statusPill, lineLimit: 1)
                    }
                    .padding(.horizontal, DesignSpace.m)
                    .padding(.vertical, DesignSpace.xs)
                    .background(Capsule().fill(DesignColors.statusPillBg))
                }
            }
            BzrText(bzrString("offers.headline"), style: DesignType.screenTitle)
            HStack(spacing: DesignSpace.xs) {
                BzrIcon(.shield, tint: DesignColors.primary600, size: DesignSize.iconSmall)
                BzrText(bzrString("offers.payment_note"), style: DesignType.linkSmall)
            }
        }
    }

    private func card(_ values: [String], index: Int) -> some View {
        let inspection = state.pricingOptions.first == "inspection"
        let eta = Int(values[6]).map { minutes in
            minutes >= Self.minutesPerHour && minutes % Self.minutesPerHour == 0
                ? bzrPlural("offer.eta.hours", count: minutes / Self.minutesPerHour)
                : bzrPlural("offer.eta", count: minutes)
        }
        return OfferCard(
            provider: values[1], subtitle: values[11], rating: values[2],
            reviews: values[3].isEmpty ? "" : BzrFormatter.fill(bzrString("offer.reviews"), values: [("count", values[3])]),
            price: BzrFormatter.fill(bzrString("customer.home.offers.amount"), values: [("amount", values[4])]),
            priceCaption: values[5].isEmpty ? (inspection ? bzrString("offer.inspection_fee") : "") : values[5],
            detail: eta ?? values[6], materials: values[7], note: values[8].isEmpty ? "" : "“\(values[8])”",
            verified: values[9] == "true", verifiedLabel: bzrString("offer.verified"),
            button: bzrString("offer.select"), profileLabel: bzrString("offer.profile"), chatLabel: bzrString("button.chat"),
            showSelect: state.visibleActions.contains("accept_offer"), showChat: state.visibleActions.contains("chat"),
            availableNow: values.indices.contains(12) && values[12] == "true",
            onOpen: { onProvider(index) }, onSelect: { onSelect(index) }, onChat: { onAction("chat") })
    }
}
