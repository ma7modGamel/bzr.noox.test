import BzrCore
import DesignSystem
import SwiftUI

public struct C03ProblemView: View {
    let state: CustomerUIState
    let onCategorySelect: (Int) -> Void
    let onProblemSelect: (Int) -> Void
    let onDescriptionChange: (String) -> Void
    let onOpen: (String) -> Void

    public init(
        state: CustomerUIState, onCategorySelect: @escaping (Int) -> Void = { _ in },
        onProblemSelect: @escaping (Int) -> Void = { _ in },
        onDescriptionChange: @escaping (String) -> Void = { _ in },
        onOpen: @escaping (String) -> Void = { _ in }
    ) {
        self.state = state
        self.onCategorySelect = onCategorySelect
        self.onProblemSelect = onProblemSelect
        self.onDescriptionChange = onDescriptionChange
        self.onOpen = onOpen
    }

    public var body: some View {
        CustomerScreen(title: bzrString("request.problem.title"), state: state) {
            LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 3), spacing: DesignSpace.m) {
                ForEach(Array(state.options.enumerated()), id: \.offset) { index, label in
                    Button(
                        action: { onCategorySelect(index) },
                        label: {
                            SelectableTile(
                                text: label, icon: .home,
                                state: index == state.selectedIndex ? .selected : .unselected,
                                categoryIcon: state.optionIcons.indices.contains(index)
                                    ? state.optionIcons[index] : nil)
                        }
                    )
                    .buttonStyle(BzrPressStyle())
                }
            }
            ForEach(Array(state.itemDetails.enumerated()), id: \.offset) { index, label in
                Button(
                    action: { onProblemSelect(index) },
                    label: {
                        SelectableChip(
                            text: label,
                            state: index == state.selectedOptionIndex ? .selected : .unselected)
                    }
                )
                .buttonStyle(BzrPressStyle())
            }
            TextAreaField(
                label: bzrString("request.description.label"),
                placeholder: bzrString("request.description.placeholder"),
                value: state.fieldValues.first ?? "",
                state: state.descriptionError == nil ? .filled : .error,
                error: state.descriptionError.map { bzrString($0) },
                onValueChange: onDescriptionChange
            )
            ScreenHeading(bzrString("request.media.title"))
            SecondaryButton(text: bzrString("request.media.add"), onClick: { onOpen("SCR-C17") })
            if state.fieldValues.indices.contains(1), Int(state.fieldValues[1]) ?? 0 > 0 {
                MediaThumb(
                    title: bzrString("media.photo"), stateLabel: bzrString("media.uploaded"), kind: .photo,
                    state: .uploaded)
            }
            PrimaryButton(
                text: bzrString("common.next"), state: state.canContinue ? .normal : .disabled,
                onClick: { onOpen("SCR-C04") })
        }
    }
}
