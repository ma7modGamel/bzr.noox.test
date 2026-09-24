import BzrCore
import DesignSystem
import SwiftUI

public struct C21PaymentSummaryView: View {
    let state: CustomerUIState
    let onMethodSelect: (Int) -> Void
    let onPay: () -> Void
    let onAction: (String) -> Void

    public init(
        state: CustomerUIState,
        onMethodSelect: @escaping (Int) -> Void = { _ in },
        onPay: @escaping () -> Void = {}, onAction: @escaping (String) -> Void = { _ in }
    ) {
        self.state = state
        self.onMethodSelect = onMethodSelect
        self.onPay = onPay
        self.onAction = onAction
    }

    public var body: some View {
        let laborLabel = bzrString("payment.amount.labor")
        let materialsLabel = bzrString("payment.amount.materials")
        CustomerScreen(title: bzrString("payment.summary.title"), state: state) {
            ScreenHeading(bzrString("payment.summary.heading"))
            SummaryCard(
                title: bzrString("payment.summary.heading"),
                rows: [
                    (.orders, "\(laborLabel) · \(state.items[safe: 0] ?? "")"),
                    (.info, "\(materialsLabel) · \(state.items[safe: 1] ?? "")"),
                    (.check, "\(bzrString("payment.amount.total")) · \(state.items[safe: 2] ?? "")"),
                ], editText: nil)
            ScreenHeading(bzrString("payment.method.title"))
            ForEach(state.options.indices, id: \.self) { index in
                methodCard(index: index, title: state.options[index])
            }
            InfoBanner(text: bzrString("payment.option.body"))
            if state.visibleActions.contains("pay_electronic") {
                PrimaryButton(
                    text: bzrString("action.pay_electronic"),
                    state: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled),
                    onClick: onPay)
            }
            ActionButtons(actions: state.visibleActions.filter { $0 == "open_dispute" }, onAction: onAction)
        }
    }

    private func methodCard(index: Int, title: String) -> some View {
        Button(
            action: { onMethodSelect(index) },
            label: {
                RadioCard(title: title, body: bzrString("payment.option.body"), selected: state.selectedOptionIndex == index)
            }
        )
        .buttonStyle(.plain)
        .disabled(!state.visibleActions.contains("change_payment_method") || state.isBusy)
    }
}

private extension Array {
    subscript(safe index: Index) -> Element? { indices.contains(index) ? self[index] : nil }
}
