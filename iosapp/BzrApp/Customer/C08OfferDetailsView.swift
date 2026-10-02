import BzrCore
import DesignSystem
import SwiftUI

public struct C08OfferDetailsView: View {
    let state: CustomerUIState
    let onAction: (String) -> Void
    let onPaymentSelect: (Int) -> Void

    public init(
        state: CustomerUIState, onAction: @escaping (String) -> Void = { _ in },
        onPaymentSelect: @escaping (Int) -> Void = { _ in }
    ) {
        self.state = state
        self.onAction = onAction
        self.onPaymentSelect = onPaymentSelect
    }

    public var body: some View {
        CustomerScreen(title: bzrString("offer.details.title"), state: state) {
            ProviderHeader(
                name: field(0), rating: field(1), services: field(2), size: .medium,
                verifiedLabel: state.providerVerified ? bzrString("provider.verified") : nil)
            SummaryCard(
                title: bzrString("offer.details.title"),
                rows: state.items.enumerated().map { index, value in
                    (
                        [BzrIconKey.orders, .clock, .shield].indices.contains(index)
                            ? [BzrIconKey.orders, .clock, .shield][index] : .info, value
                    )
                }
                    + state.itemDetails.enumerated().map { index, value in
                        (index == 0 ? .home : .location, value)
                    }, editText: nil)
            if state.showEta {
                EtaCard(
                    value: field(3), unit: bzrString("unit.minute"), title: bzrString("eta.title"),
                    subtitle: bzrString("eta.subtitle"))
            }
            WarningBox(text: bzrString("offer.warning"))
            ForEach(Array(state.options.enumerated()), id: \.offset) { index, label in
                Button(
                    action: { onPaymentSelect(index) },
                    label: {
                        RadioCard(title: label, body: "", selected: index == state.selectedOptionIndex)
                    }
                )
                .buttonStyle(BzrPressStyle())
            }
            ActionButtons(actions: state.visibleActions, onAction: onAction)
        }
    }

    private func field(_ index: Int) -> String {
        state.fieldValues.indices.contains(index) ? state.fieldValues[index] : ""
    }
}
