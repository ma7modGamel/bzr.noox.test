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
                rows: state.items.enumerated().map { index, value in
                    (index == 0 ? .home : .edit, value)
                }
                    + state.itemDetails.enumerated().map { index, value in
                        (index == 0 ? .location : .calendar, value)
                    } + state.itemStates.map { (.orders, $0) },
                editText: bzrString("common.edit"))
            if !state.showPricing { InfoBanner(text: bzrString("request.staff.pricing")) }
            Button(action: onTermsChange) {
                CheckRow(text: bzrString("request.review.terms"), checked: state.termsError == nil)
            }
            .buttonStyle(BzrPressStyle())
            if state.termsError != nil { InfoBanner(text: bzrString("request.terms.required")) }
            // A refused publish shows the server's reason here (SCR-C05 §الأخطاء).
            if state.messageKey == "request.publish.error" {
                WarningBox(text: (state.fieldValues.first ?? "").isEmpty ? bzrString("error.body") : state.fieldValues[0])
            }
            PrimaryButton(
                text: bzrString("request.publish"),
                state: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled),
                onClick: onPublish)
        }
    }
}
