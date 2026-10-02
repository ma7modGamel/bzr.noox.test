import BzrCore
import DesignSystem
import SwiftUI

public struct C30ExecutionQuoteView: View {
    let state: CustomerUIState
    let onAction: (String) -> Void
    public init(state: CustomerUIState, onAction: @escaping (String) -> Void = { _ in }) {
        self.state = state
        self.onAction = onAction
    }
    public var body: some View {
        ProposalDecisionView(screen: "SCR-C30", state: state, onAction: onAction)
    }
}

public struct C31AdditionalCostView: View {
    let state: CustomerUIState
    let onAction: (String) -> Void
    public init(state: CustomerUIState, onAction: @escaping (String) -> Void = { _ in }) {
        self.state = state
        self.onAction = onAction
    }
    public var body: some View {
        ProposalDecisionView(screen: "SCR-C31", state: state, onAction: onAction)
    }
}

private struct ProposalDecisionView: View {
    let screen: String
    let state: CustomerUIState
    let onAction: (String) -> Void
    var body: some View {
        CustomerScreen(
            title: bzrString(
                screen == "SCR-C30" ? "proposal.execution.title" : "proposal.additional.title"),
            state: state
        ) {
            SummaryCard(
                title: state.items[safe: 0] ?? bzrString("proposal.amount"),
                rows: [
                    (.orders, state.items[safe: 1] ?? ""), (.info, state.items[safe: 2] ?? ""),
                    (.check, state.items[safe: 3] ?? ""),
                ],
                editText: nil)
            if state.hasPhoto {
                MediaThumb(
                    title: bzrString("proposal.photo"), stateLabel: "", kind: .photo, state: .uploaded)
            }
            Countdown(seconds: state.countdownSeconds, label: bzrString("countdown.label"))
            if screen == "SCR-C30" { InfoBanner(text: bzrString("proposal.free_inspection")) }
            WarningBox(text: bzrString(state.messageKey ?? "proposal.pending_warning"))
            ActionButtons(actions: state.visibleActions, onAction: onAction)
        }
    }
}

extension Array {
    fileprivate subscript(safe index: Index) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
