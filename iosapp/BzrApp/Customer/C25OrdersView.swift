import BzrCore
import DesignSystem
import SwiftUI

public struct C25OrdersView: View {
    let state: CustomerUIState
    let onTabSelect: (Int) -> Void
    let onOrderSelect: (Int) -> Void

    public init(
        state: CustomerUIState,
        onTabSelect: @escaping (Int) -> Void = { _ in },
        onOrderSelect: @escaping (Int) -> Void = { _ in }
    ) {
        self.state = state
        self.onTabSelect = onTabSelect
        self.onOrderSelect = onOrderSelect
    }

    public var body: some View {
        CustomerScreen(title: bzrString("orders.title"), state: state) {
            SortChips(
                title: nil, labels: [bzrString("orders.current"), bzrString("orders.past")],
                selectedIndex: state.selectedOptionIndex, onSelect: onTabSelect)
            if state.phase == .empty {
                EmptyState(
                    title: bzrString(state.selectedOptionIndex == 1 ? "orders.past.empty.title" : "orders.current.empty.title"),
                    body: bzrString(state.selectedOptionIndex == 1 ? "orders.past.empty.body" : "orders.current.empty.body"))
            }
            ForEach(state.items.indices, id: \.self) { index in
                OrderCard(
                    title: state.items[index], subtitle: value(state.itemDetails, index),
                    status: value(state.itemStates, index), onClick: { onOrderSelect(index) })
            }
            if state.isBusy { BzrText(bzrString("orders.loading_more"), style: DesignType.secondary) }
        }
    }

    private func value(_ values: [String], _ index: Int) -> String {
        values.indices.contains(index) ? values[index] : ""
    }
}
