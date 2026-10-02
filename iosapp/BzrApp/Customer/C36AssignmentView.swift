import BzrCore
import DesignSystem
import SwiftUI

/// SCR-C36 (employee mode): the published order waiting for assignment; edit and cancel from `available_actions`.
public struct C36AssignmentView: View {
    let state: CustomerUIState
    let onAction: (String) -> Void
    let onBack: (() -> Void)?

    public init(
        state: CustomerUIState, onAction: @escaping (String) -> Void = { _ in },
        onBack: (() -> Void)? = nil
    ) {
        self.state = state
        self.onAction = onAction
        self.onBack = onBack
    }

    public var body: some View {
        CustomerScreen(title: bzrString("assignment.waiting.title"), state: state, onBack: onBack) {
            InfoBanner(text: bzrString("tracking.status.open"))
            SummaryCard(
                title: title,
                rows: state.items.enumerated().map { index, row in
                    (index == 0 ? .orders : index == 1 ? .location : .calendar, row)
                },
                editText: nil)
            ActionButtons(actions: state.visibleActions, onAction: onAction)
        }
    }

    private var title: String {
        let value = state.fieldValues.first ?? ""
        return value.isEmpty ? bzrString("assignment.summary.title") : value
    }
}
