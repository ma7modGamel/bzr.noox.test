import BzrCore
import DesignSystem
import SwiftUI

public struct C04TimingView: View {
    let state: CustomerUIState
    let onOpen: (String) -> Void

    public init(state: CustomerUIState, onOpen: @escaping (String) -> Void = { _ in }) { self.state = state; self.onOpen = onOpen }

    public var body: some View {
        CustomerScreen(title: bzrString("request.timing.title"), state: state) {
            SummaryCard(
                title: bzrString("request.address.title"), rows: [(.location, bzrString("request.address.value"))], editText: bzrString("common.change"),
                onEdit: { onOpen("SCR-C14") })
            RadioCard(title: state.options.first ?? "", body: bzrString("radio.now.body"), selected: state.selectedOptionIndex == 0)
            Button(
                action: { onOpen("SCR-C16") },
                label: {
                    RadioCard(title: state.options.indices.contains(1) ? state.options[1] : "", body: bzrString("request.slot.value"), selected: state.selectedOptionIndex == 1)
                }
            )
            .buttonStyle(BzrPressStyle())
            if let message = state.messageKey { InfoBanner(text: bzrString(message)) }
            if state.showPricing {
                AppTextField(label: bzrString("request.budget.label"), placeholder: bzrString("field.amount.placeholder"), value: "450", state: .filled)
            } else {
                InfoBanner(text: bzrString("request.staff.pricing"))
            }
            PrimaryButton(text: bzrString("common.next"), state: state.canContinue ? .normal : .disabled, onClick: { onOpen("SCR-C05") })
        }
    }
}
