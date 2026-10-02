import BzrCore
import DesignSystem
import SwiftUI

public struct C32NotificationsView: View {
    let state: CustomerUIState
    let onOpen: (Int) -> Void
    let onOpenSettings: () -> Void
    let onBack: (() -> Void)?

    public init(
        state: CustomerUIState, onOpen: @escaping (Int) -> Void = { _ in },
        onOpenSettings: @escaping () -> Void = {}, onBack: (() -> Void)? = nil
    ) {
        self.state = state
        self.onOpen = onOpen
        self.onOpenSettings = onOpenSettings
        self.onBack = onBack
    }

    public var body: some View {
        CustomerScreen(title: bzrString("notifications.title"), state: state, onBack: onBack) {
            // DEC-058 — الإذن مرفوض على الجهاز: تنبيه ورابط الإعدادات، والقائمة تعمل كما هي.
            if state.notificationsDenied {
                InfoBanner(text: bzrString("notifications.permission.off"))
                LinkButton(text: bzrString("action.open_settings"), onClick: onOpenSettings)
            }
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

extension Array {
    fileprivate subscript(safe index: Index) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
