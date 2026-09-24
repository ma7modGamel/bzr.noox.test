import BzrCore
import DesignSystem
import SwiftUI

public struct C08OfferDetailsView: View {
    let state: CustomerUIState
    let onAction: (String) -> Void

    public init(state: CustomerUIState, onAction: @escaping (String) -> Void = { _ in }) { self.state = state; self.onAction = onAction }

    public var body: some View {
        CustomerScreen(title: bzrString("offer.details.title"), state: state) {
            ProviderHeader(name: bzrString("offer.provider"), rating: "4.9", services: "86", size: .medium, verifiedLabel: bzrString("provider.verified"))
            SummaryCard(
                title: bzrString("offer.details.title"),
                rows: [
                    (.orders, bzrString("offer.price")), (.clock, state.showEta ? "25 " + bzrString("unit.minute") : bzrString("offer.appointment")),
                    (.shield, bzrString("offer.inspection.deductible")),
                ], editText: bzrString("common.edit"))
            if state.showEta { EtaCard(value: "25", unit: bzrString("unit.minute"), title: bzrString("eta.title"), subtitle: bzrString("eta.subtitle")) }
            WarningBox(text: bzrString("offer.warning"))
            ActionButtons(actions: state.visibleActions, onAction: onAction)
        }
    }
}
