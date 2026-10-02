import CoreText
import SwiftUI
import UIKit

/// Registers the four static Cairo files (one per weight, 43 §2) once per process.
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
    String(localized: String.LocalizationValue(key), bundle: .module)
}

extension BzrTextStyle {
    public func colored(_ color: Color) -> BzrTextStyle {
        BzrTextStyle(size: size, fontName: fontName, color: color)
    }
    public func weighted(_ fontName: String) -> BzrTextStyle {
        BzrTextStyle(size: size, fontName: fontName, color: color)
    }
}

/// Text in a token style. Dynamic Type follows the environment and is capped at DesignA11y.maxFontScale.
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
        return min(scaled, style.size * DesignA11y.maxFontScale)
    }

    public var body: some View {
        Text(value)
            .font(.custom(style.fontName, fixedSize: scaledSize))
            .foregroundStyle(style.color)
            .multilineTextAlignment(alignment)
            .lineLimit(lineLimit)
    }
}

public struct BzrIcon: View {
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
            .accessibilityLabel(label ?? "")
            .accessibilityHidden(label == nil)
    }
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
        .bzrFrame(
            radius: DesignRadius.card, fill: DesignColors.surface, border: DesignColors.border,
            width: DesignBorder.width)
    }
}

/// Renders a button's label untouched, so disabled / pressed looks come only from the tokens (no system dimming).
public struct BzrPressStyle: ButtonStyle {
    public init() {}
    public func makeBody(configuration: Configuration) -> some View {
        configuration.label.contentShape(Rectangle())
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

/// RTL always, light always (43 §2).
public struct BzrTheme<Content: View>: View {
    private let content: Content

    public init(@ViewBuilder content: () -> Content) {
        BzrFontRegistrar.register()
        self.content = content()
    }

    public var body: some View {
        content
            .environment(\.layoutDirection, .rightToLeft)
            .environment(\.colorScheme, .light)
            .preferredColorScheme(.light)
            .tint(DesignColors.primary600)
    }
}
