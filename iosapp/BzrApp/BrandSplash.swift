import DesignSystem
import SwiftUI

/// DEC-064: the animated launch that follows the launch screen. It starts on the launch screen's colour with the
/// same symbol at the same size and place, so the hand-off is seamless; then the brand gradient turns slowly, a soft
/// glow breathes behind the symbol, two rings ripple out and the name rises in. The app fades it out when the first
/// screen is ready. With Reduce Motion it is a still gradient.
struct BrandSplash: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var entered = false
    @State private var startedAt = Date()

    var body: some View {
        TimelineView(.animation(paused: reduceMotion)) { timeline in
            let loop = reduceMotion ? 0 : phase(at: timeline.date)
            ZStack {
                background(loop: loop)
                glow(loop: loop)
                if !reduceMotion {
                    ring(progress: (loop * 2).truncatingRemainder(dividingBy: 1))
                    ring(progress: (loop * 2 + 0.5).truncatingRemainder(dividingBy: 1))
                }
                Image("BremoLaunchSymbol")
                    .resizable()
                    .scaledToFit()
                    .frame(width: DesignSize.brandLaunchLogo)
                    .scaleEffect(1 + Self.breath * (1 - cos(loop * 4 * .pi)) / 2)
                    // The symbol stays centred like the launch screen; the name hangs a gap below it.
                    .overlay(alignment: .bottom) {
                        BzrText(
                            bzrString("app.launcher_name"), style: DesignType.heroTitle.colored(DesignColors.onPrimary)
                        )
                        .fixedSize()
                        .alignmentGuide(.bottom) { $0[.top] - DesignSpace.xl }
                        .opacity(entered ? 1 : 0)
                        .offset(y: entered ? 0 : DesignSpace.l)
                    }
            }
        }
        .accessibilityElement()
        .accessibilityLabel(bzrString("app.name"))
        .onAppear {
            startedAt = Date()
            withAnimation(.easeOut(duration: Double(DesignMotion.splashEnterMs) / 1000)) { entered = true }
        }
    }

    private func phase(at date: Date) -> Double {
        date.timeIntervalSince(startedAt).truncatingRemainder(dividingBy: Double(DesignMotion.splashLoopMs) / 1000)
            / (Double(DesignMotion.splashLoopMs) / 1000)
    }

    /// The gradient axis swings a quarter turn each way around the brand diagonal.
    private func background(loop: Double) -> some View {
        let angle = Angle.degrees(Self.diagonal + Self.swing * sin(loop * 2 * .pi))
        let dx = cos(angle.radians) / 2
        let dy = sin(angle.radians) / 2
        return ZStack {
            DesignColors.brandBackdrop
            LinearGradient(
                colors: DesignGradients.splashColors,
                startPoint: UnitPoint(x: 0.5 - dx, y: 0.5 - dy), endPoint: UnitPoint(x: 0.5 + dx, y: 0.5 + dy)
            )
            .opacity(entered ? 1 : 0)
        }
        .ignoresSafeArea()
    }

    private func glow(loop: Double) -> some View {
        let breathe = (1 + sin(loop * 4 * .pi)) / 2
        let radius = DesignSize.brandLaunchLogo * DesignRatio.splashRingScale * (0.75 + 0.25 * breathe)
        return RadialGradient(
            colors: [DesignColors.onPrimary.opacity(DesignRatio.splashGlow), DesignColors.onPrimary.opacity(0)],
            center: .center, startRadius: 0, endRadius: radius
        )
        .opacity(entered ? 1 : 0)
        .ignoresSafeArea()
    }

    private func ring(progress: Double) -> some View {
        let diameter = DesignSize.brandLaunchLogo * (1 + (DesignRatio.splashRingScale - 1) * progress)
        return Circle()
            .stroke(
                DesignColors.onPrimary.opacity(DesignRatio.splashGlow * 2 * (1 - progress) * (entered ? 1 : 0)),
                lineWidth: DesignSize.controlStroke
            )
            .frame(width: diameter, height: diameter)
    }

    /// Top-start → bottom-end, the direction of every brand gradient (DEC-063).
    private static let diagonal = 45.0
    /// How far the gradient axis swings each way.
    private static let swing = 45.0
    /// How much the symbol grows at the top of each breath.
    private static let breath = 0.04
}
