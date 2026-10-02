import BzrCore
import DesignSystem
import SwiftUI

/// Every customer screen's back goes through the shared history unless a screen passes its own (6ب).
private struct CustomerBackKey: EnvironmentKey {
    static let defaultValue: () -> Void = {}
}

extension EnvironmentValues {
    var customerBack: () -> Void {
        get { self[CustomerBackKey.self] }
        set { self[CustomerBackKey.self] = newValue }
    }
}

struct CustomerScreen<Content: View>: View {
    let title: String
    let state: CustomerUIState
    let onBack: (() -> Void)?
    let content: Content
    @Environment(\.customerBack) private var customerBack

    init(
        title: String, state: CustomerUIState, onBack: (() -> Void)? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.state = state
        self.onBack = onBack
        self.content = content()
    }

    var body: some View {
        VStack(spacing: 0) {
            AppTopBar(title: title, onBack: onBack ?? customerBack)
            ScrollView {
                VStack(alignment: .leading, spacing: DesignSpace.m) {
                    switch state.phase {
                    case .loading:
                        ForEach(0..<3, id: \.self) { _ in LoadingSkeleton() }
                    case .error:
                        ErrorState(
                            title: bzrString("error.title"), body: bzrString("error.body"),
                            retry: bzrString("common.retry"))
                    default:
                        content
                    }
                }
                .padding(.horizontal, DesignSpace.screenHorizontal)
                .padding(.vertical, DesignSpace.m)
            }
        }
        .background(DesignColors.surface)
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
        BzrText(title, style: DesignType.sectionTitle)
        if let detail { BzrText(detail, style: DesignType.secondary) }
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
