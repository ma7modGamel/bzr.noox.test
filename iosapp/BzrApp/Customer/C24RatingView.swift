import BzrCore
import DesignSystem
import SwiftUI

public struct C24RatingView: View {
    let state: CustomerUIState
    let onRate: (Int, Int) -> Void
    let onCommentChange: (String) -> Void
    let onSubmit: () -> Void

    public init(
        state: CustomerUIState,
        onRate: @escaping (Int, Int) -> Void = { _, _ in },
        onCommentChange: @escaping (String) -> Void = { _ in },
        onSubmit: @escaping () -> Void = {}
    ) {
        self.state = state
        self.onRate = onRate
        self.onCommentChange = onCommentChange
        self.onSubmit = onSubmit
    }

    public var body: some View {
        CustomerScreen(title: bzrString("rating.title"), state: state) {
            ScreenHeading(bzrString("rating.heading"), detail: bzrString("rating.help"))
            RatingRow(label: bzrString("rating.quality"), selected: rating(0)) { onRate(0, $0) }
            RatingRow(label: bzrString("rating.punctuality"), selected: rating(1)) { onRate(1, $0) }
            RatingRow(label: bzrString("rating.conduct"), selected: rating(2)) { onRate(2, $0) }
            TextAreaField(
                label: bzrString("rating.comment"), placeholder: bzrString("rating.comment.placeholder"),
                value: state.fieldValues.first ?? "", state: state.fieldErrors.isEmpty ? .filled : .error,
                error: state.fieldErrors.first.map(errorText), onValueChange: onCommentChange)
            if let message = state.messageKey {
                if message == "rating.success" {
                    InfoBanner(text: bzrString(message))
                } else {
                    WarningBox(text: bzrString(message))
                }
            }
            if state.visibleActions.contains("rate") {
                PrimaryButton(
                    text: bzrString("rating.submit"),
                    state: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled),
                    onClick: onSubmit)
            }
        }
    }

    private func rating(_ index: Int) -> Int {
        state.ratings.indices.contains(index) ? state.ratings[index] : 0
    }
    private func errorText(_ key: String) -> String { bzrString(key) }
}

private struct RatingRow: View {
    let label: String
    let selected: Int
    let onSelect: (Int) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: DesignSpace.s) {
            BzrText(label, style: DesignType.cardTitle)
            HStack(spacing: DesignSpace.s) {
                ForEach(1...5, id: \.self) { value in
                    IconSquareButton(
                        label: "\(label) \(value)", icon: value <= selected ? .starFilled : .star
                    ) {
                        onSelect(value)
                    }
                }
            }
        }
    }
}
