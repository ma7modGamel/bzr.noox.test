import BzrCore
import DesignSystem
import SwiftUI

public struct C22ElectronicPaymentView: View {
    let state: CustomerUIState
    let onChannelSelect: (Int) -> Void
    let onCreate: () -> Void
    let onOpenCheckout: () -> Void
    let onCopyCode: () -> Void

    public init(
        state: CustomerUIState,
        onChannelSelect: @escaping (Int) -> Void = { _ in },
        onCreate: @escaping () -> Void = {},
        onOpenCheckout: @escaping () -> Void = {},
        onCopyCode: @escaping () -> Void = {}
    ) {
        self.state = state
        self.onChannelSelect = onChannelSelect
        self.onCreate = onCreate
        self.onOpenCheckout = onOpenCheckout
        self.onCopyCode = onCopyCode
    }

    public var body: some View {
        CustomerScreen(title: bzrString("payment.electronic.title"), state: state) {
            ScreenHeading(bzrString("payment.electronic.heading"))
            SummaryCard(
                title: bzrString("payment.amount.total"), rows: [(.check, state.items[safe: 0] ?? "")],
                editText: nil)
            ScreenHeading(bzrString("payment.channel.title"))
            ForEach(state.options.indices, id: \.self) { index in
                channelCard(index, state.options[index])
            }
            if state.messageKey == "payment.creating" { InfoBanner(text: bzrString("payment.creating")) }
            resultContent
            if shouldShowCreateButton {
                PrimaryButton(
                    text: bzrString("payment.create"),
                    state: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled),
                    onClick: onCreate)
            }
        }
    }

    private var paymentStatus: String { state.itemStates.first ?? "" }
    private var shouldShowCreateButton: Bool {
        state.visibleActions.contains("pay_electronic")
            && (paymentStatus.isEmpty || ["FAILED", "EXPIRED"].contains(paymentStatus))
    }

    private func channelCard(_ index: Int, _ title: String) -> some View {
        Button(
            action: { onChannelSelect(index) },
            label: {
                RadioCard(
                    title: title, body: bzrString("payment.option.body"),
                    selected: state.selectedOptionIndex == index)
            }
        )
        .buttonStyle(.plain)
        .disabled(!paymentStatus.isEmpty || state.isBusy)
    }

    @ViewBuilder private var resultContent: some View {
        switch paymentStatus {
        case "PENDING" where !(state.items[safe: 1] ?? "").isEmpty:
            InfoBanner(text: bzrString("payment.kiosk.ready"))
            SummaryCard(
                title: bzrString("payment.kiosk.reference"), rows: [(.info, state.items[safe: 1] ?? "")],
                editText: nil)
            SecondaryButton(text: bzrString("payment.kiosk.copy"), onClick: onCopyCode)
            Countdown(seconds: state.countdownSeconds, label: bzrString("payment.expires"))
        case "PENDING":
            InfoBanner(text: bzrString("payment.checkout.ready"))
            SecondaryButton(text: bzrString("payment.checkout.open"), onClick: onOpenCheckout)
        case "SUCCEEDED": InfoBanner(text: bzrString("payment.success"))
        case "FAILED": WarningBox(text: bzrString("payment.failed"))
        case "EXPIRED": WarningBox(text: bzrString("payment.expired"))
        default: EmptyView()
        }
    }
}

extension Array {
    fileprivate subscript(safe index: Index) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
