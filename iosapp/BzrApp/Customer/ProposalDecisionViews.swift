import BzrCore
import DesignSystem
import SwiftUI

/// SCR-C30 execution quote — Figma «نتيجة الفحص» 3002:2264 — and SCR-C31 additional cost — Figma 3002:4029 (DEC-076).
/// `items`: type, amount, reason, projected total; `fieldValues`: provider name, rating, category, agreed price.
/// The optional image is the current fault, loaded through an authenticated request.
struct ProposalDecisionView: View {
    var loadImage: (String) async -> Data? = { _ in nil }
    let state: CustomerUIState
    let screen: String
    let onAction: (String) -> Void

    private func item(_ index: Int) -> String { state.items.indices.contains(index) ? state.items[index] : "" }
    private func value(_ index: Int) -> String { state.fieldValues.indices.contains(index) ? state.fieldValues[index] : "" }
    private func amount(_ text: String) -> String {
        BzrFormatter.fill(bzrString("customer.home.offers.amount"), values: [("amount", text)])
    }
    private var isQuote: Bool { screen == "SCR-C30" }

    private var remaining: Int { CustomerLogic.proposalRemaining(state, uptimeSeconds: Int(ProcessInfo.processInfo.systemUptime)) }
    private var mayDecide: Bool { state.canContinue && (state.proposalDeadline.isEmpty || remaining > 0) }

    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { _ in
            CustomerScreen(
                title: bzrString(isQuote ? "proposal.execution.title" : "proposal.additional.title"), state: state,
                bottomBar: AnyView(dock)
            ) {
                if !state.proposalDeadline.isEmpty {
                    card {
                        BzrText(bzrString("proposal.deadline") + ": " + state.proposalDeadline, style: DesignType.caption)
                        BzrText(bzrString("proposal.remaining") + ": \(remaining / 60):\(String(format: "%02d", remaining % 60))", style: DesignType.detailStrong)
                        if !state.proposalExpiryMessage.isEmpty { BzrText(state.proposalExpiryMessage, style: DesignType.caption) }
                    }
                }
                if isQuote { quote } else { additional }
                if !state.proposalDeadline.isEmpty && remaining == 0 {
                    banner(.warning, bzrString("proposal.expired"), fill: DesignColors.warningBg)
                } else if let key = state.messageKey {
                    banner(.warning, bzrString(key == "proposal.expired" ? "proposal.expired" : "proposal.deciding"), fill: DesignColors.warningBg)
                }
            }
        }
    }

    @ViewBuilder private var quote: some View {
        banner(.check, bzrString("proposal.inspection_done"), fill: DesignColors.statusPillBg)
        providerCard
        card {
            heading(bzrString("proposal.diagnosis"), icon: .search, badge: bzrString("proposal.field"))
            if !item(2).isEmpty { BzrText(item(2), style: DesignType.secondary) }
            if !state.proposalPhotoURL.isEmpty { ServicePhotoSection(title: bzrString("proposal.fault_photo"), urls: [state.proposalPhotoURL], loadImage: loadImage) }
        }
        if !value(3).isEmpty {
            VStack(alignment: .leading, spacing: DesignSpace.m) {
                HStack {
                    BzrText(bzrString("proposal.agreement"), style: DesignType.caption)
                    Spacer(minLength: DesignSpace.s)
                    BzrText(bzrString("proposal.confirmed"), style: DesignType.statusPill)
                }
                HStack {
                    BzrText(bzrString("proposal.agreed_price"), style: DesignType.tileTitle)
                    Spacer(minLength: DesignSpace.s)
                    BzrText(amount(value(3)), style: DesignType.offerPrice.colored(DesignColors.navy900))
                }
            }
            .padding(DesignSpace.l)
            .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surfaceAlt2))
        }
        card {
            HStack {
                BzrText(item(0), style: DesignType.caption)
                Spacer(minLength: DesignSpace.s)
                pendingPill
            }
            HStack {
                BzrText(item(2), style: DesignType.tileTitle)
                Spacer(minLength: DesignSpace.s)
                BzrText(amount(item(1)), style: DesignType.statValue)
                    .padding(.horizontal, DesignSpace.m)
                    .padding(.vertical, DesignSpace.xs)
                    .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.surfaceAlt2))
            }
            banner(.info, bzrString("proposal.not_added"), fill: DesignColors.surfaceAlt2)
        }
        HStack(spacing: DesignSpace.s) {
            if state.visibleActions.contains("chat") {
                SecondaryButton(
                    text: BzrFormatter.fill(bzrString("proposal.message"), values: [("name", value(0).components(separatedBy: " ").first ?? "")]),
                    icon: .messages, onClick: { onAction("chat") })
            }
            if state.visibleActions.contains("open_dispute") {
                SecondaryButton(text: bzrString("tracking.report"), icon: .warning, onClick: { onAction("open_dispute") })
            }
        }
    }

    @ViewBuilder private var additional: some View {
        HStack(spacing: DesignSpace.s) {
            BzrIcon(.clock, tint: DesignColors.amber500, size: DesignSize.iconSmall)
            BzrText(bzrString("proposal.status"), style: DesignType.tileTitle)
            Spacer(minLength: DesignSpace.s)
            pendingPill
        }
        .padding(DesignSpace.m)
        .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.warningBg))
        providerCard
        if !item(2).isEmpty { banner(.info, item(2), fill: DesignColors.surfaceAlt2) }
        card {
            heading(bzrString("proposal.cost_details"), icon: .receipt, badge: nil)
            if !value(3).isEmpty { line(bzrString("proposal.current_total"), amount(value(3)), style: DesignType.statValue) }
            HStack(spacing: DesignSpace.s) {
                BzrIcon(.wrench, tint: DesignColors.primary600, size: DesignSize.iconSmall)
                BzrText(item(0), style: DesignType.caption)
                Spacer(minLength: DesignSpace.s)
                BzrText(amount(item(1)), style: DesignType.statValue.colored(DesignColors.primary600))
            }
            .padding(.horizontal, DesignSpace.m)
            .padding(.vertical, DesignSpace.s)
            .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.surfaceAlt2))
            Rectangle().fill(DesignColors.border).frame(height: DesignBorder.width)
            line(bzrString("proposal.new_total"), amount(item(3)), style: DesignType.amountDisplay.colored(DesignColors.primary600))
        }
        if !state.proposalPhotoURL.isEmpty {
            card {
                heading(bzrString("proposal.photo"), icon: .image, badge: nil)
                ServicePhotoSection(title: bzrString("proposal.fault_photo"), urls: [state.proposalPhotoURL], loadImage: loadImage)
                BzrText(bzrString("proposal.no_charge"), style: DesignType.hint, alignment: .center)
            }
        }
    }

    @ViewBuilder private var dock: some View {
        VStack(spacing: DesignSpace.s) {
            if isQuote && !item(3).isEmpty {
                line(bzrString("proposal.total_after"), amount(item(3)), style: DesignType.offerPrice.colored(DesignColors.navy900))
            }
            if state.visibleActions.contains("approve_proposal") {
                PrimaryButton(
                    text: BzrFormatter.fill(bzrString("proposal.approve"), values: [("amount", item(3))]),
                    state: state.isBusy ? .loading : (mayDecide ? .normal : .disabled), icon: .check,
                    onClick: { if mayDecide { onAction("approve_proposal") } })
            }
            if !isQuote && state.visibleActions.contains("chat") {
                SecondaryButton(text: bzrString("proposal.ask"), icon: .messages, onClick: { onAction("chat") })
            }
            if state.visibleActions.contains("reject_proposal") {
                DangerTextButton(text: bzrString("proposal.reject"), onClick: { if mayDecide { onAction("reject_proposal") } }).disabled(!mayDecide)
            }
        }
    }

    private var providerCard: some View {
        card {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 0) {
                    BzrText(value(0), style: DesignType.offerName)
                    if !value(2).isEmpty {
                        BzrText(
                            state.providerVerified
                                ? BzrFormatter.fill(bzrString("provider.verified_title"), values: [("category", value(2))]) : value(2),
                            style: DesignType.hint)
                    }
                }
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
        }
    }

    private var pendingPill: some View {
        BzrText(bzrString("proposal.pending"), style: DesignType.statusPill.colored(DesignColors.warningText))
            .padding(.horizontal, DesignSpace.s)
            .background(Capsule().fill(DesignColors.badgeBg))
    }

    private func heading(_ title: String, icon: BzrIconKey, badge: String?) -> some View {
        HStack(spacing: DesignSpace.s) {
            BzrIcon(icon, tint: DesignColors.primary600, size: DesignSize.iconSmall)
            BzrText(title, style: DesignType.homeSection)
            Spacer(minLength: DesignSpace.s)
            if let badge {
                BzrText(badge, style: DesignType.statusPill)
                    .padding(.horizontal, DesignSpace.s)
                    .background(Capsule().fill(DesignColors.surfaceContainer))
            }
        }
    }

    private func banner(_ icon: BzrIconKey, _ text: String, fill: Color) -> some View {
        HStack(spacing: DesignSpace.s) {
            BzrIcon(icon, tint: DesignColors.primary600, size: DesignSize.iconSmall)
            BzrText(text, style: DesignType.caption)
            Spacer(minLength: 0)
        }
        .padding(.horizontal, DesignSpace.m)
        .padding(.vertical, DesignSpace.s)
        .background(RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(fill))
    }

    private func line(_ label: String, _ text: String, style: BzrTextStyle) -> some View {
        HStack {
            BzrText(label, style: DesignType.caption)
            Spacer(minLength: DesignSpace.s)
            BzrText(text, style: style)
        }
    }

    private func card<Content: View>(@ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: DesignSpace.m) { content() }
            .padding(DesignSpace.l)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
            .bzrShadow(DesignShadows.card)
    }
}

/// SCR-C30.
public struct C30ExecutionQuoteView: View {
    let loadImage: (String) async -> Data?
    let state: CustomerUIState
    let onAction: (String) -> Void

    public init(state: CustomerUIState, loadImage: @escaping (String) async -> Data? = { _ in nil }, onAction: @escaping (String) -> Void = { _ in }) {
        self.loadImage = loadImage
        self.state = state
        self.onAction = onAction
    }

    public var body: some View { ProposalDecisionView(loadImage: loadImage, state: state, screen: "SCR-C30", onAction: onAction) }
}

/// SCR-C31.
public struct C31AdditionalCostView: View {
    let loadImage: (String) async -> Data?
    let state: CustomerUIState
    let onAction: (String) -> Void

    public init(state: CustomerUIState, loadImage: @escaping (String) async -> Data? = { _ in nil }, onAction: @escaping (String) -> Void = { _ in }) {
        self.loadImage = loadImage
        self.state = state
        self.onAction = onAction
    }

    public var body: some View { ProposalDecisionView(loadImage: loadImage, state: state, screen: "SCR-C31", onAction: onAction) }
}
