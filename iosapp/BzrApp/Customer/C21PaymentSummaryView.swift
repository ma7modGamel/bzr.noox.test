import BzrCore
import DesignSystem
import SwiftUI

/// SCR-C21 — Figma «إنهاء الخدمة والدفع» 3002:4134 (DEC-076): hero, provider card, invoice, the server's payment methods
/// and «ادفع الآن» when electronic payment is open. `items`: labor, materials, total.
public struct C21PaymentSummaryView: View {
    let loadImage: (String) async -> Data?
    let state: CustomerUIState
    let onMethodSelect: (Int) -> Void
    let onPay: () -> Void
    let onAction: (String) -> Void

    public init(
        state: CustomerUIState, loadImage: @escaping (String) async -> Data? = { _ in nil }, onMethodSelect: @escaping (Int) -> Void = { _ in }, onPay: @escaping () -> Void = {},
        onAction: @escaping (String) -> Void = { _ in }
    ) {
        self.loadImage = loadImage
        self.state = state
        self.onMethodSelect = onMethodSelect
        self.onPay = onPay
        self.onAction = onAction
    }

    private func value(_ index: Int) -> String { state.fieldValues.indices.contains(index) ? state.fieldValues[index] : "" }
    private func item(_ index: Int) -> String { state.items.indices.contains(index) ? state.items[index] : "" }

    public var body: some View {
        CustomerScreen(title: bzrString("completion.title"), state: state, bottomBar: AnyView(dock)) {
            if !state.warrantyCoverage.isEmpty { InfoBanner(text: state.warrantyCoverage) }
            ServicePhotoSection(title: bzrString("repair.photos.before"), urls: state.beforePhotoURLs, loadImage: loadImage)
            ServicePhotoSection(title: bzrString("repair.photos.after"), urls: state.afterPhotoURLs, loadImage: loadImage)
            FinishServiceBlocks.hero(name: value(0))
            FinishServiceBlocks.providerCard(name: value(0), verified: state.providerVerified, title: value(1), address: value(2))
            FinishServiceBlocks.invoice(labor: item(0), materials: item(1), total: item(2))
            BzrText(bzrString("confirm.payment"), style: DesignType.homeSection)
            let canSwitch = state.visibleActions.contains("change_payment_method")
            ForEach(Array(state.options.enumerated()), id: \.offset) { index, label in
                let code = state.optionCodes.indices.contains(index) ? state.optionCodes[index] : ""
                Button(action: { if canSwitch { onMethodSelect(index) } }) {
                    RadioCard(
                        title: label, body: methodBody(code), selected: index == state.selectedOptionIndex,
                        icon: code == "CASH" ? .payments : .receipt)
                }
                .buttonStyle(BzrPressStyle())
                .disabled(!canSwitch || state.isBusy)
            }
        }
    }

    private func methodBody(_ code: String) -> String {
        switch code {
        case "CASH": bzrString("completion.method.CASH")
        case "ELECTRONIC": bzrString("completion.method.ELECTRONIC")
        default: ""
        }
    }

    @ViewBuilder private var dock: some View {
        VStack(spacing: DesignSpace.xs) {
            if state.visibleActions.contains("pay_electronic") || state.visibleActions.contains("submit_instapay_transfer") {
                PrimaryButton(
                    text: BzrFormatter.fill(bzrString("completion.pay"), values: [("amount", item(2))]),
                    state: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled), icon: .check, onClick: onPay)
            }
            if state.visibleActions.contains("open_dispute") {
                FinishServiceBlocks.problemLink { onAction("open_dispute") }
            }
        }
    }
}
