import BzrCore
import DesignSystem
import SwiftUI

/// Every customer screen's back goes through the shared history unless a screen passes its own (6ب).
private struct CustomerBackKey: EnvironmentKey {
    static let defaultValue: () -> Void = {}
}

private struct CustomerRetryKey: EnvironmentKey {
    static let defaultValue: (() -> Void)? = nil
}

extension EnvironmentValues {
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
    /// C01 draws its own header instead of the top bar (DEC-063).
    let showsTopBar: Bool
    @Environment(\.customerBack) private var customerBack
    @Environment(\.customerRetry) private var customerRetry

    init(
        title: String, state: CustomerUIState, onBack: (() -> Void)? = nil, bottomBar: AnyView? = nil,
        showsTopBar: Bool = true, @ViewBuilder content: () -> Content
    ) {
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
                    title: title, showsBack: !["SCR-C01", "SCR-C25", "SCR-C18", "SCR-C02"].contains(state.screen),
                    onBack: onBack ?? customerBack)
            }
            ScrollView {
                VStack(alignment: .leading, spacing: DesignSpace.l) {
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
            .scrollDismissesKeyboard(.interactively)
        }
        .background(DesignGradients.screen.ignoresSafeArea())
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if state.phase != .loading && state.phase != .error, let bottomBar {
                bottomBar
                    .padding(.horizontal, DesignSpace.screenHorizontal)
                    .padding(.vertical, DesignSpace.s)
                    .background(DesignColors.surface)
            }
        }
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
