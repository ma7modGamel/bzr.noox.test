import BzrCore
import DesignSystem
import SwiftUI

public struct C35NoOffersView: View {
    let state: CustomerUIState
    let onAction: (String) -> Void

    public init(state: CustomerUIState, onAction: @escaping (String) -> Void = { _ in }) {
        self.state = state
        self.onAction = onAction
    }

    public var body: some View {
        CustomerScreen(title: bzrString("no.offers.title"), state: state) {
            EmptyState(title: bzrString("no.offers.title"), body: bzrString("no.offers.body"))
            ActionButtons(actions: state.visibleActions, onAction: onAction)
        }
    }
}
