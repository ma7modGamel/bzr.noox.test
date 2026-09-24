import BzrCore
import DesignSystem
import SwiftUI

public struct C20CancellationView: View {
    let state: CustomerUIState
    let onReasonSelect: (Int) -> Void
    let onNoteChange: (String) -> Void
    let onConfirm: () -> Void
    let onBack: () -> Void

    public init(
        state: CustomerUIState, onReasonSelect: @escaping (Int) -> Void = { _ in },
        onNoteChange: @escaping (String) -> Void = { _ in }, onConfirm: @escaping () -> Void = {},
        onBack: @escaping () -> Void = {}
    ) {
        self.state = state
        self.onReasonSelect = onReasonSelect
        self.onNoteChange = onNoteChange
        self.onConfirm = onConfirm
        self.onBack = onBack
    }

    public var body: some View {
        ZStack(alignment: .bottom) {
            CustomerScreen(title: bzrString("cancel.title"), state: state) {
                InfoBanner(text: bzrString("cancel.free"))
                ForEach(state.options.indices, id: \.self) { index in
                    Button(
                        action: { onReasonSelect(index) },
                        label: {
                            RadioCard(
                                title: state.options[index], body: bzrString("cancel.body"),
                                selected: state.selectedOptionIndex == index)
                        }
                    )
                    .buttonStyle(BzrPressStyle())
                }
                TextAreaField(
                    label: bzrString("cancel.note.label"), placeholder: bzrString("cancel.note.placeholder"),
                    value: state.fieldValues.first ?? "", state: state.isBusy ? .disabled : .empty,
                    optionalLabel: bzrString("common.optional"), onValueChange: onNoteChange)
            }
            if state.phase == .content {
                AppBottomSheet(
                    title: bzrString("cancel.title"), body: bzrString("cancel.free"),
                    action: bzrString("cancel.confirm"),
                    actionState: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled),
                    secondaryAction: bzrString("common.cancel"),
                    onAction: onConfirm, onSecondary: onBack)
            }
        }
    }
}
