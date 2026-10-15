import BzrCore
import DesignSystem
import SwiftUI

/// SCR-C35 — Figma «Primo App» «طلبك اتنشر» 3002:1519 (DEC-073): the open request while no offer has arrived. Before
/// the offer window closes (edit allowed) it reads as published; once only republish is left it says no offers came.
/// Buttons come from `available_actions`; «متابعة حالة الطلب» reloads the order (and so its offers).
public struct C35NoOffersView: View {
    let state: CustomerUIState
    let onAction: (String) -> Void

    public init(state: CustomerUIState, onAction: @escaping (String) -> Void = { _ in }) {
        self.state = state
        self.onAction = onAction
    }

    private var waiting: Bool { !state.visibleActions.contains("republish") }
    private var summary: [String] { state.itemDetails }
    private func row(_ index: Int) -> String { summary.indices.contains(index) ? summary[index] : "" }

    public var body: some View {
        CustomerScreen(title: bzrString("waiting.title"), state: state) {
            VStack(spacing: DesignSpace.s) {
                ZStack {
                    Circle().fill(DesignColors.successTint)
                    Circle().fill(DesignColors.success).frame(width: DesignSize.successCore, height: DesignSize.successCore)
                    BzrIcon(.check, tint: DesignColors.onPrimary)
                }
                .frame(width: DesignSize.successRing, height: DesignSize.successRing)
                BzrText(bzrString(waiting ? "waiting.heading" : "offers.empty.title"), style: DesignType.screenTitle, alignment: .center)
                BzrText(bzrString(waiting ? "waiting.body" : "offers.empty.body"), style: DesignType.subtitle, alignment: .center)
            }
            .frame(maxWidth: .infinity)
            orderCard
            if !state.waitingInfo.isEmpty { InfoBanner(text: state.waitingInfo) }
            InfoBanner(text: bzrString("waiting.compare"))
            VStack(spacing: DesignSpace.s) {
                let republish = state.visibleActions.contains("republish")
                PrimaryButton(
                    text: bzrString(republish ? "action.republish" : "waiting.follow"),
                    state: state.isBusy ? .loading : .normal, icon: .arrowForward,
                    onClick: { onAction(republish ? "republish" : "follow") })
                if state.visibleActions.contains("edit_request") {
                    SecondaryButton(text: bzrString("action.edit_request"), onClick: { onAction("edit_request") })
                }
                if state.visibleActions.contains("cancel") {
                    DangerTextButton(text: bzrString("action.cancel"), onClick: { onAction("cancel") })
                }
            }
        }
    }

    private var orderCard: some View {
        VStack(alignment: .leading, spacing: DesignSpace.m) {
            HStack(spacing: DesignSpace.m) {
                CategoryGlyph(state.optionIcons.first, size: DesignSize.icon)
                    .frame(width: DesignSize.optionWell, height: DesignSize.optionWell)
                    .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.surfaceContainer))
                VStack(alignment: .leading, spacing: 0) {
                    BzrText(bzrString(state.showEta ? "waiting.kind.now" : "waiting.kind.scheduled"), style: DesignType.hint)
                    BzrText(row(0), style: DesignType.homeSection)
                }
                Spacer(minLength: 0)
                HStack(spacing: DesignSpace.xs) {
                    Circle().fill(DesignColors.amber500).frame(width: DesignSize.statusDot, height: DesignSize.statusDot)
                    BzrText(bzrString("waiting.status"), style: DesignType.hint)
                }
                .padding(.horizontal, DesignSpace.s)
                .background(Capsule().fill(DesignColors.surfaceAlt2))
                .frame(maxHeight: .infinity, alignment: .top)
            }
            .fixedSize(horizontal: false, vertical: true)
            VStack(alignment: .leading, spacing: DesignSpace.s) {
                if !row(1).isEmpty { info(.location, row(1)) }
                let time = state.showEta ? bzrString("provider.home.timing.now") : row(2)
                if !time.isEmpty { info(.clock, time) }
                if !row(3).isEmpty {
                    info(.payments, BzrFormatter.fill(bzrString("waiting.budget"), values: [("amount", row(3))]))
                }
            }
        }
        .padding(DesignSpace.l)
        .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
        .bzrShadow(DesignShadows.card)
    }

    private func info(_ icon: BzrIconKey, _ text: String) -> some View {
        HStack(spacing: DesignSpace.xs) {
            BzrIcon(icon, tint: DesignColors.primary600, size: DesignSize.iconSmall)
            BzrText(text, style: DesignType.caption.colored(DesignColors.navy900))
        }
        .padding(.horizontal, DesignSpace.m)
        .padding(.vertical, DesignSpace.xs)
        .background(Capsule().fill(DesignColors.surfaceAlt2))
    }
}
