import BzrCore
import DesignSystem
import SwiftUI

public struct C06OffersView: View {
    let state: CustomerUIState
    let onAction: (String) -> Void

    public init(state: CustomerUIState, onAction: @escaping (String) -> Void = { _ in }) { self.state = state; self.onAction = onAction }

    public var body: some View {
        CustomerScreen(title: bzrString("offers.title"), state: state) {
            if state.phase == .empty {
                EmptyState(title: bzrString("offers.empty.title"), body: bzrString("offers.empty.body"))
                ActionButtons(actions: state.visibleActions, onAction: onAction)
            } else {
                ScreenHeading(bzrString("offers.count"))
                Countdown(seconds: 1320, label: bzrString("offers.expires"))
                SortChips(title: bzrString("sort.title"), labels: [bzrString("sort.top_rated"), bzrString("sort.lowest_price"), bzrString("sort.fastest")], selectedIndex: 0)
                OfferCard(
                    provider: bzrString("offer.provider"), rating: "4.9", services: "86", price: bzrString("offer.price"), detail: "25 " + bzrString("unit.minute"),
                    badge: bzrString("badge.top_rated"), button: bzrString("offer.select"), chatLabel: bzrString("action.chat"), variant: .execution)
                OfferCard(
                    provider: bzrString("drawer.name"), rating: "4.8", services: "54", price: bzrString("offer.inspection.price"), detail: bzrString("offer.scheduled"),
                    badge: bzrString("badge.best_price"), button: bzrString("offer.select"), chatLabel: bzrString("action.chat"), variant: .inspection,
                    priceCaption: bzrString("offer.inspection_fee"))
                ActionButtons(actions: state.visibleActions.filter { $0 != "accept_offer" }, onAction: onAction)
            }
        }
    }
}
