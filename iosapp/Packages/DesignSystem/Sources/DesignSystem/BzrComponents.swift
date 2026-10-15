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
public enum BadgeKind {
    case highlight, status, success, warning, neutral, danger

    public static func forOrderStatus(_ status: String) -> BadgeKind {
        switch status {
        case "CLOSED": return .success
        case "OPEN", "AWAITING_QUOTE_APPROVAL", "AWAITING_PAYMENT", "AWAITING_CONFIRMATION": return .warning
        case "CANCELLED", "EXPIRED": return .neutral
        case "DISPUTED": return .danger
        default: return .status
        }
    }

    var foreground: Color {
        switch self {
        case .highlight: return DesignColors.badgeText
        case .status: return DesignColors.infoText
        case .success: return DesignColors.success
        case .warning: return DesignColors.warningText
        case .neutral: return DesignColors.neutralText
        case .danger: return DesignColors.danger
        }
    }

    var background: Color {
        switch self {
        case .highlight: return DesignColors.badgeBg
        case .status: return DesignColors.infoBg
        case .success: return DesignColors.successTint
        case .warning: return DesignColors.warningBg
        case .neutral: return DesignColors.neutralBg
        case .danger: return DesignColors.dangerTint
        }
    }

    public var statusIcon: BzrIconKey {
        switch self {
        case .success: return .check
        case .warning: return .clock
        case .neutral: return .close
        case .danger: return .warning
        default: return .orders
        }
    }
}
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

/// Native indeterminate progress is visible for the entire asynchronous action.
private struct ProgressArc: View {
    let color: Color
    let label: String

    var body: some View {
        ProgressView()
            .tint(color)
            .frame(width: DesignSize.icon, height: DesignSize.icon)
            .accessibilityLabel(label)
    }
}

extension View {
    /// DEC-070 (Figma «Primo App»): the chosen state is a tint (plus a check where the component draws one), never an outline.
    fileprivate func selectionFrame(
        _ state: SelectionState, radius: CGFloat, unselectedFill: Color = DesignColors.surface
    ) -> some View {
        let selected = state == .selected
        return
            self
            .background(RoundedRectangle(cornerRadius: radius).fill(selected ? DesignBorder.selectedFill : unselectedFill))
            .clipShape(RoundedRectangle(cornerRadius: radius))
            .opacity(state == .disabled ? DesignOpacity.disabled : 1)
    }
}

// ── Actions & navigation ─────────────────────────────────────────────────

public struct AppTopBar: View {
    private let title: String
    private let actionIcon: BzrIconKey?
    private let actionLabel: String?
    private let onBack: () -> Void
    private let showsBack: Bool
    private let onAction: () -> Void

    public init(
        title: String, actionIcon: BzrIconKey? = nil, actionLabel: String? = nil,
        showsBack: Bool = true, onBack: @escaping () -> Void = {}, onAction: @escaping () -> Void = {}
    ) {
        self.title = title
        self.showsBack = showsBack
        self.actionIcon = actionIcon
        self.actionLabel = actionLabel
        self.onBack = onBack
        self.onAction = onAction
    }

