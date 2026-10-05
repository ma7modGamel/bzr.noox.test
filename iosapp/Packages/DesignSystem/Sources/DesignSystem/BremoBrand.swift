import SwiftUI

/// Original artwork stays upright in both layout directions.
public struct BremoBrandMark: View {
    private let size: CGFloat

    public init(size: CGFloat = DesignSize.brandAuthLogo) {
        self.size = size
    }

    public var body: some View {
        Image("bremo_brand_icon", bundle: .module)
            .renderingMode(.original)
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)
            .clipShape(RoundedRectangle(cornerRadius: size * DesignRatio.brandCorner))
            .accessibilityLabel(bzrString("app.name"))
    }
}

/// A restrained breathing mark; removing this view also stops its animation.
public struct BremoBrandLoader: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    public init() {}

    public var body: some View {
        VStack(spacing: DesignSpace.m) {
            if reduceMotion {
                BremoBrandMark(size: DesignSize.brandLoaderLogo)
            } else {
                BremoBrandMark(size: DesignSize.brandLoaderLogo)
                    .phaseAnimator([false, true]) { content, resting in
                        content
                            .opacity(resting ? 1 : DesignRatio.loadingDim)
                            .scaleEffect(resting ? 1 : DesignRatio.brandLoadingScale)
                    } animation: { _ in
                        .easeInOut(duration: Double(DesignMotion.brandPulseMs) / 1000)
                    }
            }
            BzrText(bzrString("brand.loading"), style: DesignType.secondary, alignment: .center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, DesignSpace.l)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(bzrString("a11y.loading"))
    }
}
