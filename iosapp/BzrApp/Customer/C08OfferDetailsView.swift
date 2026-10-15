import BzrCore
import DesignSystem
import SwiftUI

/// SCR-C08 — Figma «Primo App» «تأكيد اختيار الفني» 3002:1757 (DEC-073): technician card, offer details (price, what it
/// includes, materials, assurance), request summary, payment method, then «تأكيد اختيار …» and «الرجوع للعروض».
/// `fieldValues`: name, rating, completed, eta, category, materials; `items`: amount, eta or slot, includes;
/// `itemDetails`: problem, address, slot (blank for «الآن»).
public struct C08OfferDetailsView: View {
    let state: CustomerUIState
    let onAction: (String) -> Void
    let onPaymentSelect: (Int) -> Void
    @Environment(\.customerBack) private var customerBack

    public init(
        state: CustomerUIState, onAction: @escaping (String) -> Void = { _ in },
        onPaymentSelect: @escaping (Int) -> Void = { _ in }
    ) {
        self.state = state
        self.onAction = onAction
        self.onPaymentSelect = onPaymentSelect
    }

    private func value(_ index: Int) -> String { state.fieldValues.indices.contains(index) ? state.fieldValues[index] : "" }
    private func item(_ index: Int) -> String { state.items.indices.contains(index) ? state.items[index] : "" }
    private func detail(_ index: Int) -> String { state.itemDetails.indices.contains(index) ? state.itemDetails[index] : "" }
    private var eta: String {
        Int(value(3)).map { bzrPlural("offer.eta", count: $0) } ?? ""
    }

    public var body: some View {
        CustomerScreen(title: bzrString("confirm.title"), state: state, bottomBar: AnyView(dock)) {
            providerCard
            heading(bzrString("confirm.offer_details"), badge: bzrString("confirm.fixed"))
            offerCard
            HStack(spacing: DesignSpace.xs) {
                BzrIcon(.shield, tint: DesignColors.primary600, size: DesignSize.iconSmall)
                BzrText(bzrString("confirm.assurance"), style: DesignType.hint)
            }
            heading(bzrString("confirm.summary"))
            summaryCard
            if !state.options.isEmpty {
                heading(bzrString("confirm.payment"))
                ForEach(Array(state.options.enumerated()), id: \.offset) { index, label in
                    Button(action: { onPaymentSelect(index) }) {
                        RadioCard(title: label, body: "", selected: index == state.selectedOptionIndex, icon: .payments)
                    }
                    .buttonStyle(BzrPressStyle())
                }
            }
        }
    }

    private func card<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: DesignSpace.s) { content() }
            .padding(DesignSpace.l)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
            .bzrShadow(DesignShadows.card)
    }

    private func heading(_ title: String, badge: String? = nil) -> some View {
        HStack {
            BzrText(title, style: DesignType.homeSection)
            Spacer(minLength: DesignSpace.s)
            if let badge {
                BzrText(badge, style: DesignType.statusPill)
                    .padding(.horizontal, DesignSpace.s)
                    .background(Capsule().fill(DesignColors.surfaceContainer))
            }
        }
    }

    private var providerCard: some View {
        card {
            HStack {
                BzrText(value(0), style: DesignType.offerName)
                Spacer(minLength: DesignSpace.s)
                if !value(1).isEmpty {
                    HStack(spacing: DesignSpace.xs) {
                        BzrIcon(.starFilled, tint: DesignColors.star, size: DesignSize.iconSmall)
                        BzrText(value(1), style: DesignType.detailStrong)
                    }
                    .padding(.horizontal, DesignSpace.s)
                    .background(Capsule().fill(DesignColors.surfaceAlt2))
                }
            }
            let meta = [
                state.providerVerified && !value(4).isEmpty
                    ? BzrFormatter.fill(bzrString("provider.verified_title"), values: [("category", value(4))]) : "",
                value(2).isEmpty
                    ? ""
                    : bzrPlural("confirm.completed", count: Int(value(2)) ?? 0),
            ].filter { !$0.isEmpty }.joined(separator: " • ")
            if !meta.isEmpty { BzrText(meta, style: DesignType.hint.colored(DesignColors.primary600)) }
            if state.showEta && !eta.isEmpty {
                HStack(spacing: DesignSpace.xs) {
                    BzrIcon(.bolt, tint: DesignColors.primary600, size: DesignSize.iconSmall)
                    BzrText(eta, style: DesignType.linkSmall)
                }
            }
        }
    }

    private var offerCard: some View {
        card {
            HStack {
                BzrText(bzrString("confirm.offer_price"), style: DesignType.caption)
                Spacer(minLength: DesignSpace.s)
                BzrText(
                    BzrFormatter.fill(bzrString("customer.home.offers.amount"), values: [("amount", item(0))]),
                    style: DesignType.amountDisplay.colored(DesignColors.primary600))
            }
            let includes = item(2) == "deductible" ? bzrString("offer.inspection.deductible") : item(2)
            if !includes.isEmpty { check(.check, includes) }
            if !value(5).isEmpty { check(.info, value(5)) }
        }
    }

    private var summaryCard: some View {
        // STATIC (DEC-074, BACKEND-NOTES B-15) only when the offer has no ETA: «الآن» then the offer's own window.
        let time =
            detail(2).isEmpty
            ? [bzrString("provider.home.timing.now"), eta].filter { !$0.isEmpty }.joined(separator: " — ") : detail(2)
        return card {
            ForEach(
                [
                    (BzrIconKey.wrench, "confirm.summary.problem", detail(0)), (.location, "confirm.summary.address", detail(1)),
                    (.clock, "confirm.summary.time", time),
                ].filter { !$0.2.isEmpty }, id: \.1
            ) { icon, label, text in
                HStack(spacing: DesignSpace.m) {
                    BzrIcon(icon, tint: DesignColors.primary600, size: DesignSize.iconSmall)
                        .frame(width: DesignSize.orderWell, height: DesignSize.orderWell)
                        .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.surfaceContainer))
                    VStack(alignment: .leading, spacing: 0) {
                        BzrText(bzrString(label), style: DesignType.hint)
                        BzrText(text, style: DesignType.tileTitle)
                    }
                    Spacer(minLength: 0)
                }
            }
        }
    }

    private func check(_ icon: BzrIconKey, _ text: String) -> some View {
        HStack(spacing: DesignSpace.s) {
            BzrIcon(icon, tint: DesignColors.primary600, size: DesignSize.iconSmall)
            BzrText(text, style: DesignType.secondary)
        }
    }

    private var dock: some View {
        VStack(spacing: DesignSpace.xs) {
            if state.visibleActions.contains("accept_offer") {
                PrimaryButton(
                    text: BzrFormatter.fill(bzrString("confirm.action"), values: [("name", value(0).components(separatedBy: " ").first ?? "")]),
                    state: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled), icon: .arrowForward,
                    onClick: { onAction("accept_offer") })
            }
            Button(action: customerBack) {
                BzrText(bzrString("confirm.back"), style: DesignType.caption, alignment: .center)
                    .frame(maxWidth: .infinity, minHeight: DesignSize.touchTargetMin)
            }
            .buttonStyle(BzrPressStyle())
        }
    }
}
