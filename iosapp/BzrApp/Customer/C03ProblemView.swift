import BzrCore
import DesignSystem
import SwiftUI

public struct C03ProblemView: View {
    let state: CustomerUIState
    let onProblemSelect: () -> Void
    let onOpen: (String) -> Void

    public init(
        state: CustomerUIState, onProblemSelect: @escaping () -> Void = {},
        onOpen: @escaping (String) -> Void = { _ in }
    ) {
        self.state = state
        self.onProblemSelect = onProblemSelect
        self.onOpen = onOpen
    }

    public var body: some View {
        CustomerScreen(title: bzrString("request.problem.title"), state: state) {
            HStack(spacing: DesignSpace.m) {
                Button(action: onProblemSelect) {
                    SelectableTile(text: bzrString("request.problem.leak"), icon: .home, state: .selected)
                }
                .buttonStyle(BzrPressStyle())
                SelectableTile(text: bzrString("request.problem.blockage"), icon: .warning, state: .unselected)
            }
            HStack(spacing: DesignSpace.m) {
                SelectableTile(text: bzrString("request.problem.installation"), icon: .edit, state: .unselected)
                SelectableTile(text: bzrString("request.problem.other"), icon: .more, state: .unselected)
            }
            TextAreaField(
                label: bzrString("request.description.label"), placeholder: bzrString("request.description.placeholder"),
                value: state.phase == .empty ? "" : bzrString("request.review.description"),
                state: state.descriptionError == nil ? .filled : .error,
                error: state.descriptionError.map { bzrString($0) }
            )
            ScreenHeading(bzrString("request.media.title"))
            SecondaryButton(text: bzrString("request.media.add"), onClick: { onOpen("SCR-C17") })
            if state.canContinue { MediaThumb(title: bzrString("media.photo"), stateLabel: bzrString("media.uploaded"), kind: .photo, state: .uploaded) }
            PrimaryButton(text: bzrString("common.next"), state: state.canContinue ? .normal : .disabled, onClick: { onOpen("SCR-C04") })
        }
    }
}
