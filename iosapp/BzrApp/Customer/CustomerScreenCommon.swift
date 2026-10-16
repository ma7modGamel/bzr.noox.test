import BzrCore
import DesignSystem
import SwiftUI

/// Every customer screen's back goes through the shared history unless a screen passes its own (6ب).
private struct CustomerBackKey: EnvironmentKey {
    static let defaultValue: () -> Void = {}
}

private struct CustomerRefreshKey: EnvironmentKey {
    static let defaultValue: (() async -> Void)? = nil
}

private struct CustomerRetryKey: EnvironmentKey {
    static let defaultValue: (() -> Void)? = nil
}

extension EnvironmentValues {
    var customerRefresh: (() async -> Void)? {
        get { self[CustomerRefreshKey.self] }
        set { self[CustomerRefreshKey.self] = newValue }
    }
    var customerBack: () -> Void {
        get { self[CustomerBackKey.self] }
        set { self[CustomerBackKey.self] = newValue }
    }

    var customerRetry: (() -> Void)? {
        get { self[CustomerRetryKey.self] }
        set { self[CustomerRetryKey.self] = newValue }
    }
}

struct CustomerScreen<Content: View>: View {
    let title: String
    let state: CustomerUIState
    let onBack: (() -> Void)?
    let content: Content
    let bottomBar: AnyView?
    /// DEC-073: a screen-owned header that stays above the scrolling body (Figma home header).
    let header: AnyView?
    /// C01 draws its own header instead of the top bar (DEC-063).
    let showsTopBar: Bool
    /// DEC-076: an optional top-bar action (Figma's bell on «الرسائل»).
    let topAction: (icon: BzrIconKey, label: String, action: () -> Void)?
    @Environment(\.customerRefresh) private var customerRefresh
    @Environment(\.customerBack) private var customerBack
    @Environment(\.customerRetry) private var customerRetry

    init(
        title: String, state: CustomerUIState, onBack: (() -> Void)? = nil, bottomBar: AnyView? = nil,
        showsTopBar: Bool = true, header: AnyView? = nil,
        topAction: (icon: BzrIconKey, label: String, action: () -> Void)? = nil, @ViewBuilder content: () -> Content
    ) {
        self.topAction = topAction
        self.header = header
        self.showsTopBar = showsTopBar
        self.title = title
        self.state = state
        self.onBack = onBack
        self.bottomBar = bottomBar
        self.content = content()
    }

    var body: some View {
        VStack(spacing: 0) {
            if showsTopBar {
                AppTopBar(
                    title: title, actionIcon: topAction?.icon, actionLabel: topAction?.label,
                    showsBack: !["SCR-C01", "SCR-C25", "SCR-C18", "SCR-C02"].contains(state.screen),
                    onBack: onBack ?? customerBack, onAction: topAction?.action ?? {})
            }
            if let header { header }
            if state.isRefreshing { ProgressView(bzrString("refresh.loading")) }
            ScrollView {
                VStack(alignment: .leading, spacing: DesignSpace.l) {
                    if let key = state.refreshMessageKey { InfoBanner(text: bzrString(key), isError: ["refresh.failed", "notifications.read_failed"].contains(key)) }
                    switch state.phase {
                    case .loading:
                        BremoBrandLoader()
                        ForEach(0..<2, id: \.self) { _ in LoadingSkeleton().accessibilityHidden(true) }
                    case .error:
                        ErrorState(
                            title: bzrString("error.title"), body: bzrString("error.body"),
                            retry: bzrString(customerRetry == nil ? "common.cancel" : "common.retry"),
                            onRetry: customerRetry ?? onBack ?? customerBack)
                    default:
                        content
                    }
                }
                .frame(maxWidth: DesignSize.contentMaxWidth, alignment: .leading)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, DesignSpace.screenHorizontal)
                .padding(.top, DesignSpace.screenVertical)
                .padding(.bottom, DesignSpace.contentBottom)
            }
            .modifier(OptionalPullRefresh(action: customerRefresh))
            .scrollDismissesKeyboard(.interactively)
        }
        .background(DesignGradients.screen.ignoresSafeArea())
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if state.phase != .loading && state.phase != .error, let bottomBar {
                // DEC-070: Figma's bottom dock — tinted, lifted by a soft shadow.
                bottomBar
                    .padding(.horizontal, DesignSpace.screenHorizontal)
                    .padding(.vertical, DesignSpace.m)
                    .background(DesignColors.surfaceAlt.bzrShadow(DesignShadows.card))
            }
        }
    }
}

/// DEC-071: the request flow's five-step header (Figma «Primo App»).
struct RequestProgress: View {
    let current: Int
    var total: Int = 5
    var hint: String = ""

    var body: some View {
        StepIndicator(
            current: current, total: total,
            label: BzrFormatter.fill(
                bzrString("format.step"), values: [("current", BzrFormat.number(current)), ("total", BzrFormat.number(total))]),
            hint: hint)
    }
}

struct ScreenHeading: View {
    let title: String
    let detail: String?

    init(_ title: String, detail: String? = nil) {
        self.title = title
        self.detail = detail
    }

    var body: some View {
        VStack(alignment: .leading, spacing: DesignSpace.s) {
            BzrText(title, style: DesignType.sectionTitle).accessibilityAddTraits(.isHeader)
            if let detail { BzrText(detail, style: DesignType.secondary) }
        }
        .padding(.top, DesignSpace.s)
    }
}

struct ActionButtons: View {
    let actions: [String]
    let onAction: (String) -> Void

    init(actions: [String], onAction: @escaping (String) -> Void = { _ in }) {
        self.actions = actions
        self.onAction = onAction
    }

    var body: some View {
        ForEach(actions, id: \.self) { action in
            if [
                "accept_offer", "republish", "approve_proposal", "pay_electronic", "confirm_completion",
                "rate",
            ].contains(action) {
                PrimaryButton(text: actionText(action), onClick: { onAction(action) })
            } else if ["cancel", "open_dispute", "report_provider"].contains(action) {
                DangerTextButton(text: actionText(action), onClick: { onAction(action) })
            } else {
                SecondaryButton(text: actionText(action), onClick: { onAction(action) })
            }
        }
    }

    private func actionText(_ action: String) -> String {
        bzrString(
            [
                "request_warranty": "action.request_warranty",
                "accept_offer": "action.accept_offer", "edit_request": "action.edit_request",
                "republish": "action.republish", "cancel": "action.cancel", "call": "action.call",
                "chat": "action.chat", "share_visit": "action.share_visit",
                "open_dispute": "action.open_dispute", "report_provider": "action.report_provider",
                "approve_proposal": "action.approve_proposal", "reject_proposal": "action.reject_proposal",
                "change_payment_method": "action.change_payment_method",
                "pay_electronic": "action.pay_electronic",
                "confirm_completion": "action.confirm_completion", "rate": "action.rate",
            ][action] ?? "common.close")
    }
}

/// Applying refreshable conditionally keeps pull gestures out of editable forms.
struct OptionalPullRefresh: ViewModifier {
    let action: (() async -> Void)?
    @ViewBuilder func body(content: Content) -> some View {
        if let action { content.refreshable { await action() }.accessibilityHint(bzrString("refresh.pull")) } else { content }
    }
}