    public var body: some View {
        HStack(spacing: DesignSpace.s) {
            if showsBack {
                iconButton(.arrowBack, label: bzrString("a11y.back"), action: onBack)
            }
            BzrText(title, style: DesignType.screenTitle)
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityAddTraits(.isHeader)
            if let actionIcon {
                iconButton(actionIcon, label: actionLabel, action: onAction)
            }
        }
        .frame(minHeight: DesignSize.topBarHeight)
        .padding(.horizontal, DesignSpace.screenHorizontal)
        .padding(.vertical, DesignSpace.xs)
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

/// 43 §3 PrimaryButton. DEC-070 (Figma «Primo App»): an optional `icon` after the label — the forward arrow on flow
/// buttons, mirrored in RTL — and a `compact` size (smaller label and height) for buttons inside cards.
public struct PrimaryButton: View {
    private let text: String
    private let state: ButtonVisualState
    private let height: CGFloat
    private let icon: BzrIconKey?
    private let compact: Bool
    private let onClick: () -> Void

    public init(
        text: String, state: ButtonVisualState = .normal,
        height: CGFloat = DesignSize.primaryButtonHeight, icon: BzrIconKey? = nil, compact: Bool = false,
        onClick: @escaping () -> Void = {}
    ) {
        self.text = text
        self.state = state
        self.height = compact ? min(height, DesignSize.homeButtonHeight) : height
        self.icon = icon
        self.compact = compact
        self.onClick = onClick
    }

    public var body: some View {
        Button(action: onClick) {
            ZStack {
                if state == .loading {
                    ProgressArc(color: DesignColors.onPrimary, label: bzrString("a11y.loading"))
                } else {
                    HStack(spacing: DesignSpace.s) {
                        BzrText(text, style: compact ? DesignType.buttonSmall : DesignType.button, alignment: .center)
                        if let icon {
                            BzrIcon(icon, tint: DesignColors.onPrimary, size: compact ? DesignSize.iconSmall : DesignSize.icon)
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, DesignSpace.l)
            .padding(.vertical, DesignSpace.m)
            .frame(minHeight: height)
            // DEC-063: brand gradient (deeper when pressed) with a soft brand-coloured shadow.
            .background(state == .pressed ? DesignGradients.brandPressed : DesignGradients.brand)
            .clipShape(RoundedRectangle(cornerRadius: DesignRadius.button))
            .bzrShadow(state == .normal || state == .pressed ? DesignShadows.brand : nil)
        }
        .buttonStyle(BzrPressStyle())
        .disabled(!(state == .normal || state == .pressed))
        .opacity(state == .disabled ? DesignOpacity.disabled : 1)
        .accessibilityLabel(text)
        .accessibilityValue(state == .loading ? bzrString("a11y.loading") : "")
    }
}

public struct SecondaryButton: View {
    private let text: String
    private let enabled: Bool
    private let icon: BzrIconKey?
    private let onClick: () -> Void

    public init(text: String, enabled: Bool = true, icon: BzrIconKey? = nil, onClick: @escaping () -> Void = {}) {
        self.text = text
        self.enabled = enabled
        self.icon = icon
        self.onClick = onClick
    }

    public var body: some View {
        Button(action: onClick) {
            // DEC-070 (Figma «Primo App»): filled with the container tint, no outline; DEC-076: optional leading icon.
            HStack(spacing: DesignSpace.s) {
                if let icon { BzrIcon(icon, tint: DesignColors.primary600, size: DesignSize.iconSmall) }
                BzrText(text, style: DesignType.secondaryButton, alignment: .center)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, DesignSpace.s)
            .frame(minHeight: DesignSize.secondaryButtonVisualHeight)
            .background(RoundedRectangle(cornerRadius: DesignRadius.button).fill(DesignColors.secondaryButtonBg))
            .frame(minHeight: DesignSize.touchTargetMin)
            .contentShape(Rectangle())
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
                .padding(.vertical, DesignSpace.s)
                .frame(minHeight: DesignSize.secondaryButtonHeight)
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
                .padding(.vertical, DesignSpace.s)
                .frame(minHeight: DesignSize.secondaryButtonHeight)
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
                .background(RoundedRectangle(cornerRadius: DesignRadius.button).fill(DesignColors.surfaceContainer))
        }
        .buttonStyle(BzrPressStyle())
    }
}

/// Stable tab destinations with a restrained selection capsule and native button feedback.
/// Stable destinations with readable labels. DEC-070 (Figma «Primo App» nav): a tinted bar lifted by a soft shadow,
/// each tab an icon over its label; the chosen tab takes the primary colour (Figma's lighter teal is darkened for
/// contrast).
public struct BottomNav: View {
    private let items: [(BzrIconKey, String)]
    private let selectedIndex: Int
    private let onSelect: (Int) -> Void
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    public init(
        items: [(BzrIconKey, String)], selectedIndex: Int, onSelect: @escaping (Int) -> Void = { _ in }
    ) {
        self.items = items
        self.selectedIndex = selectedIndex
        self.onSelect = onSelect
    }

    public var body: some View {
        HStack(alignment: .center, spacing: DesignSpace.xs) {
            ForEach(items.indices, id: \.self) { index in
                let tint = index == selectedIndex ? DesignColors.primary600 : DesignColors.slate500
                Button {
                    onSelect(index)
                } label: {
                    VStack(spacing: DesignSpace.xs) {
                        BzrIcon(items[index].0, tint: tint)
                        BzrText(items[index].1, style: DesignType.navLabel.colored(tint), alignment: .center)
                    }
                    .frame(maxWidth: .infinity)
                    .frame(minHeight: DesignSize.touchTargetMin)
                    .contentShape(Rectangle())
                }
                .buttonStyle(BzrPressStyle())
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(items[index].1)
                .accessibilityAddTraits(index == selectedIndex ? [.isSelected] : [])
            }
        }
        .padding(.horizontal, DesignSpace.s)
        .frame(minHeight: DesignSize.bottomNavHeight)
        .background(DesignColors.surfaceAlt.bzrShadow(DesignShadows.card))
        .animation(reduceMotion ? nil : .easeOut(duration: Double(DesignMotion.navBarMs) / 1000), value: selectedIndex)
    }
}

public struct MenuRow: View {
    private let icon: BzrIconKey
    private let text: String
    private let onClick: () -> Void
    private let danger: Bool

    /// `danger` marks a destructive row (sign out): red icon in a red-tinted well, red label (DEC-063).
    public init(icon: BzrIconKey, text: String, onClick: @escaping () -> Void = {}, danger: Bool = false) {
        self.icon = icon
        self.text = text
        self.onClick = onClick
        self.danger = danger
    }

    public var body: some View {
        Button(action: onClick) {
            HStack(spacing: DesignSpace.m) {
                Group {
                    if danger {
                        BzrIcon(icon, tint: DesignColors.danger)
                    } else {
                        BzrGradientIcon(icon)
                    }
                }
                .frame(width: DesignSize.menuIconBox, height: DesignSize.menuIconBox)
                .background {
                    RoundedRectangle(cornerRadius: DesignRadius.menuIconBox)
                        .fill(danger ? AnyShapeStyle(DesignColors.dangerTint) : AnyShapeStyle(DesignGradients.iconWell))
                }
                BzrText(text, style: DesignType.body.colored(danger ? DesignColors.danger : DesignColors.navy900))
                    .frame(maxWidth: .infinity, alignment: .leading)
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

/// 43 §3 StepIndicator, restyled after Figma «Primo App» (DEC-070): «الخطوة n من total» and a hint on one line,
/// then one rounded segment per step — reached segments in the primary colour, the rest in the container tint.
public struct StepIndicator: View {
    private let current: Int
    private let total: Int
    private let label: String
    private let hint: String

    public init(current: Int, total: Int, label: String, hint: String = "") {
        self.current = current
        self.total = total
        self.label = label
        self.hint = hint
    }

    public var body: some View {
        VStack(spacing: DesignSpace.s) {
            HStack {
                BzrText(label, style: DesignType.stepLabel)
                Spacer(minLength: DesignSpace.s)
                if !hint.isEmpty { BzrText(hint, style: DesignType.hint, lineLimit: 1) }
            }
            HStack(spacing: DesignSize.stepSegmentGap) {
                ForEach(1...max(total, 1), id: \.self) { step in
                    Capsule()
                        .fill(step <= current ? DesignColors.primary600 : DesignColors.surfaceContainerHighest)
                        .frame(maxWidth: .infinity)
                        .frame(height: DesignSize.stepSegmentHeight)
                }
            }
            .accessibilityHidden(true)
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .combine)
    }
}

/// 43 §3 SelectableTile. DEC-070 (Figma «Primo App»): white card without an outline, an optional one-line `detail`,
/// and the chosen tile takes the selection tint, a deeper icon well and a check badge at its top-start corner.
public struct SelectableTile: View {
    private let text: String
    private let detail: String
    private let icon: BzrIconKey
    private let state: SelectionState
    private let categoryIcon: String?

    /// `categoryIcon` is the `icon_path` file name from `/catalog` (DEC-059).
    public init(text: String, icon: BzrIconKey, state: SelectionState, categoryIcon: String? = nil, detail: String = "") {
        self.text = text
        self.detail = detail
        self.icon = icon
        self.state = state
        self.categoryIcon = categoryIcon
    }

    public var body: some View {
        let selected = state == .selected
        VStack(spacing: DesignSpace.s) {
            ZStack {
                RoundedRectangle(cornerRadius: DesignRadius.menuIconBox)
                    .fill(selected ? DesignColors.primaryWellSelected : CategoryPalettes.palette(categoryIcon).tint)
                if let asset = categoryIcon.flatMap(CategoryIcons.asset) {
                    Image(asset, bundle: .module)
                        .renderingMode(.template)
                        .resizable()
                        .foregroundStyle(DesignColors.primary600)
                        .frame(width: DesignSize.tileIcon, height: DesignSize.tileIcon)
                        .accessibilityHidden(true)
                } else {
                    BzrIcon(icon, tint: DesignColors.primary600, size: DesignSize.tileIcon)
                }
            }
            .frame(width: DesignSize.tileIconWell, height: DesignSize.tileIconWell)
            BzrText(text, style: DesignType.tileTitle, alignment: .center, lineLimit: 2)
            if !detail.isEmpty {
                BzrText(detail, style: DesignType.hint, alignment: .center, lineLimit: 1)
            }
        }
        .padding(DesignSpace.m)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .frame(minHeight: DesignSize.categoryTileH)
        .selectionFrame(state, radius: DesignRadius.card)
        .overlay(alignment: .topLeading) {
            if selected {
                ZStack {
                    Circle().fill(DesignColors.primary600)
                    BzrIcon(.check, tint: DesignColors.onPrimary, size: DesignSize.iconXs)
                }
                .frame(width: DesignSize.checkBadge, height: DesignSize.checkBadge)
                .padding(DesignSpace.s)
                .accessibilityHidden(true)
            }
        }
        .bzrShadow(DesignShadows.card)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel([text, detail].filter { !$0.isEmpty }.joined(separator: bzrString("format.list_separator")))
        .accessibilityAddTraits(selected ? [.isButton, .isSelected] : .isButton)
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
        BzrText(text, style: .chip, color: state == .selected ? DesignColors.chipSelectedText : DesignColors.chipText, alignment: .center)
            .padding(.horizontal, DesignSpace.m)
            .padding(.vertical, DesignSpace.xs)
            .frame(minHeight: height)
            .selectionFrame(state, radius: DesignRadius.chip, unselectedFill: DesignColors.surfaceContainer)
            .frame(minHeight: DesignSize.touchTargetMin)
            .contentShape(Rectangle())
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(text)
            .accessibilityAddTraits(state == .selected ? [.isButton, .isSelected] : .isButton)
            .disabled(state == .disabled)
    }
}

/// 43 §3 RadioCard. DEC-070 (Figma «Primo App»): white card with a soft shadow and no outline, an optional icon well
/// at the start and badge beside the title; the chosen card takes the selection tint and a filled radio with a check
/// (the unselected radio keeps its outline so it stays visible on white).
public struct RadioCard: View {
    private let title: String
    private let detail: String
    private let selected: Bool
    private let icon: BzrIconKey?
    private let badge: String

    public init(title: String, body: String, selected: Bool, icon: BzrIconKey? = nil, badge: String = "") {
        self.title = title
        self.detail = body
        self.selected = selected
        self.icon = icon
        self.badge = badge
    }

    public var body: some View {
        HStack(spacing: DesignSpace.m) {
            if let icon {
                ZStack {
                    Circle().fill(DesignColors.surfaceContainerHigh)
                    BzrIcon(icon, tint: DesignColors.primary600, size: DesignSize.icon)
                }
                .frame(width: DesignSize.optionWell, height: DesignSize.optionWell)
            }
            VStack(alignment: .leading, spacing: DesignSpace.xs) {
                HStack(spacing: DesignSpace.s) {
                    BzrText(title, style: DesignType.cardTitle)
                    if !badge.isEmpty { Badge(text: badge, kind: .status) }
                }
                if !detail.isEmpty { BzrText(detail, style: DesignType.secondary) }
            }
            Spacer(minLength: 0)
            ZStack {
                if selected {
                    Circle().fill(DesignColors.primary600)
                    BzrIcon(.check, tint: DesignColors.onPrimary, size: DesignSize.iconSmall)
                } else {
                    Circle().strokeBorder(DesignColors.slate300, lineWidth: DesignSize.controlStroke)
                }
            }
            .frame(width: DesignSize.radioCheck, height: DesignSize.radioCheck)
        }
        .padding(DesignSpace.l)
        .frame(maxWidth: .infinity)
        .selectionFrame(selected ? .selected : .unselected, radius: DesignRadius.card)
        .bzrShadow(DesignShadows.card)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(selected ? [.isButton, .isSelected] : .isButton)
    }
}

/// A catalog category's icon (DEC-059) as a single-tone glyph (DEC-070); a wrench when the key is unknown.
public struct CategoryGlyph: View {
    private let key: String?
    private let size: CGFloat

    public init(_ key: String?, size: CGFloat = DesignSize.iconSmall) {
        self.key = key
        self.size = size
    }

    public var body: some View {
        if let asset = key.flatMap(CategoryIcons.asset) {
            Image(asset, bundle: .module)
                .renderingMode(.template)
                .resizable()
                .foregroundStyle(DesignColors.primary600)
                .frame(width: size, height: size)
                .accessibilityHidden(true)
        } else {
            BzrIcon(.wrench, tint: DesignColors.primary600, size: size)
        }
    }
}

/// DEC-073: a photo from `design/images` (generated into the asset catalog as `img_<name>`), filling its frame.
public struct DesignPhoto: View {
    private let name: String

    public init(_ name: String) { self.name = name }

    public var body: some View {
        Image(name, bundle: .module)
            .resizable()
            .scaledToFill()
            .accessibilityHidden(true)
    }
}

/// DEC-070 SearchField (Figma «Primo App» 3002:2): a filled box with a search glyph and a clear button once there is text.
public struct SearchField: View {
    private let placeholder: String
    private let value: String
    private let onValueChange: ((String) -> Void)?

    public init(placeholder: String, value: String, onValueChange: ((String) -> Void)? = nil) {
        self.placeholder = placeholder
        self.value = value
        self.onValueChange = onValueChange
    }

    public var body: some View {
        HStack(spacing: DesignSpace.s) {
            BzrIcon(.search, tint: DesignColors.slate400, size: DesignSize.iconSmall)
            TextField(
                "", text: Binding(get: { value }, set: { onValueChange?($0) }),
                prompt: Text(placeholder).foregroundColor(DesignColors.slate400)
            )
            .font(.custom(DesignType.body.fontName, size: DesignType.body.size, relativeTo: .body))
            .foregroundStyle(DesignType.body.color)
            .submitLabel(.search)
            .disabled(onValueChange == nil)
            if !value.isEmpty {
                Button(action: { onValueChange?("") }) {
                    BzrIcon(.close, label: bzrString("common.clear"), tint: DesignColors.slate400, size: DesignSize.iconSmall)
                }
                .frame(width: DesignSize.touchTargetMin, height: DesignSize.touchTargetMin)
            }
        }
        .padding(.leading, DesignSpace.m)
        .frame(minHeight: DesignSize.searchFieldHeight)
        .background(RoundedRectangle(cornerRadius: DesignRadius.field).fill(DesignColors.surfaceAlt2))
    }
}

/// DEC-070 ActionPill (Figma «Primo App» 3002:144): a compact white card button — icon then label.
public struct ActionPill: View {
    private let text: String
    private let icon: BzrIconKey
    private let onClick: () -> Void

    public init(text: String, icon: BzrIconKey, onClick: @escaping () -> Void = {}) {
        self.text = text
        self.icon = icon
        self.onClick = onClick
    }

    public var body: some View {
        Button(action: onClick) {
            HStack(spacing: DesignSpace.xs) {
                BzrIcon(icon, tint: DesignColors.primary600, size: DesignSize.iconSmall)
                BzrText(text, style: DesignType.caption.colored(DesignColors.navy900), lineLimit: 1)
            }
            .padding(.horizontal, DesignSpace.m)
            .frame(maxWidth: .infinity, minHeight: DesignSize.actionPillHeight)
            .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
            .bzrShadow(DesignShadows.card)
        }
        .buttonStyle(BzrPressStyle())
        .frame(minHeight: DesignSize.touchTargetMin)
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
                    checked ? AnyShapeStyle(DesignGradients.brand) : AnyShapeStyle(DesignColors.surface))
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
            .frame(minHeight: height, alignment: topAligned ? .top : .center)
            .bzrFrame(
                radius: DesignRadius.field,
                fill: DesignColors.surface,
                border: borderColor,
                width: effective == .error || effective == .focused
                    ? DesignBorder.selectedWidth : DesignBorder.width
            )
            if let error {
                HStack(alignment: .top, spacing: DesignSpace.xs) {
                    BzrIcon(.error, tint: DesignColors.danger, size: DesignSize.iconSmall)
                    BzrText(error, style: DesignType.caption.colored(DesignColors.danger))
                }
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
                .padding(.vertical, DesignSpace.s)
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
                            .frame(minWidth: DesignSize.touchTargetMin, minHeight: DesignSize.touchTargetMin)
                    }
                    .buttonStyle(BzrPressStyle())
                }
            }
            ForEach(rows.indices, id: \.self) { index in
                if index > 0 { HorizontalLine() }
                HStack(spacing: DesignSpace.m) {
                    BzrGradientIcon(rows[index].0)
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
                BzrIcon(icon, tint: kind.foreground, size: DesignSize.badgeIcon)
            }
            BzrText(text, style: DesignType.badge.colored(kind.foreground))
        }
        .padding(.horizontal, DesignSpace.s)
        .padding(.vertical, DesignSpace.xs)
        .frame(minHeight: DesignSize.badgeHeight)
        .background(kind.background)
        .clipShape(RoundedRectangle(cornerRadius: DesignRadius.badge))
    }
}

private struct Avatar: View {
    let size: CGFloat

    var body: some View {
        // DEC-063: the brand gradient until a photo exists.
        BzrIcon(.account, label: bzrString("a11y.avatar"), tint: DesignColors.onPrimary)
            .frame(width: size, height: size)
            .background(DesignGradients.iconBrand)
            .clipShape(Circle())
    }
}

private struct VerifiedAvatar: View {
    let size: CGFloat
    var verified = false

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            Avatar(size: size)
            if verified {
                ZStack {
                    Circle().fill(DesignColors.verifiedText)
                    Circle().strokeBorder(DesignColors.surface, lineWidth: DesignSize.controlStroke)
                    BzrIcon(
                        .shield, label: bzrString("a11y.verified"), tint: DesignColors.onPrimary,
                        size: DesignSize.iconSmall)
                }
                .frame(width: DesignSize.verifiedShield, height: DesignSize.verifiedShield)
            }
        }
        .frame(width: size, height: size)
    }
}

private struct RatingLine: View {
    let rating: String
    let services: String?

    var body: some View {
        HStack(spacing: DesignSpace.s) {
            if !rating.isEmpty {
                HStack(spacing: DesignSpace.xs) {
                    BzrIcon(.starFilled, tint: DesignColors.star, size: DesignSize.iconSmall)
                    BzrText(rating)
                }
            }
            if let services {
                BzrText(services, style: DesignType.secondary)
            }
        }
    }
}

public struct ProviderHeader: View {
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
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
        headerLayout {
            VerifiedAvatar(size: size.value, verified: verifiedLabel != nil)
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
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var headerLayout: AnyLayout {
        dynamicTypeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: DesignSpace.m))
            : AnyLayout(HStackLayout(spacing: DesignSpace.m))
    }
}

/// 43 §3 OfferCard, after Figma «Primo App» 3002:446 (DEC-073): avatar (initial) with an online dot, name and
/// «موثّق», subtitle, rating and review count, price and its caption; an arrival / materials strip; the technician's
/// note; «اختيار العرض ←», «عرض الملف» and chat.
public struct OfferCard: View {
    private let provider: String
    private let subtitle: String
    private let rating: String
    private let reviews: String
    private let price: String
    private let priceCaption: String
    private let detail: String
    private let materials: String
    private let note: String
    private let verified: Bool
    private let verifiedLabel: String
    private let button: String
    private let profileLabel: String
    private let chatLabel: String
    private let showSelect: Bool
    private let showChat: Bool
    private let availableNow: Bool
    private let onOpen: () -> Void
    private let onSelect: () -> Void
    private let onChat: () -> Void

    public init(
        provider: String, subtitle: String = "", rating: String, reviews: String = "", price: String,
        priceCaption: String = "", detail: String, materials: String = "", note: String = "", verified: Bool = false,
        verifiedLabel: String = "", button: String, profileLabel: String = "", chatLabel: String,
        showSelect: Bool = true, showChat: Bool = true, availableNow: Bool = false, onOpen: @escaping () -> Void = {},
        onSelect: @escaping () -> Void = {}, onChat: @escaping () -> Void = {}
    ) {
        self.provider = provider
        self.subtitle = subtitle
        self.rating = rating
        self.reviews = reviews
        self.price = price
        self.priceCaption = priceCaption
        self.detail = detail
        self.materials = materials
        self.note = note
        self.verified = verified
        self.verifiedLabel = verifiedLabel
        self.button = button
        self.profileLabel = profileLabel
        self.chatLabel = chatLabel
        self.showSelect = showSelect
        self.showChat = showChat
        self.availableNow = availableNow
        self.onOpen = onOpen
        self.onSelect = onSelect
        self.onChat = onChat
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: DesignSpace.m) {
            HStack(alignment: .top, spacing: DesignSpace.m) {
                BzrText(String(provider.trimmingCharacters(in: .whitespaces).prefix(1)), style: DesignType.offerName.colored(DesignColors.primary600))
                    .frame(width: DesignSize.offerAvatar, height: DesignSize.offerAvatar)
                    .background(Circle().fill(DesignColors.surfaceContainer))
                    .overlay(alignment: .bottomLeading) {
                        if availableNow {
                            Circle().fill(DesignColors.success)
                                .overlay(Circle().strokeBorder(DesignColors.surface, lineWidth: DesignSize.controlStroke))
                                .frame(width: DesignSize.onlineDot, height: DesignSize.onlineDot)
                        }
                    }
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 0) {
                    HStack(spacing: DesignSpace.s) {
                        BzrText(provider, style: DesignType.offerName, lineLimit: 1)
                        if verified {
                            HStack(spacing: DesignSpace.xs) {
                                BzrIcon(.verified, tint: DesignColors.verifiedText, size: DesignSize.badgeIcon)
                                BzrText(verifiedLabel, style: DesignType.statusPill.colored(DesignColors.verifiedText))
                            }
                            .padding(.horizontal, DesignSpace.s)
                            .background(RoundedRectangle(cornerRadius: DesignRadius.badge).fill(DesignColors.verifiedBg))
                        }
                    }
                    if !subtitle.isEmpty { BzrText(subtitle, style: DesignType.secondary) }
                    if !rating.isEmpty {
                        HStack(spacing: DesignSpace.xs) {
                            BzrIcon(.starFilled, tint: DesignColors.star, size: DesignSize.iconSmall)
                            BzrText(rating, style: DesignType.detailStrong)
                            if !reviews.isEmpty { BzrText(reviews, style: DesignType.hint) }
                        }
                    }
                }
                Spacer(minLength: 0)
                VStack(alignment: .trailing, spacing: 0) {
                    BzrText(price, style: DesignType.offerPrice)
                    if !priceCaption.isEmpty { BzrText(priceCaption, style: DesignType.hint) }
                }
            }
            if !detail.isEmpty || !materials.isEmpty {
                HStack(spacing: DesignSpace.s) {
                    if !detail.isEmpty {
                        BzrIcon(.clock, tint: DesignColors.primary600, size: DesignSize.iconSmall)
                        BzrText(detail, style: DesignType.detailStrong)
                    }
                    Spacer(minLength: DesignSpace.s)
                    if !materials.isEmpty {
                        BzrIcon(.wrench, tint: DesignColors.slate500, size: DesignSize.iconSmall)
                        BzrText(materials, style: DesignType.caption)
                    }
                }
                .padding(DesignSpace.m)
                .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.surfaceAlt2))
            }
            if !note.isEmpty {
                BzrText(note, style: DesignType.quote)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(DesignSpace.m)
                    .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.surfaceAlt2))
            }
            HStack(spacing: DesignSpace.s) {
                if showSelect {
                    PrimaryButton(text: button, state: .normal, icon: .arrowForward, compact: true, onClick: onSelect)
                }
                if !profileLabel.isEmpty {
                    SecondaryButton(text: profileLabel, onClick: onOpen).fixedSize()
                }
                if showChat { IconSquareButton(label: chatLabel, onClick: onChat) }
            }
        }
        .padding(DesignSpace.xl)
        .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
        .bzrShadow(DesignShadows.card)
    }
}

