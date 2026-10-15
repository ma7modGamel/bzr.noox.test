import BzrCore
import DesignSystem
import SwiftUI

/// SCR-C05, step 5 of 5 (DEC-071): one card per part of the draft, each with «تعديل» back to its step, then terms and
/// publish. Rows come from the draft (DEC-059): `items` = service, description…; `itemDetails` = address, time,
/// materials; `itemStates` = pricing rows (marketplace only).
public struct C05ReviewView: View {
    let state: CustomerUIState
    let onEdit: (String) -> Void
    let onTermsChange: () -> Void
    let onPublish: () -> Void

    public init(
        state: CustomerUIState, onEdit: @escaping (String) -> Void = { _ in }, onTermsChange: @escaping () -> Void = {},
        onPublish: @escaping () -> Void = {}
    ) {
        self.state = state
        self.onEdit = onEdit
        self.onTermsChange = onTermsChange
        self.onPublish = onPublish
    }

    public var body: some View {
        CustomerScreen(title: bzrString("request.review.title"), state: state, bottomBar: AnyView(primaryAction)) {
            RequestProgress(current: state.requestSteps, total: state.requestSteps, hint: bzrString("request.review.final"))
            let service = (state.items.first ?? "").components(separatedBy: " — ")
            card(.wrench, service.first ?? "", service.count > 1 ? service[1] : "", "", "SCR-C03")
            if state.items.count > 1 {
                card(.orders, bzrString("request.review.details"), state.items.dropFirst().joined(separator: "\n"), "", "SCR-C37")
            }
            if let address = state.itemDetails.first {
                let parts = address.components(separatedBy: " · ")
                card(.location, parts.first ?? address, parts.dropFirst().joined(separator: " · "), "", "SCR-C04")
            }
            if state.itemDetails.count > 1 {
                card(.bolt, bzrString("request.review.visit"), state.itemDetails[1], "", "SCR-C04")
            }
            let materials = state.itemDetails.count > 2 ? state.itemDetails[2] : ""
            if !state.itemStates.isEmpty || !materials.isEmpty {
                card(
                    .payments,
                    state.itemStates.count > 1 ? state.itemStates[state.itemStates.count - 1] : bzrString("request.budget.title"),
                    state.itemStates.first ?? "", materials, state.requestSteps == 4 ? "SCR-C04" : "SCR-C38")
            }
            if !state.showPricing { InfoBanner(text: bzrString("request.staff.pricing")) }
            InfoBanner(text: bzrString("request.review.notice"))
            Button(action: onTermsChange) {
                CheckRow(text: bzrString("request.review.terms"), checked: state.termsError == nil)
            }
            .buttonStyle(BzrPressStyle())
            if state.termsError != nil { InfoBanner(text: bzrString("request.terms.required")) }
            // A refused publish shows the server's reason here (SCR-C05 §الأخطاء).
            if state.messageKey == "request.publish.error" {
                WarningBox(text: (state.fieldValues.first ?? "").isEmpty ? bzrString("error.body") : state.fieldValues[0])
            }
        }
    }

    private func card(_ icon: BzrIconKey, _ title: String, _ detail: String, _ extra: String, _ step: String) -> some View {
        HStack(spacing: DesignSpace.m) {
            ZStack {
                RoundedRectangle(cornerRadius: DesignRadius.menuIconBox).fill(DesignColors.surfaceContainer)
                BzrIcon(icon, tint: DesignColors.primary600, size: DesignSize.icon)
            }
            .frame(width: DesignSize.reviewWell, height: DesignSize.reviewWell)
            VStack(alignment: .leading, spacing: DesignSpace.xs) {
                BzrText(title, style: DesignType.cardTitle)
                if !detail.isEmpty { BzrText(detail, style: DesignType.secondary) }
                if !extra.isEmpty { BzrText(extra, style: DesignType.hint) }
            }
            Spacer(minLength: 0)
            Button(action: { onEdit(step) }) {
                BzrText(bzrString("common.edit"), style: DesignType.linkSmall)
                    .padding(.horizontal, DesignSpace.m)
                    .frame(minHeight: DesignSize.sortChipHeight)
                    .background(Capsule().fill(DesignColors.surfaceContainer))
            }
            .buttonStyle(BzrPressStyle())
            .accessibilityLabel("\(bzrString("common.edit")) \(title)")
        }
        .padding(DesignSpace.l)
        .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
        .bzrShadow(DesignShadows.card)
    }

    private var primaryAction: some View {
        VStack(spacing: DesignSpace.s) {
            PrimaryButton(
                text: bzrString("request.publish"),
                state: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled), icon: .send,
                onClick: onPublish)
            HStack(spacing: DesignSpace.xs) {
                BzrIcon(.shield, tint: DesignColors.slate400, size: DesignSize.iconSmall)
                BzrText(bzrString("request.review.secure"), style: DesignType.hint)
            }
        }
    }
}
