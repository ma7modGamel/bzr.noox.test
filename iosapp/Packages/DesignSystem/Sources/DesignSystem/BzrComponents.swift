// swiftlint:disable file_length
import SwiftUI
import UIKit

// Mirror of BzrComponents.kt: same names, same parameters, same states (43 §3).
// Compose defaults are reproduced explicitly: Column → VStack(alignment: .leading), Row → HStack,
// `weight(1f)` → `.frame(maxWidth: .infinity, alignment: .leading)`, `border()` → `strokeBorder` (inside).

public enum ButtonVisualState { case normal, pressed, disabled, loading }
public enum SelectionState { case selected, unselected, disabled }
public enum FieldVisualState: CaseIterable { case empty, filled, focused, error, disabled }
public enum FieldImeAction { case next, done }
public enum OfferVariant { case execution, inspection, scheduled }
public enum StepState { case done, active, pending, onHold }
public enum MediaState { case uploading, uploaded, failed }
public enum MediaKind { case photo, video, audio }
public enum BadgeKind { case highlight, status }
public enum ChatKind { case text, image, blocked }
public enum AvatarSize {
    case small, medium, large

    var value: CGFloat {
        switch self {
        case .small: return DesignSize.avatarSmall
        case .medium: return DesignSize.avatarMedium
        case .large: return DesignSize.avatarLarge
        }
    }
}

// ── Foundation ───────────────────────────────────────────────────────────

private struct HorizontalLine: View {
    var body: some View { Rectangle().fill(DesignColors.border).frame(height: DesignBorder.width) }
}

private struct VerticalLine: View {
    let height: CGFloat
    var body: some View {
        Rectangle().fill(DesignColors.border).frame(width: DesignBorder.width, height: height)
    }
}

/// Deterministic arc instead of an animated spinner, so snapshots always show the loading state.
private struct ProgressArc: View {
    let color: Color
    let label: String

    var body: some View {
        Circle()
            .trim(from: 0, to: DesignRatio.progressArc)
            .stroke(color, style: StrokeStyle(lineWidth: DesignSize.progressStroke, lineCap: .round))
            .rotationEffect(.degrees(-90))
            .padding(DesignSize.progressStroke / 2)
            .frame(width: DesignSize.icon, height: DesignSize.icon)
            .accessibilityLabel(label)
    }
}

extension View {
    fileprivate func selectionFrame(_ state: SelectionState, radius: CGFloat) -> some View {
        let selected = state == .selected
        return
            self
            .bzrFrame(
                radius: radius,
                fill: selected ? DesignBorder.selectedFill : DesignColors.surface,
                border: selected ? DesignBorder.selectedColor : DesignColors.border,
                width: selected ? DesignBorder.selectedWidth : DesignBorder.width
            )
            .opacity(state == .disabled ? DesignOpacity.disabled : 1)
    }
}

// ── Actions & navigation ─────────────────────────────────────────────────

public struct AppTopBar: View {
    private let title: String
    private let actionIcon: BzrIconKey?
    private let actionLabel: String?
    private let onBack: () -> Void
    private let onAction: () -> Void

    public init(
        title: String, actionIcon: BzrIconKey? = nil, actionLabel: String? = nil,
        onBack: @escaping () -> Void = {}, onAction: @escaping () -> Void = {}
    ) {
        self.title = title
        self.actionIcon = actionIcon
        self.actionLabel = actionLabel
        self.onBack = onBack
        self.onAction = onAction
    }

    public var body: some View {
        ZStack {
            BzrText(title, style: DesignType.screenTitle, lineLimit: 1)
            HStack(spacing: 0) {
                if let actionIcon {
                    iconButton(actionIcon, label: actionLabel, action: onAction)
                }
                Spacer(minLength: 0)
                iconButton(.arrowBack, label: bzrString("a11y.back"), action: onBack)
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: DesignSize.topBarHeight)
        .padding(.horizontal, DesignSpace.screenHorizontal)
        .background(DesignColors.surface)
    }

    private func iconButton(_ icon: BzrIconKey, label: String?, action: @escaping () -> Void)
        -> some View
    {
        Button(action: action) {
            BzrIcon(icon, label: label).frame(
                width: DesignSize.menuIconBox, height: DesignSize.menuIconBox)
        }
        .buttonStyle(BzrPressStyle())
    }
}

public struct PrimaryButton: View {
    private let text: String
    private let state: ButtonVisualState
    private let height: CGFloat
    private let onClick: () -> Void

    public init(
        text: String, state: ButtonVisualState = .normal,
        height: CGFloat = DesignSize.primaryButtonHeight, onClick: @escaping () -> Void = {}
    ) {
        self.text = text
        self.state = state
        self.height = height
        self.onClick = onClick
    }

