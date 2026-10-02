import BzrCore
import DesignSystem
import SwiftUI

public struct C06OffersView: View {
    let state: CustomerUIState
    let onAction: (String) -> Void
    let onSort: (Int) -> Void
    let onSelect: (Int) -> Void
    let onProvider: (Int) -> Void

    public init(
        state: CustomerUIState, onAction: @escaping (String) -> Void = { _ in },
        onSort: @escaping (Int) -> Void = { _ in },
        onSelect: @escaping (Int) -> Void = { _ in },
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
            if state.phase == .empty {
                EmptyState(title: bzrString("offers.empty.title"), body: bzrString("offers.empty.body"))
                ActionButtons(actions: state.visibleActions, onAction: onAction)
            } else {
                ScreenHeading(state.fieldValues.first ?? "")
                Countdown(seconds: state.countdownSeconds, label: bzrString("offers.expires"))
                SortChips(
                    title: bzrString("sort.title"),
                    labels: state.showEta
                        ? [
                            bzrString("sort.top_rated"), bzrString("sort.lowest_price"),
                            bzrString("sort.fastest"),
                        ] : [bzrString("sort.top_rated"), bzrString("sort.lowest_price")],
                    selectedIndex: state.selectedIndex, onSelect: onSort)
                ForEach(Array(state.items.enumerated()), id: \.offset) { index, row in
                    let values = row.split(separator: "|", omittingEmptySubsequences: false).map(String.init)
                    if values.count >= 8 {
                        OfferCard(
                            provider: values[1], rating: values[2], services: values[3],
                            price: values[4],
                            detail: state.showEta && Int(values[5]) != nil
                                ? "\(values[5]) \(bzrString("unit.minute"))" : values[5],
                            badge: values[6],
                            button: bzrString("offer.select"), chatLabel: bzrString("action.chat"),
                            variant: !state.showEta
                                ? .scheduled
                                : state.pricingOptions.first == "inspection" ? .inspection : .execution,
                            priceCaption: state.pricingOptions.first == "inspection"
                                ? bzrString("offer.inspection_fee") : nil,
                            showSelect: state.visibleActions.contains("accept_offer"),
                            showChat: state.visibleActions.contains("chat"),
                            onOpen: { onProvider(index) }, onSelect: { onSelect(index) },
                            onChat: { onAction("chat") })
                    }
                }
                ActionButtons(
                    actions: state.visibleActions.filter { $0 != "accept_offer" }, onAction: onAction)
            }
        }
    }
}
