import BzrCore
import DesignSystem
import SwiftUI

public struct C05ReviewView: View {
    let state: CustomerUIState
    let onTermsChange: () -> Void
    let onPublish: () -> Void

    public init(
        state: CustomerUIState, onTermsChange: @escaping () -> Void = {},
        onPublish: @escaping () -> Void = {}
    ) {
        self.state = state
        self.onTermsChange = onTermsChange
        self.onPublish = onPublish
    }

    public var body: some View {
        CustomerScreen(title: bzrString("request.review.title"), state: state) {
            SummaryCard(
                title: bzrString("summary.title"),
                rows: [
                    (.home, bzrString("request.review.service")), (.edit, bzrString("request.review.description")),
                    (.location, bzrString("request.address.value")), (.calendar, bzrString("request.slot.value")),
                ], editText: bzrString("common.edit"))
            if !state.showPricing { InfoBanner(text: bzrString("request.staff.pricing")) }
            Button(action: onTermsChange) {
                CheckRow(text: bzrString("request.review.terms"), checked: state.termsError == nil)
            }
            .buttonStyle(BzrPressStyle())
            if state.termsError != nil { InfoBanner(text: bzrString("request.terms.required")) }
            PrimaryButton(text: bzrString("request.publish"), state: state.canContinue ? .normal : .disabled, onClick: onPublish)
        }
    }
}