    public var body: some View {
        Button(action: onClick) {
            ZStack {
                if state == .loading {
                    ProgressArc(color: DesignColors.onPrimary, label: bzrString("a11y.loading"))
                } else {
                    BzrText(text, style: DesignType.button, lineLimit: 1)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(height: height)
            .background(state == .pressed ? DesignColors.primary700 : DesignColors.primary600)
            .clipShape(RoundedRectangle(cornerRadius: DesignRadius.button))
        }
        .buttonStyle(BzrPressStyle())
        .disabled(!(state == .normal || state == .pressed))
        .opacity(state == .disabled ? DesignOpacity.disabled : 1)
    }
}

public struct SecondaryButton: View {
    private let text: String
    private let enabled: Bool
    private let onClick: () -> Void

    public init(text: String, enabled: Bool = true, onClick: @escaping () -> Void = {}) {
        self.text = text
        self.enabled = enabled
        self.onClick = onClick
    }

    public var body: some View {
        Button(action: onClick) {
            BzrText(text, style: DesignType.button.colored(DesignColors.primary700), lineLimit: 1)
                .frame(maxWidth: .infinity)
                .frame(height: DesignSize.secondaryButtonHeight)
                .bzrFrame(
                    radius: DesignRadius.button, fill: DesignColors.surface, border: DesignColors.primary600,
                    width: DesignBorder.width)
        }
        .buttonStyle(BzrPressStyle())
        .disabled(!enabled)
        .opacity(enabled ? 1 : DesignOpacity.disabled)
    }
}

public struct DangerTextButton: View {
    private let text: String
    private let onClick: () -> Void

    public init(text: String, onClick: @escaping () -> Void = {}) {
        self.text = text
        self.onClick = onClick
    }

    public var body: some View {
        Button(action: onClick) {
            BzrText(text, style: DesignType.body.colored(DesignColors.danger))
                .padding(.horizontal, DesignSpace.m)
                .frame(height: DesignSize.secondaryButtonHeight)
        }
        .buttonStyle(BzrPressStyle())
    }
}

public struct LinkButton: View {
    private let text: String
    private let onClick: () -> Void

    public init(text: String, onClick: @escaping () -> Void = {}) {
        self.text = text
        self.onClick = onClick
    }

    public var body: some View {
        Button(action: onClick) {
            BzrText(text, style: DesignType.body.colored(DesignColors.primary700))
                .frame(height: DesignSize.secondaryButtonHeight)
                .padding(.horizontal, DesignSpace.m)
        }
        .buttonStyle(BzrPressStyle())
    }
}

public struct IconSquareButton: View {
    private let label: String
    private let icon: BzrIconKey
    private let onClick: () -> Void

    public init(label: String, icon: BzrIconKey = .chat, onClick: @escaping () -> Void = {}) {
        self.label = label
        self.icon = icon
        self.onClick = onClick
    }

    public var body: some View {
        Button(action: onClick) {
            BzrIcon(icon, label: label, tint: DesignColors.primary600)
                .frame(width: DesignSize.chatSquareButtonW, height: DesignSize.chatSquareButtonH)
                .bzrFrame(
                    radius: DesignRadius.button, fill: DesignColors.surface, border: DesignColors.primary600,
                    width: DesignBorder.width)
        }
        .buttonStyle(BzrPressStyle())
    }
}

public struct BottomNav: View {
    private let items: [(BzrIconKey, String)]
    private let selectedIndex: Int
    private let onSelect: (Int) -> Void

    public init(
        items: [(BzrIconKey, String)], selectedIndex: Int, onSelect: @escaping (Int) -> Void = { _ in }
    ) {
        self.items = items
        self.selectedIndex = selectedIndex
        self.onSelect = onSelect
    }

    private func color(_ index: Int) -> Color {
        index == selectedIndex ? DesignColors.primary500 : DesignColors.slate400
    }

    public var body: some View {
        VStack(spacing: 0) {
            HorizontalLine()
            HStack(spacing: 0) {
                ForEach(items.indices, id: \.self) { index in
                    Button {
                        onSelect(index)
                    } label: {
                        VStack(spacing: DesignSpace.xs) {
                            BzrIcon(items[index].0, tint: color(index))
                            BzrText(items[index].1, style: DesignType.caption.colored(color(index)), lineLimit: 1)
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(BzrPressStyle())
                }
            }
            .frame(height: DesignSize.bottomNavHeight)
        }
        .background(DesignColors.surface)
    }
}

public struct MenuRow: View {
    private let icon: BzrIconKey
    private let text: String
    private let onClick: () -> Void

    public init(icon: BzrIconKey, text: String, onClick: @escaping () -> Void = {}) {
        self.icon = icon
        self.text = text
        self.onClick = onClick
    }

    public var body: some View {
        Button(action: onClick) {
            HStack(spacing: DesignSpace.m) {
                BzrIcon(icon)
                    .frame(width: DesignSize.menuIconBox, height: DesignSize.menuIconBox)
                    .background(DesignColors.surfaceAlt)
                    .clipShape(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox))
                BzrText(text).frame(maxWidth: .infinity, alignment: .leading)
                BzrIcon(.chevron, tint: DesignColors.slate400)
            }
            .padding(.vertical, DesignSpace.s)
        }
        .buttonStyle(BzrPressStyle())
    }
}

public struct DrawerMenu: View {
    private let name: String
    private let rating: String
    private let verifiedLabel: String
    private let rows: [(BzrIconKey, String)]
    private let action: String
    private let actionState: ButtonVisualState
    private let onAction: () -> Void

    public init(
        name: String, rating: String, verifiedLabel: String, rows: [(BzrIconKey, String)],
        action: String, onAction: @escaping () -> Void = {}
    ) {
        self.name = name
        self.rating = rating
        self.verifiedLabel = verifiedLabel
        self.rows = rows
        self.action = action
        self.onAction = onAction
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: DesignSpace.s) {
            ProviderHeader(
                name: name, rating: rating, services: nil, size: .large, verifiedLabel: verifiedLabel)
            Color.clear.frame(height: DesignSpace.s)
            ForEach(rows.indices, id: \.self) { index in
                MenuRow(icon: rows[index].0, text: rows[index].1)
            }
            Color.clear.frame(height: DesignSpace.s)
            PrimaryButton(text: action, onClick: onAction)
        }
    }
}

// ── Selection & fields ───────────────────────────────────────────────────

public struct StepIndicator: View {
    private let current: Int
    private let total: Int
    private let label: String

    public init(current: Int, total: Int, label: String) {
        self.current = current
        self.total = total
        self.label = label
    }

    private func state(_ step: Int) -> StepState {
        step < current ? .done : (step == current ? .active : .pending)
    }

    public var body: some View {
        VStack(spacing: DesignSpace.s) {
            HStack(spacing: 0) {
                ForEach(1...max(total, 1), id: \.self) { step in
                    StepCircle(step: step, state: state(step))
                    if step < total {
                        Rectangle()
                            .fill(step < current ? DesignColors.primary600 : DesignColors.border)
                            .frame(maxWidth: .infinity)
                            .frame(height: DesignSize.stepperLine)
                    }
                }
            }
            .frame(width: DesignSize.stepIndicatorWidth)
            BzrText(label, style: DesignType.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct StepCircle: View {
    let step: Int
    let state: StepState

    var body: some View {
        let filled = state != .pending
        let filledColor = state == .onHold ? DesignColors.star : DesignColors.primary600
        ZStack {
            Circle().fill(filled ? filledColor : DesignColors.surface)
            Circle().strokeBorder(
                filled ? filledColor : DesignColors.border, lineWidth: DesignSize.controlStroke)
            switch state {
            case .done:
                BzrIcon(.check, tint: DesignColors.onPrimary, size: DesignSize.iconSmall)
            case .active:
                BzrText(BzrFormat.number(step), style: DesignType.caption.colored(DesignColors.onPrimary))
            case .onHold:
                BzrText(BzrFormat.number(step), style: DesignType.caption.colored(DesignColors.onPrimary))
            case .pending:
                BzrText(BzrFormat.number(step), style: DesignType.caption)
            }
        }
        .frame(width: DesignSize.stepCircle, height: DesignSize.stepCircle)
    }
}

public struct SelectableTile: View {
    private let text: String
    private let icon: BzrIconKey
    private let state: SelectionState
    private let categoryIcon: String?

    /// `categoryIcon` is the `icon_path` file name from `/catalog`, drawn in its own two tones (DEC-059).
    public init(text: String, icon: BzrIconKey, state: SelectionState, categoryIcon: String? = nil) {
        self.text = text
        self.icon = icon
        self.state = state
        self.categoryIcon = categoryIcon
    }

    public var body: some View {
        VStack(spacing: DesignSpace.s) {
            if let asset = categoryIcon.flatMap(CategoryIcons.asset) {
                Image(asset, bundle: .module)
                    .resizable()
                    .frame(width: DesignSize.tileIcon, height: DesignSize.tileIcon)
                    .accessibilityHidden(true)
            } else {
                BzrIcon(
                    icon, tint: state == .selected ? DesignColors.primary600 : DesignColors.navy800,
                    size: DesignSize.tileIcon)
            }
            BzrText(text, alignment: .center, lineLimit: 1)
        }
        .padding(DesignSpace.s)
        .frame(maxWidth: .infinity)
        .frame(height: DesignSize.categoryTileH)
        .selectionFrame(state, radius: DesignRadius.card)
    }
}

/// `fillWidth` stands in for the `Modifier.weight(1f)` a Compose caller passes.
public struct SelectableChip: View {
    private let text: String
    private let state: SelectionState
    private let height: CGFloat
    public init(text: String, state: SelectionState, height: CGFloat = DesignSize.chipHeight) {
        self.text = text
        self.state = state
        self.height = height
    }

    public var body: some View {
        BzrText(text, lineLimit: 1)
            .padding(.horizontal, DesignSpace.m)
            .frame(height: height)
            .selectionFrame(state, radius: DesignRadius.chip)
            .frame(minHeight: DesignSize.touchTargetMin)
            .contentShape(Rectangle())
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(text)
            .accessibilityAddTraits(state == .selected ? [.isButton, .isSelected] : .isButton)
            .disabled(state == .disabled)
    }
}

public struct RadioCard: View {
    private let title: String
    private let detail: String
    private let selected: Bool

    public init(title: String, body: String, selected: Bool) {
        self.title = title
        self.detail = body
        self.selected = selected
    }

    public var body: some View {
        HStack(spacing: DesignSpace.m) {
            ZStack {
                Circle().strokeBorder(
                    selected ? DesignColors.primary600 : DesignColors.slate300,
                    lineWidth: DesignSize.controlStroke)
                if selected {
                    Circle().fill(DesignColors.primary600).frame(
                        width: DesignSize.radioDot, height: DesignSize.radioDot)
                }
            }
            .frame(width: DesignSize.radio, height: DesignSize.radio)
            VStack(alignment: .leading, spacing: DesignSpace.xs) {
                BzrText(title, style: DesignType.cardTitle)
                BzrText(detail, style: DesignType.secondary)
            }
            Spacer(minLength: 0)
        }
        .padding(DesignSpace.l)
        .frame(maxWidth: .infinity)
        .selectionFrame(selected ? .selected : .unselected, radius: DesignRadius.card)
    }
}

public struct CheckRow: View {
    private let text: String
    private let checked: Bool

    public init(text: String, checked: Bool) {
        self.text = text
        self.checked = checked
    }

    public var body: some View {
        HStack(spacing: DesignSpace.m) {
            ZStack {
                RoundedRectangle(cornerRadius: DesignRadius.checkbox).fill(
                    checked ? DesignColors.primary600 : DesignColors.surface)
                RoundedRectangle(cornerRadius: DesignRadius.checkbox)
                    .strokeBorder(
                        checked ? DesignColors.primary600 : DesignColors.slate300,
                        lineWidth: DesignSize.controlStroke)
                if checked {
                    BzrIcon(.check, tint: DesignColors.onPrimary, size: DesignSize.iconSmall)
                }
            }
            .frame(width: DesignSize.checkbox, height: DesignSize.checkbox)
            BzrText(text).frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(DesignSpace.m)
        .bzrFrame(
            radius: DesignRadius.field, fill: checked ? DesignColors.primary50 : DesignColors.surface,
            border: DesignColors.border, width: DesignBorder.width)
    }
}

private struct FieldShell<Content: View>: View {
    let label: String
    let state: FieldVisualState
    let focused: Bool
    let error: String?
    let optionalLabel: String?
    let height: CGFloat
    let topAligned: Bool
    let padding: EdgeInsets
    let content: Content

    init(
        label: String, state: FieldVisualState, focused: Bool, error: String?, optionalLabel: String?,
        height: CGFloat, topAligned: Bool, padding: EdgeInsets,
        @ViewBuilder content: () -> Content
    ) {
        self.label = label
        self.state = state
        self.focused = focused
        self.error = error
        self.optionalLabel = optionalLabel
        self.height = height
        self.topAligned = topAligned
        self.padding = padding
        self.content = content()
    }

    private var effective: FieldVisualState {
        focused && state != .error && state != .disabled ? .focused : state
    }

    private var borderColor: Color {
        switch effective {
        case .error: return DesignColors.danger
        case .focused: return DesignColors.primary600
        default: return DesignColors.border
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: DesignSpace.xs) {
            HStack(spacing: 0) {
                BzrText(label, style: DesignType.secondary).frame(maxWidth: .infinity, alignment: .leading)
                if let optionalLabel {
                    BzrText(optionalLabel, style: DesignType.caption)
                }
            }
            HStack(alignment: topAligned ? .top : .center, spacing: DesignSpace.s) {
                content
            }
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: topAligned ? .topLeading : .leading)
            .frame(height: height, alignment: topAligned ? .top : .center)
            .bzrFrame(
                radius: DesignRadius.field,
                fill: DesignColors.surface,
                border: borderColor,
                width: effective == .error || effective == .focused
                    ? DesignBorder.selectedWidth : DesignBorder.width
            )
            if let error {
                BzrText(error, style: DesignType.caption.colored(DesignColors.danger))
            }
        }
        .opacity(state == .disabled ? DesignOpacity.disabled : 1)
    }
}

/// Static text when no handler is given (gallery / snapshots); a real input otherwise.
private struct FieldValue: View {
    let value: String
    let placeholder: String
    let enabled: Bool
    let singleLine: Bool
    let onValueChange: ((String) -> Void)?
    let accessibilityLabel: String
    let imeAction: FieldImeAction

    var body: some View {
        Group {
            if let onValueChange {
                ZStack(alignment: .topLeading) {
                    if value.isEmpty {
                        BzrText(placeholder, style: DesignType.placeholder)
                    }
                    TextField(
                        "", text: Binding(get: { value }, set: onValueChange),
                        axis: singleLine ? .horizontal : .vertical
                    )
                    .font(.custom(DesignType.body.fontName, size: DesignType.body.size))
                    .foregroundStyle(DesignType.body.color)
                    .accessibilityLabel(accessibilityLabel)
                    .submitLabel(imeAction == .next ? .next : .done)
                    .disabled(!enabled)
                }
            } else {
                BzrText(
                    value.isEmpty ? placeholder : value,
                    style: value.isEmpty ? DesignType.placeholder : DesignType.body,
                    lineLimit: singleLine ? 1 : nil
                )
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

public struct AppTextField: View {
    @FocusState private var focused: Bool
    private let label: String
    private let placeholder: String
    private let value: String
    private let state: FieldVisualState
    private let error: String?
    private let optionalLabel: String?
    private let imeAction: FieldImeAction
    private let onValueChange: ((String) -> Void)?

    public init(
        label: String, placeholder: String, value: String, state: FieldVisualState,
        error: String? = nil, optionalLabel: String? = nil,
        imeAction: FieldImeAction = .next, onValueChange: ((String) -> Void)? = nil
    ) {
        self.label = label
        self.placeholder = placeholder
        self.value = value
        self.state = state
        self.error = error
        self.optionalLabel = optionalLabel
        self.imeAction = imeAction
        self.onValueChange = onValueChange
    }

    public var body: some View {
        FieldShell(
            label: label, state: state, focused: focused, error: error, optionalLabel: optionalLabel,
            height: DesignSize.fieldHeight, topAligned: false,
            padding: EdgeInsets(top: 0, leading: DesignSpace.m, bottom: 0, trailing: DesignSpace.m)
        ) {
            FieldValue(
                value: value, placeholder: placeholder, enabled: state != .disabled, singleLine: true,
                onValueChange: onValueChange, accessibilityLabel: label, imeAction: imeAction
            )
            .focused($focused)
        }
    }
}

public struct SecureTextField: View {
    @FocusState private var focused: Bool
    private let label: String
    private let placeholder: String
    private let value: String
    private let state: FieldVisualState
    private let error: String?
    private let optionalLabel: String?
    private let onValueChange: ((String) -> Void)?

    public init(
        label: String, placeholder: String, value: String, state: FieldVisualState,
        error: String? = nil, optionalLabel: String? = nil, onValueChange: ((String) -> Void)? = nil
    ) {
        self.label = label
        self.placeholder = placeholder
        self.value = value
        self.state = state
        self.error = error
        self.optionalLabel = optionalLabel
        self.onValueChange = onValueChange
    }

    public var body: some View {
        FieldShell(
            label: label, state: state, focused: focused, error: error, optionalLabel: optionalLabel,
            height: DesignSize.fieldHeight, topAligned: false,
            padding: EdgeInsets(top: 0, leading: DesignSpace.m, bottom: 0, trailing: DesignSpace.m)
        ) {
            Group {
                if let onValueChange {
                    ZStack(alignment: .leading) {
                        if value.isEmpty {
                            BzrText(placeholder, style: DesignType.placeholder)
                        }
                        SecureField("", text: Binding(get: { value }, set: onValueChange))
                            .font(.custom(DesignType.body.fontName, size: DesignType.body.size))
                            .foregroundStyle(DesignType.body.color)
                            .disabled(state == .disabled)
                    }
                } else {
                    BzrText(
                        value.isEmpty ? placeholder : String(repeating: "•", count: value.count),
                        style: value.isEmpty ? DesignType.placeholder : DesignType.body)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .focused($focused)
        }
    }
}

public struct AmountField: View {
    @FocusState private var focused: Bool
    private let label: String
    private let placeholder: String
    private let value: String
    private let currency: String
    private let state: FieldVisualState
    private let error: String?
    private let optionalLabel: String?
    private let onValueChange: ((String) -> Void)?

    public init(
        label: String, placeholder: String, value: String, currency: String, state: FieldVisualState,
        error: String? = nil, optionalLabel: String? = nil,
        onValueChange: ((String) -> Void)? = nil
    ) {
        self.label = label
        self.placeholder = placeholder
        self.value = value
        self.currency = currency
        self.state = state
        self.error = error
        self.optionalLabel = optionalLabel
        self.onValueChange = onValueChange
    }

    public var body: some View {
        FieldShell(
            label: label, state: state, focused: focused, error: error, optionalLabel: optionalLabel,
            height: DesignSize.fieldHeight, topAligned: false,
            padding: EdgeInsets(
                top: DesignSpace.xs, leading: DesignSpace.m, bottom: DesignSpace.xs,
                trailing: DesignSpace.xs)
        ) {
            FieldValue(
                value: value, placeholder: placeholder, enabled: state != .disabled, singleLine: true,
                onValueChange: onValueChange, accessibilityLabel: label, imeAction: .next
            )
            .focused($focused)
            BzrText(currency)
                .frame(width: DesignSize.currencyBoxWidth)
                .frame(maxHeight: .infinity)
                .background(DesignColors.surfaceAlt2)
                .clipShape(RoundedRectangle(cornerRadius: DesignRadius.badge))
        }
    }
}

public struct TextAreaField: View {
    @FocusState private var focused: Bool
    private let label: String
    private let placeholder: String
    private let value: String
    private let state: FieldVisualState
    private let error: String?
    private let optionalLabel: String?
    private let onValueChange: ((String) -> Void)?

    public init(
        label: String, placeholder: String, value: String, state: FieldVisualState,
        error: String? = nil, optionalLabel: String? = nil, onValueChange: ((String) -> Void)? = nil
    ) {
        self.label = label
        self.placeholder = placeholder
        self.value = value
        self.state = state
        self.error = error
        self.optionalLabel = optionalLabel
        self.onValueChange = onValueChange
    }

    public var body: some View {
        FieldShell(
            label: label, state: state, focused: focused, error: error, optionalLabel: optionalLabel,
            height: DesignSize.textAreaHeight, topAligned: true,
            padding: EdgeInsets(
                top: DesignSpace.m, leading: DesignSpace.m, bottom: DesignSpace.m, trailing: DesignSpace.m)
        ) {
            FieldValue(
                value: value, placeholder: placeholder, enabled: state != .disabled, singleLine: false,
                onValueChange: onValueChange, accessibilityLabel: label, imeAction: .done
            )
            .focused($focused)
        }
    }
}

// ── Cards & data ─────────────────────────────────────────────────────────

public struct SummaryCard: View {
    private let title: String
    private let rows: [(BzrIconKey, String)]
    private let editText: String?
    private let onEdit: () -> Void

    public init(
        title: String, rows: [(BzrIconKey, String)], editText: String?,
        onEdit: @escaping () -> Void = {}
    ) {
        self.title = title
        self.rows = rows
        self.editText = editText
        self.onEdit = onEdit
    }

    public var body: some View {
        BzrCard {
            HStack(spacing: 0) {
                BzrText(title, style: DesignType.cardTitle).frame(maxWidth: .infinity, alignment: .leading)
                if let editText {
                    Button(action: onEdit) {
                        BzrText(editText, style: DesignType.caption.colored(DesignColors.primary700))
                            .padding(.horizontal, DesignSpace.m)
                            .padding(.vertical, DesignSpace.xs)
                            .background(DesignColors.primary50)
                            .clipShape(RoundedRectangle(cornerRadius: DesignRadius.badge))
                    }
                    .buttonStyle(BzrPressStyle())
                }
            }
            ForEach(rows.indices, id: \.self) { index in
                if index > 0 { HorizontalLine() }
                HStack(spacing: DesignSpace.m) {
                    BzrIcon(rows[index].0, tint: DesignColors.primary600)
                    BzrText(rows[index].1)
                }
            }
        }
    }
}

public struct Badge: View {
    private let text: String
    private let kind: BadgeKind
    private let icon: BzrIconKey?

    public init(text: String, kind: BadgeKind = .highlight, icon: BzrIconKey? = nil) {
        self.text = text
        self.kind = kind
        self.icon = icon
    }

    public var body: some View {
        HStack(spacing: DesignSpace.xs) {
            if let icon {
                BzrIcon(icon, tint: DesignColors.star, size: DesignSize.iconSmall)
            }
            BzrText(
                text,
                style: DesignType.badge.colored(
                    kind == .highlight ? DesignColors.badgeText : DesignColors.primary700), lineLimit: 1)
        }
        .padding(.horizontal, DesignSpace.s)
        .padding(.vertical, DesignSpace.xs)
        .background(kind == .highlight ? DesignColors.badgeBg : DesignColors.primary50Available)
        .clipShape(RoundedRectangle(cornerRadius: DesignRadius.badge))
    }
}

private struct Avatar: View {
    let size: CGFloat

    var body: some View {
        BzrIcon(.account, label: bzrString("a11y.avatar"), tint: DesignColors.slate400)
            .frame(width: size, height: size)
            .background(DesignColors.surfaceAlt)
            .clipShape(Circle())
    }
}

private struct VerifiedAvatar: View {
    let size: CGFloat

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            Avatar(size: size)
            ZStack {
                Circle().fill(DesignColors.primary500)
                Circle().strokeBorder(DesignColors.surface, lineWidth: DesignSize.controlStroke)
                BzrIcon(
                    .shield, label: bzrString("a11y.verified"), tint: DesignColors.onPrimary,
                    size: DesignSize.iconSmall)
            }
            .frame(width: DesignSize.verifiedShield, height: DesignSize.verifiedShield)
        }
        .frame(width: size, height: size)
    }
}

private struct RatingLine: View {
    let rating: String
    let services: String?

    var body: some View {
        HStack(spacing: DesignSpace.s) {
            HStack(spacing: DesignSpace.xs) {
                BzrIcon(.starFilled, tint: DesignColors.star, size: DesignSize.iconSmall)
                BzrText(rating, lineLimit: 1)
            }
            if let services {
                BzrText(services, style: DesignType.secondary, lineLimit: 1)
            }
        }
    }
}

public struct ProviderHeader: View {
    private let name: String
    private let rating: String
    private let services: String?
    private let size: AvatarSize
    private let verifiedLabel: String?

    public init(
        name: String, rating: String, services: String?, size: AvatarSize, verifiedLabel: String? = nil
    ) {
        self.name = name
        self.rating = rating
        self.services = services
        self.size = size
        self.verifiedLabel = verifiedLabel
    }

    public var body: some View {
        HStack(spacing: DesignSpace.m) {
            VerifiedAvatar(size: size.value)
            VStack(alignment: .leading, spacing: DesignSpace.xs) {
                BzrText(name, style: DesignType.cardTitle)
                if let verifiedLabel {
                    HStack(spacing: DesignSpace.xs) {
                        Circle().fill(DesignColors.primary500).frame(
                            width: DesignSize.statusDot, height: DesignSize.statusDot)
                        BzrText(verifiedLabel, style: DesignType.secondary.colored(DesignColors.primary700))
                    }
                }
                RatingLine(rating: rating, services: services)
            }
        }
    }
}

public struct OfferCard: View {
    private let provider: String
    private let rating: String
    private let services: String
    private let price: String
    private let detail: String
    private let badge: String
    private let button: String
    private let chatLabel: String
    private let variant: OfferVariant
    private let priceCaption: String?
    private let showSelect: Bool
    private let showChat: Bool
    private let onOpen: () -> Void
    private let onSelect: () -> Void
    private let onChat: () -> Void

    public init(
        provider: String, rating: String, services: String, price: String, detail: String,
        badge: String,
        button: String, chatLabel: String, variant: OfferVariant, priceCaption: String? = nil,
        showSelect: Bool = true, showChat: Bool = true,
        onOpen: @escaping () -> Void = {}, onSelect: @escaping () -> Void = {},
        onChat: @escaping () -> Void = {}
    ) {
        self.provider = provider
        self.rating = rating
        self.services = services
        self.price = price
        self.detail = detail
        self.badge = badge
        self.button = button
        self.chatLabel = chatLabel
        self.variant = variant
        self.priceCaption = priceCaption
        self.showSelect = showSelect
        self.showChat = showChat
        self.onOpen = onOpen
        self.onSelect = onSelect
        self.onChat = onChat
    }

    public var body: some View {
        BzrCard {
            HStack(alignment: .top, spacing: DesignSpace.m) {
                VerifiedAvatar(size: DesignSize.avatarSmall)
                VStack(alignment: .leading, spacing: DesignSpace.xs) {
                    BzrText(provider, style: DesignType.cardTitle)
                    RatingLine(rating: rating, services: services)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                VStack(alignment: .trailing, spacing: DesignSpace.xs) {
                    if !badge.isEmpty { Badge(text: badge, kind: .highlight, icon: .starFilled) }
                    if variant == .inspection, let priceCaption {
                        BzrText(priceCaption, style: DesignType.caption)
                    }
                    BzrText(price, style: DesignType.price)
                    HStack(spacing: DesignSpace.xs) {
                        BzrIcon(
                            variant == .scheduled ? .calendar : .clock, tint: DesignColors.slate500,
                            size: DesignSize.iconSmall)
                        BzrText(detail, style: DesignType.secondary)
                    }
                }
            }
            .contentShape(Rectangle())
            .onTapGesture(perform: onOpen)
            if showSelect || showChat {
                HorizontalLine()
                HStack(spacing: DesignSpace.s) {
                    if showSelect {
                        PrimaryButton(text: button, height: DesignSize.compactButtonHeight, onClick: onSelect)
                    }
                    if showChat { IconSquareButton(label: chatLabel, onClick: onChat) }
                }
            }
        }
    }
}

/// Scrolls horizontally when the chips do not fit the screen width (C06 at 400pt is already at the limit).
public struct SortChips: View {
    private let title: String?
    private let labels: [String]
    private let selectedIndex: Int
    private let onSelect: (Int) -> Void

    public init(
        title: String?, labels: [String], selectedIndex: Int,
        onSelect: @escaping (Int) -> Void = { _ in }
    ) {
        self.title = title
        self.labels = labels
        self.selectedIndex = selectedIndex
        self.onSelect = onSelect
    }

    public var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: DesignSpace.s) {
                if let title {
                    BzrText(title, lineLimit: 1)
                }
                ForEach(labels.indices, id: \.self) { index in
                    Button {
                        onSelect(index)
                    } label: {
                        SelectableChip(
                            text: labels[index], state: index == selectedIndex ? .selected : .unselected,
                            height: DesignSize.sortChipHeight)
                    }
                    .buttonStyle(BzrPressStyle())
                }
            }
        }
    }
}

public struct StatItem {
    public let icon: BzrIconKey
    public let value: String
    public let label: String

    public init(icon: BzrIconKey, value: String, label: String) {
        self.icon = icon
        self.value = value
        self.label = label
    }
}

public struct StatRow: View {
    private let items: [StatItem]

    public init(items: [StatItem]) { self.items = items }

    public var body: some View {
        BzrCard {
            HStack(spacing: 0) {
                ForEach(items.indices, id: \.self) { index in
                    VStack(spacing: DesignSpace.xs) {
                        BzrIcon(items[index].icon, tint: DesignColors.primary500)
                        BzrText(items[index].value, style: DesignType.cardTitle)
                        BzrText(
                            items[index].label, style: DesignType.secondary, alignment: .center, lineLimit: 1)
                    }
                    .frame(maxWidth: .infinity)
                    if index != items.count - 1 {
                        VerticalLine(height: DesignSize.statDivider)
                    }
                }
            }
        }
    }
}

public struct RatingBars: View {
    private let items: [(String, Double)]
    private let max: Int

    public init(items: [(String, Double)], max: Int) {
        self.items = items
        self.max = max
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: DesignSpace.s) {
            ForEach(items.indices, id: \.self) { index in
                HStack(spacing: DesignSpace.m) {
                    BzrText(items[index].0, style: DesignType.secondary, lineLimit: 1)
                        .frame(width: DesignSize.ratingLabelWidth, alignment: .leading)
                    GeometryReader { geometry in
                        HStack(spacing: 0) {
                            Capsule()
                                .fill(DesignColors.primary500)
                                .frame(width: geometry.size.width * CGFloat(items[index].1 / Double(max)))
                            Spacer(minLength: 0)
                        }
                        .background(Capsule().fill(DesignColors.surfaceAlt))
                    }
                    .frame(height: DesignSize.ratingBarHeight)
                    BzrText(
                        BzrFormat.number(items[index].1),
                        style: DesignType.caption.colored(DesignColors.navy800))
                }
            }
        }
    }
}

private struct Stars: View {
    let count: Int

    var body: some View {
        HStack(spacing: 0) {
            ForEach(0..<count, id: \.self) { _ in
                BzrIcon(.starFilled, tint: DesignColors.star, size: DesignSize.iconSmall)
            }
        }
    }
}

public struct ReviewCard: View {
    private let name: String
    private let detail: String
    private let verified: String
    private let time: String
    private let tag: String?
    private let rating: Int

    public init(name: String, body: String, verified: String, time: String, tag: String?, rating: Int) {
        self.name = name
        self.detail = body
        self.verified = verified
        self.time = time
        self.tag = tag
        self.rating = rating
    }

    public var body: some View {
        BzrCard {
            HStack(alignment: .top, spacing: DesignSpace.m) {
                Avatar(size: DesignSize.avatarXs)
                VStack(alignment: .leading, spacing: DesignSpace.xs) {
                    BzrText(
                        name, style: DesignType.body.weighted(DesignFont.bold).colored(DesignColors.navy900))
                    HStack(spacing: DesignSpace.s) {
                        Stars(count: rating)
                        BzrIcon(.check, tint: DesignColors.primary600, size: DesignSize.iconSmall)
                        BzrText(verified, style: DesignType.caption.colored(DesignColors.primary700))
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                BzrText(time, style: DesignType.caption)
            }
            BzrText(detail)
            if let tag {
                BzrText(tag, style: DesignType.caption)
                    .padding(.horizontal, DesignSpace.s)
                    .padding(.vertical, DesignSpace.xs)
                    .background(DesignColors.surfaceAlt)
                    .clipShape(RoundedRectangle(cornerRadius: DesignRadius.badge))
            }
        }
    }
}

public struct StickyActionBar: View {
    private let priceLabel: String
    private let price: String
    private let action: String
    private let chatLabel: String
    private let onAction: () -> Void
    private let onChat: () -> Void

    public init(
        priceLabel: String, price: String, action: String, chatLabel: String,
        onAction: @escaping () -> Void = {}, onChat: @escaping () -> Void = {}
    ) {
        self.priceLabel = priceLabel
        self.price = price
        self.action = action
        self.chatLabel = chatLabel
        self.onAction = onAction
        self.onChat = onChat
    }

    public var body: some View {
        VStack(spacing: 0) {
            HorizontalLine()
            HStack(spacing: DesignSpace.m) {
                VStack(alignment: .leading, spacing: 0) {
                    BzrText(priceLabel, style: DesignType.caption)
                    BzrText(price, style: DesignType.price, lineLimit: 1)
                }
                PrimaryButton(text: action, onClick: onAction)
                IconSquareButton(label: chatLabel, onClick: onChat)
            }
            .padding(.vertical, DesignSpace.m)
        }
        .background(DesignColors.surface)
    }
}

// ── Status & feedback ────────────────────────────────────────────────────

private struct Banner: View {
    let text: String
    let icon: BzrIconKey
    let iconTint: Color
    let fill: Color
    let foreground: Color

    var body: some View {
        HStack(spacing: DesignSpace.m) {
            BzrIcon(icon, tint: iconTint)
            BzrText(text, style: DesignType.body.colored(foreground)).frame(
                maxWidth: .infinity, alignment: .leading)
        }
        .padding(DesignSpace.l)
        .background(fill)
        .clipShape(RoundedRectangle(cornerRadius: DesignRadius.card))
    }
}

public struct InfoBanner: View {
    private let text: String
    public init(text: String) { self.text = text }
    public var body: some View {
        Banner(
            text: text, icon: .shield, iconTint: DesignColors.primary600,
            fill: DesignColors.primary50Info, foreground: DesignColors.primary700)
    }
}

public struct WarningBox: View {
    private let text: String
    public init(text: String) { self.text = text }
    public var body: some View {
        Banner(
            text: text, icon: .warningFilled, iconTint: DesignColors.star, fill: DesignColors.warningBg,
            foreground: DesignColors.warningText)
    }
}

public struct OfflineBanner: View {
    private let text: String
    public init(text: String) { self.text = text }
    public var body: some View {
        Banner(
            text: text, icon: .offline, iconTint: DesignColors.navy800, fill: DesignColors.surfaceAlt,
            foreground: DesignColors.navy800)
    }
}

public struct EtaCard: View {
    private let value: String
    private let unit: String
    private let title: String
    private let subtitle: String

    public init(value: String, unit: String, title: String, subtitle: String) {
        self.value = value
        self.unit = unit
        self.title = title
        self.subtitle = subtitle
    }

    public var body: some View {
        BzrCard {
            HStack(spacing: DesignSpace.m) {
                HStack(alignment: .bottom, spacing: DesignSpace.xs) {
                    BzrText(value, style: DesignType.displayNumber, lineLimit: 1)
                    BzrText(unit, style: DesignType.cardTitle.colored(DesignColors.primary700), lineLimit: 1)
                }
                VerticalLine(height: DesignSize.statDivider)
                VStack(alignment: .leading, spacing: DesignSpace.xs) {
                    BzrText(title, style: DesignType.cardTitle, lineLimit: 1)
                    BzrText(subtitle, style: DesignType.secondary, lineLimit: 1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                ZStack {
                    Circle().fill(DesignColors.primary50Info)
                    BzrIcon(.car, tint: DesignColors.primary600)
                }
                .frame(width: DesignSize.etaIconCircle, height: DesignSize.etaIconCircle)
            }
        }
    }
}

/// Placeholder frame for the map SDK view (Google Maps / MapKit are platform views, 43 §7).
public struct MapCard: View {
    private let title: String
    private let onMyLocation: () -> Void

    public init(title: String, onMyLocation: @escaping () -> Void = {}) {
        self.title = title
        self.onMyLocation = onMyLocation
    }

    public var body: some View {
        VStack(spacing: DesignSpace.s) {
            BzrIcon(.location, tint: DesignColors.primary600)
            BzrText(title, style: DesignType.secondary)
        }
        .frame(maxWidth: .infinity)
        .frame(height: DesignSize.mapCardHeight)
        .overlay(alignment: .bottomTrailing) {
            Button(action: onMyLocation) {
                BzrIcon(.navigation, label: bzrString("a11y.my_location"))
                    .frame(width: DesignSize.myLocationButton, height: DesignSize.myLocationButton)
                    .bzrFrame(
                        radius: DesignRadius.button, fill: DesignColors.surface,
                        border: DesignColors.border, width: DesignBorder.width)
            }
            .buttonStyle(BzrPressStyle())
            .padding(DesignSpace.m)
        }
        .bzrFrame(
            radius: DesignRadius.card, fill: DesignColors.surfaceAlt2, border: DesignColors.border,
            width: DesignBorder.width)
    }
}

private struct StepperDot: View {
    let state: StepState

    var body: some View {
        switch state {
        case .done:
            ZStack {
                Circle().fill(DesignColors.primary600)
                BzrIcon(.check, tint: DesignColors.onPrimary, size: DesignSize.iconXs)
            }
            .frame(width: DesignSize.stepperDot, height: DesignSize.stepperDot)
        case .active:
            ZStack {
                Circle().fill(DesignColors.surface)
                Circle().strokeBorder(DesignColors.primary600, lineWidth: DesignSize.controlStroke)
                Circle().fill(DesignColors.primary600).frame(
                    width: DesignSize.radioDot, height: DesignSize.radioDot)
            }
            .frame(width: DesignSize.stepperDotActive, height: DesignSize.stepperDotActive)
        case .onHold:
            ZStack {
                Circle().fill(DesignColors.surface)
                Circle().strokeBorder(DesignColors.star, lineWidth: DesignSize.controlStroke)
                Circle().fill(DesignColors.star).frame(
                    width: DesignSize.radioDot, height: DesignSize.radioDot)
            }
            .frame(width: DesignSize.stepperDotActive, height: DesignSize.stepperDotActive)
        case .pending:
            ZStack {
                Circle().fill(DesignColors.surface)
                Circle().strokeBorder(DesignColors.border, lineWidth: DesignSize.controlStroke)
            }
            .frame(width: DesignSize.stepperDot, height: DesignSize.stepperDot)
        }
    }
}

/// Horizontal stepper as in C09: the line toward the previous step is teal once this step is reached.
public struct StatusStepper: View {
    private let steps: [(String, StepState)]

    public init(steps: [(String, StepState)]) { self.steps = steps }

    private func lineColor(reached: Bool, hidden: Bool) -> Color {
        hidden ? Color.clear : (reached ? DesignColors.primary600 : DesignColors.border)
    }

    private func labelStyle(_ state: StepState) -> BzrTextStyle {
        switch state {
        case .active: return DesignType.caption.weighted(DesignFont.bold).colored(DesignColors.navy900)
        case .onHold: return DesignType.caption.weighted(DesignFont.bold).colored(DesignColors.star)
        case .done: return DesignType.caption.colored(DesignColors.navy800)
        case .pending: return DesignType.caption
        }
    }

    public var body: some View {
        HStack(alignment: .top, spacing: 0) {
            ForEach(steps.indices, id: \.self) { index in
                VStack(spacing: DesignSpace.s) {
                    ZStack {
                        HStack(spacing: 0) {
                            Rectangle()
                                .fill(lineColor(reached: steps[index].1 != .pending, hidden: index == 0))
                                .frame(height: DesignSize.stepperLine)
                            Rectangle()
                                .fill(
                                    lineColor(
                                        reached: index < steps.count - 1 && steps[index + 1].1 != .pending,
                                        hidden: index == steps.count - 1)
                                )
                                .frame(height: DesignSize.stepperLine)
                        }
                        StepperDot(state: steps[index].1)
                    }
                    .frame(height: DesignSize.stepperDotActive)
                    BzrText(
                        steps[index].0, style: labelStyle(steps[index].1), alignment: .center, lineLimit: 2)
                }
                .frame(maxWidth: .infinity)
            }
        }
    }
}

public struct OrderCard: View {
    private let title: String
    private let subtitle: String
    private let status: String
    private let onClick: () -> Void

    public init(title: String, subtitle: String, status: String, onClick: @escaping () -> Void = {}) {
        self.title = title
        self.subtitle = subtitle
        self.status = status
        self.onClick = onClick
    }

    public var body: some View {
        Button(action: onClick) {
            BzrCard {
                HStack(spacing: DesignSpace.m) {
                    VStack(alignment: .leading, spacing: DesignSpace.xs) {
                        BzrText(title, style: DesignType.cardTitle)
                        BzrText(subtitle, style: DesignType.secondary)
                        Badge(text: status, kind: .status)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    BzrIcon(.chevron, tint: DesignColors.slate400)
                }
            }
        }
        .buttonStyle(BzrPressStyle())
    }
}

public struct AppBottomSheet: View {
    private let title: String
    private let detail: String
    private let action: String
    private let actionState: ButtonVisualState
    private let secondaryAction: String?
    private let onAction: () -> Void
    private let onSecondary: () -> Void

    public init(
        title: String, body: String, action: String, actionState: ButtonVisualState = .normal,
        secondaryAction: String? = nil,
        onAction: @escaping () -> Void = {}, onSecondary: @escaping () -> Void = {}
    ) {
        self.title = title
        self.detail = body
        self.action = action
        self.actionState = actionState
        self.secondaryAction = secondaryAction
        self.onAction = onAction
        self.onSecondary = onSecondary
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: DesignSpace.m) {
            Capsule()
                .fill(DesignColors.border)
                .frame(width: DesignSize.sheetHandleW, height: DesignSize.sheetHandleH)
                .frame(maxWidth: .infinity)
            BzrText(title, style: DesignType.sectionTitle)
            BzrText(detail)
            PrimaryButton(text: action, state: actionState, onClick: onAction)
            if let secondaryAction { SecondaryButton(text: secondaryAction, onClick: onSecondary) }
        }
        .padding(.horizontal, DesignSpace.screenHorizontal)
        .padding(.vertical, DesignSpace.l)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DesignColors.surface)
        .clipShape(TopRoundedRectangle(radius: DesignRadius.sheetTop))
    }
}

private struct TopRoundedRectangle: Shape {
    let radius: CGFloat

    func path(in rect: CGRect) -> Path {
        Path(
            UIBezierPath(
                roundedRect: rect,
                byRoundingCorners: [.topLeft, .topRight],
                cornerRadii: CGSize(width: radius, height: radius)
            ).cgPath)
    }
}

private struct FeedbackState<Action: View>: View {
    let icon: BzrIconKey
    let iconTint: Color
    let iconBackground: Color
    let title: String
    let detail: String
    let action: Action

    var body: some View {
        VStack(spacing: DesignSpace.s) {
            ZStack {
                Circle().fill(iconBackground)
                BzrIcon(icon, tint: iconTint)
            }
            .frame(width: DesignSize.avatarSmall, height: DesignSize.avatarSmall)
            BzrText(title, style: DesignType.cardTitle, alignment: .center)
            BzrText(detail, style: DesignType.secondary, alignment: .center)
            action
        }
        .frame(maxWidth: .infinity)
        .padding(DesignSpace.l)
    }
}

public struct EmptyState: View {
    private let title: String
    private let detail: String
    private let action: String?
    private let onAction: () -> Void

    public init(
        title: String, body: String, action: String? = nil, onAction: @escaping () -> Void = {}
    ) {
        self.title = title
        self.detail = body
        self.action = action
        self.onAction = onAction
    }

    public var body: some View {
        FeedbackState(
            icon: .empty, iconTint: DesignColors.primary600, iconBackground: DesignColors.primary50,
            title: title, detail: detail
        ) {
            if let action { PrimaryButton(text: action, onClick: onAction) }
        }
    }
}

public struct ErrorState: View {
    private let title: String
    private let detail: String
    private let retry: String?
    private let onRetry: () -> Void

    public init(title: String, body: String, retry: String? = nil, onRetry: @escaping () -> Void = {}) {
        self.title = title
        self.detail = body
        self.retry = retry
        self.onRetry = onRetry
    }

    public var body: some View {
        FeedbackState(
            icon: .error, iconTint: DesignColors.danger, iconBackground: DesignColors.surfaceAlt,
            title: title, detail: detail
        ) {
            if let retry { SecondaryButton(text: retry, onClick: onRetry) }
        }
    }
}

extension FeedbackState {
    fileprivate init(
        icon: BzrIconKey, iconTint: Color, iconBackground: Color, title: String, detail: String,
        @ViewBuilder action: () -> Action
    ) {
        self.icon = icon
        self.iconTint = iconTint
        self.iconBackground = iconBackground
        self.title = title
        self.detail = detail
        self.action = action()
    }
}

public struct LoadingSkeleton: View {
    public init() {}

    public var body: some View {
        BzrCard {
            HStack(spacing: DesignSpace.m) {
                Circle().fill(DesignColors.surfaceAlt).frame(
                    width: DesignSize.avatarSmall, height: DesignSize.avatarSmall)
                VStack(alignment: .leading, spacing: DesignSpace.s) {
                    SkeletonLine(fraction: 1)
                    SkeletonLine(fraction: DesignRatio.skeletonShortLine)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(bzrString("a11y.loading"))
    }
}

private struct SkeletonLine: View {
    let fraction: CGFloat

    var body: some View {
        GeometryReader { geometry in
            HStack(spacing: 0) {
                RoundedRectangle(cornerRadius: DesignRadius.badge)
                    .fill(DesignColors.surfaceAlt)
                    .frame(width: geometry.size.width * fraction)
                Spacer(minLength: 0)
            }
        }
        .frame(height: DesignSize.skeletonLine)
    }
}

/// Turns `danger` below DesignThreshold.countdownUrgentSeconds (43 §3).
public struct Countdown: View {
    private let seconds: Int
    private let label: String

    public init(seconds: Int, label: String) {
        self.seconds = seconds
        self.label = label
    }

    public var body: some View {
        let urgent = seconds < DesignThreshold.countdownUrgentSeconds
        VStack(spacing: 0) {
            BzrText(
                BzrFormat.countdown(seconds),
                style: DesignType.displayNumber.colored(
                    urgent ? DesignColors.danger : DesignColors.primary700))
            BzrText(label, style: DesignType.secondary)
        }
    }
}

// ── Content ──────────────────────────────────────────────────────────────

public struct ChatBubble: View {
    private let text: String
    private let sent: Bool
    private let kind: ChatKind

    public init(text: String, sent: Bool, kind: ChatKind = .text) {
        self.text = text
        self.sent = sent
        self.kind = kind
    }

    private var blocked: Bool { kind == .blocked }
    private var fill: Color { sent && !blocked ? DesignColors.primary600 : DesignColors.surface }
    private var borderColor: Color {
        blocked ? DesignColors.danger : (sent ? DesignColors.primary600 : DesignColors.border)
    }
    private var foreground: Color {
        blocked ? DesignColors.danger : (sent ? DesignColors.onPrimary : DesignColors.navy800)
    }

    public var body: some View {
        HStack(spacing: 0) {
            if !sent { Spacer(minLength: 0) }
            HStack(spacing: DesignSpace.s) {
                switch kind {
                case .image: BzrIcon(.image, tint: foreground)
                case .blocked: BzrIcon(.warning, tint: foreground)
                case .text: EmptyView()
                }
                BzrText(text, style: DesignType.body.colored(foreground))
            }
            .padding(DesignSpace.m)
            .bzrFrame(
                radius: DesignRadius.card, fill: fill, border: borderColor, width: DesignBorder.width
            )
            .frame(maxWidth: DesignSize.chatBubbleMaxWidth, alignment: sent ? .leading : .trailing)
            if sent { Spacer(minLength: 0) }
        }
    }
}

public struct ChatInput: View {
    private let placeholder: String
    private let value: String
    private let onValueChange: ((String) -> Void)?
    private let onSend: () -> Void

    public init(
        placeholder: String, value: String = "", onValueChange: ((String) -> Void)? = nil,
        onSend: @escaping () -> Void = {}
    ) {
        self.placeholder = placeholder
        self.value = value
        self.onValueChange = onValueChange
        self.onSend = onSend
    }

    public var body: some View {
        HStack(spacing: DesignSpace.s) {
            FieldValue(
                value: value, placeholder: placeholder, enabled: true, singleLine: true,
                onValueChange: onValueChange, accessibilityLabel: placeholder, imeAction: .done
            )
            Button(action: onSend) {
                BzrIcon(.send, label: bzrString("a11y.send"), tint: DesignColors.primary600)
            }
            .buttonStyle(BzrPressStyle())
        }
        .padding(.horizontal, DesignSpace.m)
        .frame(height: DesignSize.fieldHeight)
        .bzrFrame(
            radius: DesignRadius.field, fill: DesignColors.surface, border: DesignColors.border,
            width: DesignBorder.width)
    }
}

public struct MediaThumb: View {
    private let title: String
    private let stateLabel: String
    private let kind: MediaKind
    private let state: MediaState
    private let readOnly: Bool
    private let onDelete: () -> Void

    public init(
        title: String, stateLabel: String, kind: MediaKind, state: MediaState,
        readOnly: Bool = false, onDelete: @escaping () -> Void = {}
    ) {
        self.title = title
        self.stateLabel = stateLabel
        self.kind = kind
        self.state = state
        self.readOnly = readOnly
        self.onDelete = onDelete
    }

    private var icon: BzrIconKey {
        switch kind {
        case .photo: return .image
        case .video: return .video
        case .audio: return .mic
        }
    }

    public var body: some View {
        BzrCard {
            HStack(spacing: DesignSpace.m) {
                BzrIcon(icon, tint: DesignColors.primary600)
                    .frame(width: DesignSize.menuIconBox, height: DesignSize.menuIconBox)
                    .background(DesignColors.surfaceAlt)
                    .clipShape(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox))
                VStack(alignment: .leading, spacing: DesignSpace.xs) {
                    BzrText(title)
                    if !stateLabel.isEmpty {
                        BzrText(
                            stateLabel,
                            style: DesignType.caption.colored(
                                state == .failed ? DesignColors.danger : DesignColors.slate500))
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                if !readOnly {
                    switch state {
                    case .uploading: ProgressArc(color: DesignColors.primary600, label: stateLabel)
                    case .uploaded: BzrIcon(.check, label: stateLabel, tint: DesignColors.primary600)
                    case .failed: BzrIcon(.error, label: stateLabel, tint: DesignColors.danger)
                    }
                    Button(action: onDelete) {
                        BzrIcon(.trash, label: bzrString("a11y.delete"), tint: DesignColors.danger)
                    }
                    .buttonStyle(BzrPressStyle())
                }
            }
        }
    }
}
