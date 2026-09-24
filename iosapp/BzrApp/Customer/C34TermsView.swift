import BzrCore
import DesignSystem
import SwiftUI

public struct C34TermsView: View {
    let state: CustomerUIState
    private let versionLabel = bzrString("terms.version")
    private let effectiveLabel = bzrString("terms.effective")

    public init(state: CustomerUIState) { self.state = state }

    public var body: some View {
        CustomerScreen(title: bzrString("terms.title"), state: state) {
            if !state.canContinue {
                InfoBanner(text: bzrString("terms.empty"))
            } else {
                BzrText("\(versionLabel): \(state.itemDetails.first ?? "")", style: DesignType.caption)
                BzrText("\(effectiveLabel): \(state.itemDetails.dropFirst().first ?? "")", style: DesignType.caption)
                BzrText(state.items.first ?? "")
            }
        }
    }
}
