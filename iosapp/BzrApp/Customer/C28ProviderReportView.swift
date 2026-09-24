import BzrCore
import DesignSystem
import SwiftUI

public struct C28ProviderReportView: View {
    let state: CustomerUIState
    let onReasonSelect: (Int) -> Void
    let onDescriptionChange: (String) -> Void
    let onSubmit: () -> Void
    let onBack: () -> Void

    public init(
        state: CustomerUIState, onReasonSelect: @escaping (Int) -> Void = { _ in },
        onDescriptionChange: @escaping (String) -> Void = { _ in },
        onSubmit: @escaping () -> Void = {}, onBack: @escaping () -> Void = {}
    ) {
        self.state = state
        self.onReasonSelect = onReasonSelect
        self.onDescriptionChange = onDescriptionChange
        self.onSubmit = onSubmit
        self.onBack = onBack
    }

    public var body: some View {
        ZStack(alignment: .bottom) {
            CustomerScreen(title: bzrString("provider.report.title"), state: state) {
                InfoBanner(
                    text: bzrString(
                        state.messageKey == "provider.report.success"
                            ? "provider.report.success" : "provider.report.body"))
                if state.messageKey != "provider.report.success" {
                    ScreenHeading(bzrString("provider.report.reason.title"))
                    ForEach(state.options.indices, id: \.self) { index in
                        Button(
                            action: { onReasonSelect(index) },
                            label: {
                                RadioCard(
                                    title: state.options[index], body: bzrString("payment.option.body"),
                                    selected: state.selectedOptionIndex == index)
                            }
                        ).buttonStyle(.plain).disabled(state.isBusy)
                    }
                    TextAreaField(
                        label: bzrString("provider.report.description.label"),
                        placeholder: bzrString("provider.report.description.placeholder"),
                        value: state.fieldValues.first ?? "", state: state.isBusy ? .disabled : .empty,
                        optionalLabel: bzrString("common.optional"), onValueChange: onDescriptionChange)
                }
            }
            if state.messageKey != "provider.report.success" {
                AppBottomSheet(
                    title: bzrString("provider.report.title"), body: bzrString("provider.report.body"),
                    action: bzrString("provider.report.submit"),
                    actionState: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled),
                    secondaryAction: bzrString("common.cancel"), onAction: onSubmit, onSecondary: onBack)
            }
        }
    }
}
