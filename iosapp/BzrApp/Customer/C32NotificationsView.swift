import BzrCore
import DesignSystem
import SwiftUI

public struct C32NotificationsView: View {
    let state: CustomerUIState
    let onOpen: (Int) -> Void

    public init(state: CustomerUIState, onOpen: @escaping (Int) -> Void = { _ in }) {
        self.state = state
        self.onOpen = onOpen
    }

    public var body: some View {
        CustomerScreen(title: bzrString("notifications.title"), state: state) {
            if state.items.isEmpty {
                EmptyState(
                    title: bzrString("notifications.empty.title"),
                    body: bzrString("notifications.empty.body"))
            } else {
                ForEach(state.items.indices, id: \.self) { index in
                    Button(
                        action: { onOpen(index) },
                        label: {
                            SummaryCard(
                                title: state.items[index],
                                rows: [
                                    (.messages, state.itemDetails[safe: index] ?? ""),
                                    (.clock, state.fieldValues[safe: index] ?? ""),
                                ],
                                editText: state.itemStates[safe: index] == "unread"
                                    ? bzrString("notifications.unread") : nil)
                        }
                    ).buttonStyle(.plain).disabled(state.isBusy)
                }
            }
        }
    }
}

private extension Array {
    subscript(safe index: Index) -> Element? { indices.contains(index) ? self[index] : nil }
}