/// 43 §3 SortChips. DEC-073 (Figma 3002:446): optional leading `icons`; the chosen chip is filled with the primary colour.
public struct SortChips: View {
    private let title: String?
    private let labels: [String]
    private let selectedIndex: Int
    private let icons: [BzrIconKey]
    private let onSelect: (Int) -> Void

    public init(
        title: String?, labels: [String], selectedIndex: Int, icons: [BzrIconKey] = [],
        onSelect: @escaping (Int) -> Void = { _ in }
    ) {
        self.title = title
        self.labels = labels
        self.selectedIndex = selectedIndex
        self.icons = icons
        self.onSelect = onSelect
    }

    public var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: DesignSpace.s) {
                if let title {
                    BzrText(title, lineLimit: 1)
                }
                ForEach(labels.indices, id: \.self) { index in
                    let selected = index == selectedIndex
                    let tint = selected ? DesignColors.onPrimary : DesignColors.navy900
                    Button {
                        onSelect(index)
                    } label: {
                        HStack(spacing: DesignSpace.xs) {
                            if icons.indices.contains(index) { BzrIcon(icons[index], tint: tint, size: DesignSize.iconSmall) }
                            BzrText(labels[index], style: DesignType.chip.colored(tint), lineLimit: 1)
                        }
                        .padding(.horizontal, DesignSpace.l)
                        .frame(minHeight: DesignSize.sortChipHeight)
                        .background(Capsule().fill(selected ? DesignColors.primary600 : DesignColors.chipBg))
                        .frame(minHeight: DesignSize.touchTargetMin)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(BzrPressStyle())
                    .accessibilityAddTraits(selected ? [.isSelected] : [])
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
                        BzrGradientIcon(items[index].icon)
                        BzrText(items[index].value, style: DesignType.cardTitle)
                        BzrText(
                            items[index].label, style: DesignType.secondary, alignment: .center)
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
                    BzrText(items[index].0, style: DesignType.secondary)
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
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
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
            actionLayout {
                VStack(alignment: .leading, spacing: 0) {
                    BzrText(priceLabel, style: DesignType.caption)
                    BzrText(price, style: DesignType.price)
                }
                PrimaryButton(text: action, onClick: onAction)
                IconSquareButton(label: chatLabel, onClick: onChat)
            }
            .padding(.vertical, DesignSpace.m)
        }
        .background(DesignColors.surface)
    }

    private var actionLayout: AnyLayout {
        dynamicTypeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: DesignSpace.m))
            : AnyLayout(HStackLayout(spacing: DesignSpace.m))
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
                    BzrText(unit, style: DesignType.cardTitle.colored(DesignColors.primary700))
                }
                VerticalLine(height: DesignSize.statDivider)
                VStack(alignment: .leading, spacing: DesignSpace.xs) {
                    BzrText(title, style: DesignType.cardTitle)
                    BzrText(subtitle, style: DesignType.secondary)
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
                Circle().fill(DesignGradients.brand)
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
/// 43 §3 StatusStepper (C09). DEC-076 (Figma 3002:1631/2540): `vertical` lists the steps top to bottom with optional
/// `subtitles`.
public struct StatusStepper: View {
    private let steps: [(String, StepState)]
    private let subtitles: [String]
    private let vertical: Bool

    public init(steps: [(String, StepState)], subtitles: [String] = [], vertical: Bool = false) {
        self.steps = steps
        self.subtitles = subtitles
        self.vertical = vertical
    }

    @ViewBuilder private var verticalBody: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(steps.indices, id: \.self) { index in
                let state = steps[index].1
                let active = state == .active || state == .onHold
                HStack(alignment: .top, spacing: DesignSpace.m) {
                    ZStack(alignment: .top) {
                        if index < steps.count - 1 {
                            Rectangle()
                                .fill(steps[index + 1].1 != .pending ? DesignColors.primary400 : DesignColors.surfaceContainer)
                                .frame(width: DesignSize.stepperLine)
                                .frame(maxHeight: .infinity)
                        }
                        ZStack {
                            switch state {
                            case .done:
                                Circle().fill(DesignColors.primary400)
                                BzrIcon(.check, tint: DesignColors.onPrimary, size: DesignSize.iconSmall)
                            case .active, .onHold:
                                Circle().fill(DesignColors.primary50)
                                Circle().strokeBorder(DesignColors.primary400, lineWidth: DesignSize.controlStroke)
                                Circle().fill(DesignColors.primary600).frame(width: DesignSize.radioDot, height: DesignSize.radioDot)
                            case .pending:
                                Circle().fill(DesignColors.surfaceContainer)
                                Circle().fill(DesignColors.slate400).frame(width: DesignSize.radioDot, height: DesignSize.radioDot)
                            }
                        }
                        .frame(width: DesignSize.verticalStepDot, height: DesignSize.verticalStepDot)
                    }
                    .frame(width: DesignSize.verticalStepDot)
                    VStack(alignment: .leading, spacing: 0) {
                        BzrText(
                            steps[index].0,
                            style: active
                                ? DesignType.tileTitle.colored(DesignColors.primary600)
                                : DesignType.detailStrong.colored(state == .pending ? DesignColors.slate500 : DesignColors.navy900))
                        if subtitles.indices.contains(index), !subtitles[index].isEmpty {
                            BzrText(
                                subtitles[index],
                                style: DesignType.hint.colored(
                                    active || (index == 0 && state == .done) ? DesignColors.primary600 : DesignColors.slate400))
                        }
                    }
                    .padding(.bottom, DesignSpace.l)
                    Spacer(minLength: 0)
                }
                .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

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
        if vertical { verticalBody } else { horizontalBody }
    }

    private var horizontalBody: some View {
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
    /// DEC-063: the empty state's icon takes the brand gradient in a gradient well.
    var branded = false

    var body: some View {
        VStack(spacing: DesignSpace.s) {
            ZStack {
                if branded {
                    Circle().fill(DesignGradients.iconWell)
                    BzrGradientIcon(icon)
                } else {
                    Circle().fill(iconBackground)
                    BzrIcon(icon, tint: iconTint)
                }
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
            title: title, detail: detail, branded: true
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
        branded: Bool = false, @ViewBuilder action: () -> Action
    ) {
        self.branded = branded
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
            // DEC-063: a sent bubble wears the brand gradient, as on Android.
            .background {
                if sent && !blocked {
                    RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignGradients.brand)
                }
            }
            .bzrFrame(
                radius: DesignRadius.card, fill: sent && !blocked ? .clear : fill, border: sent && !blocked ? .clear : borderColor,
                width: DesignBorder.width
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
                value: value, placeholder: placeholder, enabled: true, singleLine: false,
                onValueChange: onValueChange, accessibilityLabel: placeholder, imeAction: .done
            )
            Button(action: onSend) {
                BzrIcon(.send, label: bzrString("a11y.send"), tint: DesignColors.primary600)
                    .frame(width: DesignSize.touchTargetMin, height: DesignSize.touchTargetMin)
            }
            .buttonStyle(BzrPressStyle())
            .disabled(value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        }
        .padding(.horizontal, DesignSpace.m)
        .padding(.vertical, DesignSpace.xs)
        .frame(minHeight: DesignSize.fieldHeight)
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
