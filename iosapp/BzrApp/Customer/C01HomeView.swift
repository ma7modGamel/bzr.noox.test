import BzrCore
import DesignSystem
import SwiftUI

public struct C01HomeView: View {
    let state: CustomerUIState
    let onOpen: (String) -> Void

    public init(state: CustomerUIState, onOpen: @escaping (String) -> Void = { _ in }) {
        self.state = state
        self.onOpen = onOpen
    }

    public var body: some View {
        CustomerScreen(title: bzrString("nav.home"), state: state) {
            ScreenHeading(bzrString("customer.greeting"), detail: bzrString("customer.home.subtitle"))
            ScreenHeading(bzrString("customer.categories.title"))
            HStack(spacing: DesignSpace.m) {
                SelectableTile(text: bzrString("tile.plumbing"), icon: .home, state: .selected)
                SelectableTile(text: bzrString("tile.electricity"), icon: .warning, state: .unselected)
            }
            PrimaryButton(text: bzrString("customer.new_request"), onClick: { onOpen("SCR-C03") })
            ScreenHeading(bzrString("customer.current_orders.title"))
            if state.phase == .empty {
                EmptyState(title: bzrString("empty.title"), body: bzrString("empty.body"))
            } else {
                OrderCard(title: bzrString("order.title"), subtitle: bzrString("order.subtitle"), status: bzrString("order.status"), onClick: { onOpen("SCR-C09") })
            }
            BottomNav(
                items: [
                    (.home, bzrString("nav.home")), (.orders, bzrString("nav.orders")),
                    (.chat, bzrString("nav.messages")), (.account, bzrString("nav.account")),
                ], selectedIndex: 0,
                onSelect: { index in
                    let routes = ["SCR-C01", "SCR-C25", "SCR-C18", "SCR-C02"]
                    if routes.indices.contains(index) { onOpen(routes[index]) }
                })
        }
    }
}
