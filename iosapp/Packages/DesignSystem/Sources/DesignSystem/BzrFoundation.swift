import CoreText
import SwiftUI
import UIKit

/// Registers the four static UI font files (one per weight, 43 §2) once per process.
public enum BzrFontRegistrar {
    private static var didRegister = false

    public static func register() {
        guard !didRegister else { return }
        for file in DesignFont.files {
            let url =
                Bundle.module.url(forResource: file, withExtension: "ttf", subdirectory: "Fonts")
                ?? Bundle.module.url(forResource: file, withExtension: "ttf")
            if let url {
                CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
            }
        }
        didRegister = true
    }
}

public func bzrString(_ key: String) -> String {
    let value = String(localized: String.LocalizationValue(key), bundle: .module)
    assert(value != key, "Missing localization: \(key)")
    return value
}

public func bzrPlural(_ key: String, count: Int) -> String {
    let catalogKey = "plural." + key
    let template = Bundle.module.localizedString(forKey: catalogKey, value: nil, table: nil)
    assert(template != catalogKey, "Missing plural localization: \(key)")
    var value = String.localizedStringWithFormat(template, Int32(clamping: count))
    for (arabic, western) in zip("٠١٢٣٤٥٦٧٨٩", "0123456789") {
        value = value.replacingOccurrences(of: String(arabic), with: String(western))
    }
    return value
}

extension BzrTextStyle {
    public func colored(_ color: Color) -> BzrTextStyle {
        BzrTextStyle(size: size, fontName: fontName, color: color)
    }
    public func weighted(_ fontName: String) -> BzrTextStyle {
        BzrTextStyle(size: size, fontName: fontName, color: color)
    }
}

/// Text in a token style. The layout grows with the user's Dynamic Type preference.
public struct BzrText: View {
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    private let value: String
    private let style: BzrTextStyle
    private let alignment: TextAlignment
    private let lineLimit: Int?

    public init(
        _ value: String, style: BzrTextStyle = DesignType.body, alignment: TextAlignment = .leading,
        lineLimit: Int? = nil
    ) {
        self.value = value
        self.style = style
        self.alignment = alignment
        self.lineLimit = lineLimit
    }

    private var scaledSize: CGFloat {
        let traits = UITraitCollection(
            preferredContentSizeCategory: UIContentSizeCategory(dynamicTypeSize))
        let scaled = UIFontMetrics.default.scaledValue(for: style.size, compatibleWith: traits)
        return scaled
    }

    public var body: some View {
        Text(value)
            .font(.custom(style.fontName, fixedSize: scaledSize))
            .foregroundStyle(style.color)
            .multilineTextAlignment(alignment)
            .lineLimit(lineLimit)
            .fixedSize(horizontal: false, vertical: true)
    }
}

public struct BzrIcon: View {
    @Environment(\.layoutDirection) private var layoutDirection
    private let key: BzrIconKey
    private let label: String?
    private let tint: Color
    private let size: CGFloat

    public init(
        _ key: BzrIconKey, label: String? = nil, tint: Color = DesignIcon.color,
        size: CGFloat = DesignSize.icon
    ) {
        self.key = key
        self.label = label
        self.tint = tint
        self.size = size
    }

    public var body: some View {
        Image(key.rawValue, bundle: .module)
            .renderingMode(.template)
            .resizable()
            .foregroundStyle(tint)
            .frame(width: size, height: size)
            .scaleEffect(x: isDirectional && layoutDirection == .rightToLeft ? -1 : 1, y: 1)
            .accessibilityLabel(label ?? "")
            .accessibilityHidden(label == nil)
    }

    private var isDirectional: Bool { key == .arrowBack || key == .arrowForward || key == .chevron }
}

public struct BzrCard<Content: View>: View {
    private let content: Content

    public init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: DesignSpace.m) {
            content
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(DesignSpace.cardPadding)
        // DEC-070: Figma cards are white on the tinted screen with a soft shadow and no outline.
        .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
        .bzrShadow(DesignShadows.card)
    }
}

/// Quiet press feedback without moving the hit target or surrounding layout.
public struct BzrPressStyle: ButtonStyle {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    public init() {}
    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .contentShape(Rectangle())
            .opacity(configuration.isPressed ? DesignRatio.pressFeedback : 1)
            .animation(
                reduceMotion ? nil : .easeOut(duration: Double(DesignMotion.navBarMs) / 1000),
                value: configuration.isPressed)
    }
}

extension View {
    /// Fill + inner border + clip, matching Compose `background().border()` (border drawn inside the bounds).
    public func bzrFrame(radius: CGFloat, fill: Color, border: Color, width: CGFloat) -> some View {
        self
            .background(RoundedRectangle(cornerRadius: radius).fill(fill))
            .overlay(RoundedRectangle(cornerRadius: radius).strokeBorder(border, lineWidth: width))
            .clipShape(RoundedRectangle(cornerRadius: radius))
    }
}

/// Layout follows the selected localization; Arabic remains the source language.
public struct BzrTheme<Content: View>: View {
    private let content: Content

    public init(@ViewBuilder content: () -> Content) {
        BzrFontRegistrar.register()
        self.content = content()
    }

    public var body: some View {
        content
            .environment(\.colorScheme, .light)
            .preferredColorScheme(.light)
            .tint(DesignColors.primary600)
    }
}

/// Content-sized service tiles reflow for narrow phones and larger accessibility text.
public struct BzrServiceGrid<Content: View>: View {
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    private let content: Content
    private let columns: Int?

    /// `columns` fixes the column count (DEC-070: two for the request service tiles); nil adapts to the width.
    public init(columns: Int? = nil, @ViewBuilder content: () -> Content) {
        self.columns = columns
        self.content = content()
    }

    private var minimumWidth: CGFloat {
        let traits = UITraitCollection(
            preferredContentSizeCategory: UIContentSizeCategory(dynamicTypeSize))
        let width = UIFontMetrics.default.scaledValue(
            for: DesignSize.categoryColumnMinWidth, compatibleWith: traits)
        return min(width, DesignSize.mobileContentWidth - DesignSpace.screenHorizontal * 2)
    }

    public var body: some View {
        LazyVGrid(
            columns: columns.map { Array(repeating: GridItem(.flexible(), spacing: DesignSpace.m), count: $0) }
                ?? [GridItem(.adaptive(minimum: minimumWidth), spacing: DesignSpace.m)],
            alignment: .leading, spacing: DesignSpace.m
        ) { content }
    }
}

/// DEC-063: a line icon filled with a gradient (brand by default), the SwiftUI twin of Android `GradientIconView`.
public struct BzrGradientIcon: View {
    private let key: BzrIconKey
    private let gradient: LinearGradient
    private let size: CGFloat

    public init(_ key: BzrIconKey, gradient: LinearGradient = DesignGradients.iconBrand, size: CGFloat = DesignSize.icon) {
        self.key = key
        self.gradient = gradient
        self.size = size
    }

    public var body: some View {
        Image(key.rawValue, bundle: .module)
            .renderingMode(.template)
            .resizable()
            .frame(width: size, height: size)
            .foregroundStyle(gradient)
            .accessibilityHidden(true)
    }
}

extension View {
    /// DEC-063: a token shadow, or none.
    public func bzrShadow(_ style: DesignShadowStyle?) -> some View {
        shadow(color: style?.color ?? .clear, radius: style?.radius ?? 0, y: style?.y ?? 0)
    }
}
