import BzrCore
import DesignSystem
import SwiftUI

public struct C23CompletionView: View {
    let state: CustomerUIState
    let onAction: (String) -> Void

    public init(state: CustomerUIState, onAction: @escaping (String) -> Void = { _ in }) {
        self.state = state
        self.onAction = onAction
    }

    public var body: some View {
        let providerLabel = bzrString("completion.provider")
        let laborLabel = bzrString("payment.amount.labor")
        let materialsLabel = bzrString("payment.amount.materials")
        CustomerScreen(title: bzrString("completion.confirm.title"), state: state) {
            ScreenHeading(bzrString("completion.confirm.heading"))
            SummaryCard(
                title: bzrString("payment.summary.heading"),
                rows: [
                    (.account, "\(providerLabel) · \(state.items[safe: 0] ?? "")"),
                    (.orders, "\(laborLabel) · \(state.items[safe: 1] ?? "")"),
                    (.info, "\(materialsLabel) · \(state.items[safe: 2] ?? "")"),
                    (.check, "\(bzrString("payment.amount.total")) · \(state.items[safe: 3] ?? "")"),
                ], editText: nil)
            InfoBanner(text: bzrString("completion.confirm.help"))
            if state.visibleActions.contains("confirm_completion") {
                PrimaryButton(
                    text: bzrString("action.confirm_completion"),
                    state: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled),
                    onClick: { onAction("confirm_completion") })
            }
            if state.visibleActions.contains("open_dispute") {
                SecondaryButton(text: bzrString("completion.report_problem"), onClick: { onAction("open_dispute") })
            }
        }
    }
}

private extension Array {
    subscript(safe index: Index) -> Element? { indices.contains(index) ? self[index] : nil }
}
