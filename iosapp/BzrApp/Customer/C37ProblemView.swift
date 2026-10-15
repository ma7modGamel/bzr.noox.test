import BzrCore
import DesignSystem
import SwiftUI

/// SCR-C37, step 2 of 5 (DEC-071): problem type, description, media — the former C03 body.
public struct C37ProblemView: View {
    let state: CustomerUIState
    let onProblemSelect: (Int) -> Void
    let onDescriptionChange: (String) -> Void
    let onOpen: (String) -> Void

    private static let maxDescription = 1_000

    public init(
        state: CustomerUIState, onProblemSelect: @escaping (Int) -> Void = { _ in },
        onDescriptionChange: @escaping (String) -> Void = { _ in },
        onOpen: @escaping (String) -> Void = { _ in }
    ) {
        self.state = state
        self.onProblemSelect = onProblemSelect
        self.onDescriptionChange = onDescriptionChange
        self.onOpen = onOpen
    }

    private var description: String { state.fieldValues.first ?? "" }

    /// The type is chosen above, so its prompt is not repeated as a field error.
    private var fieldError: String? {
        state.selectedOptionIndex < 0 ? nil : state.descriptionError.map(bzrString)
    }

    public var body: some View {
        CustomerScreen(title: bzrString("request.problem.title"), state: state, bottomBar: AnyView(primaryAction)) {
            RequestProgress(current: 2, total: state.requestSteps)
            servicePill
            ScreenHeading(bzrString("request.problem.type"))
            BzrServiceGrid {
                ForEach(Array(state.itemDetails.enumerated()), id: \.offset) { index, label in
                    Button(
                        action: { onProblemSelect(index) },
                        label: {
                            SelectableChip(text: label, state: index == state.selectedOptionIndex ? .selected : .unselected)
                        }
                    )
                    .buttonStyle(BzrPressStyle())
                }
            }
            // Read before the chips while none is chosen: the step needs a problem type (BR-012).
            .accessibilityHint(state.selectedOptionIndex < 0 ? bzrString("request.problem.required") : "")
            TextAreaField(
                label: bzrString("request.description.label"),
                placeholder: bzrString("request.description.placeholder"),
                value: description,
                state: fieldError == nil ? .filled : .error,
                error: fieldError,
                optionalLabel: BzrFormatter.fill(
                    bzrString("request.description.count"),
                    values: [("count", BzrFormat.number(description.count)), ("max", BzrFormat.number(Self.maxDescription))]),
                onValueChange: onDescriptionChange
            )
            hintRow(.lightbulb, bzrString("request.description.hint"))
            HStack {
                ScreenHeading(bzrString("request.media.title"))
                Spacer(minLength: DesignSpace.s)
                BzrText(bzrString("request.media.optional"), style: DesignType.hint)
            }
            HStack(spacing: DesignSpace.s) {
                ActionPill(text: bzrString("request.media.photo"), icon: .camera) { onOpen("SCR-C17") }
                ActionPill(text: bzrString("request.media.video"), icon: .video) { onOpen("SCR-C17") }
                ActionPill(text: bzrString("media.audio"), icon: .mic) { onOpen("SCR-C17") }
            }
            BzrText(
                BzrFormatter.fill(bzrString("request.media.current"), values: [("count", BzrFormat.number(state.itemStates.count))]),
                style: DesignType.caption)
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DesignSpace.m) {
                    ForEach(Array((state.itemStates + ["add"]).enumerated()), id: \.offset) { _, kind in
                        attachmentTile(kind)
                    }
                }
                .padding(.vertical, DesignSpace.xs)
            }
            hintRow(.shield, bzrString("request.media.privacy"))
                .frame(maxWidth: .infinity)
        }
    }

    private var servicePill: some View {
        HStack(spacing: DesignSpace.s) {
            CategoryGlyph(state.optionIcons.first)
            BzrText(bzrString("request.service.selected"), style: DesignType.hint)
            BzrText(state.items.first ?? "", style: DesignType.body.weighted(DesignFont.bold), lineLimit: 1)
            Spacer(minLength: DesignSpace.s)
            LinkButton(text: bzrString("common.edit"), onClick: { onOpen("SCR-C03") })
        }
        .padding(.horizontal, DesignSpace.l)
        .padding(.vertical, DesignSpace.s)
        .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
        .bzrShadow(DesignShadows.card)
    }

    private func hintRow(_ icon: BzrIconKey, _ text: String) -> some View {
        HStack(spacing: DesignSpace.xs) {
            BzrIcon(icon, tint: DesignColors.primary600, size: DesignSize.iconSmall)
            BzrText(text, style: DesignType.hint)
        }
    }

    private func attachmentTile(_ kind: String) -> some View {
        let (icon, label): (BzrIconKey, String) =
            switch kind {
            case "photo": (.image, bzrString("media.photo"))
            case "video": (.video, bzrString("media.video"))
            case "audio": (.mic, bzrString("media.audio"))
            default: (.plus, bzrString("request.media.add"))
            }
        return Button(action: { onOpen("SCR-C17") }) {
            VStack(spacing: DesignSpace.xs) {
                BzrIcon(icon, tint: DesignColors.primary600, size: DesignSize.icon)
                BzrText(label, style: DesignType.hint, alignment: .center, lineLimit: 3)
            }
            .padding(DesignSpace.s)
            .frame(width: DesignSize.attachmentTile)
            .frame(minHeight: DesignSize.attachmentTile)
            .background(
                RoundedRectangle(cornerRadius: DesignRadius.card)
                    .fill(kind == "add" ? DesignColors.surfaceAlt2 : DesignColors.surface)
            )
            .bzrShadow(kind == "add" ? nil : DesignShadows.card)
        }
        .buttonStyle(BzrPressStyle())
    }

    private var primaryAction: some View {
        PrimaryButton(
            text: bzrString("request.step2.action"), state: state.canContinue ? .normal : .disabled, icon: .arrowForward,
            onClick: { onOpen("SCR-C04") })
    }
}
