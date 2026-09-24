import BzrCore
import DesignSystem
import SwiftUI

public struct C29HelpView: View {
    let state: CustomerUIState
    let onExpand: (Int) -> Void
    let onSubjectChange: (String) -> Void
    let onMessageChange: (String) -> Void
    let onSubmit: () -> Void

    public init(
        state: CustomerUIState, onExpand: @escaping (Int) -> Void = { _ in },
        onSubjectChange: @escaping (String) -> Void = { _ in },
        onMessageChange: @escaping (String) -> Void = { _ in }, onSubmit: @escaping () -> Void = {}
    ) {
        self.state = state
        self.onExpand = onExpand
        self.onSubjectChange = onSubjectChange
        self.onMessageChange = onMessageChange
        self.onSubmit = onSubmit
    }

    public var body: some View {
        CustomerScreen(title: bzrString("help.title"), state: state) {
            ScreenHeading(bzrString("help.faq.title"))
            ForEach(state.items.indices, id: \.self) { index in
                Button(
                    action: { onExpand(index) },
                    label: {
                        SummaryCard(
                            title: state.items[index],
                            rows: state.selectedIndex == index ? [(.info, state.itemDetails[safe: index] ?? "")] : [],
                            editText: nil)
                    }
                ).buttonStyle(.plain)
            }
            ScreenHeading(bzrString("help.contact.title"))
            if state.messageKey == "help.success" { InfoBanner(text: bzrString("help.success")) }
            AppTextField(
                label: bzrString("help.subject"), placeholder: bzrString("help.subject.placeholder"),
                value: state.fieldValues[safe: 0] ?? "", state: fieldState("help.validation.subject"),
                error: state.fieldErrors.contains("help.validation.subject") ? bzrString("help.validation.subject") : nil,
                onValueChange: onSubjectChange)
            TextAreaField(
                label: bzrString("help.message"), placeholder: bzrString("help.message.placeholder"),
                value: state.fieldValues[safe: 1] ?? "", state: fieldState("help.validation.message"),
                error: state.fieldErrors.contains("help.validation.message") ? bzrString("help.validation.message") : nil,
                onValueChange: onMessageChange)
            PrimaryButton(text: bzrString("help.send"), state: buttonState, onClick: onSubmit)
        }
    }

    private func fieldState(_ key: String) -> FieldVisualState {
        state.isBusy ? .disabled : (state.fieldErrors.contains(key) ? .error : .empty)
    }

    private var buttonState: ButtonVisualState {
        state.isBusy ? .loading : (state.canContinue ? .normal : .disabled)
    }
}

private extension Array {
    subscript(safe index: Index) -> Element? { indices.contains(index) ? self[index] : nil }
}
