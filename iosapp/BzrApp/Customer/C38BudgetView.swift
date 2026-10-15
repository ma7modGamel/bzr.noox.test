import BzrCore
import DesignSystem
import SwiftUI

/// SCR-C38, step 4 of 5 (DEC-071): materials; in marketplace the pricing mode and an optional budget (07, BR-015).
public struct C38BudgetView: View {
    let state: CustomerUIState
    let onMaterialSelect: (Int) -> Void
    let onPricingSelect: (Int) -> Void
    let onBudgetChange: (String) -> Void
    let onOpen: (String) -> Void

    public init(
        state: CustomerUIState, onMaterialSelect: @escaping (Int) -> Void = { _ in },
        onPricingSelect: @escaping (Int) -> Void = { _ in }, onBudgetChange: @escaping (String) -> Void = { _ in },
        onOpen: @escaping (String) -> Void = { _ in }
    ) {
        self.state = state
        self.onMaterialSelect = onMaterialSelect
        self.onPricingSelect = onPricingSelect
        self.onBudgetChange = onBudgetChange
        self.onOpen = onOpen
    }

    public var body: some View {
        CustomerScreen(title: bzrString("request.budget.title"), state: state, bottomBar: AnyView(primaryAction)) {
            RequestProgress(current: 4, total: state.requestSteps)
            if state.showPricing {
                heading(.payments, bzrString("request.budget.section"))
                BzrText(bzrString("request.budget.body"), style: DesignType.secondary)
                budgetCard
                ForEach(Array(state.pricingOptions.enumerated()), id: \.offset) { index, label in
                    let code = state.pricingCodes.indices.contains(index) ? state.pricingCodes[index] : ""
                    Button(action: { onPricingSelect(index) }) {
                        RadioCard(
                            title: label, body: pricingBody(code),
                            selected: index == state.selectedPricingIndex, icon: code == "INSPECTION" ? .search : .payments)
                    }
                    .buttonStyle(BzrPressStyle())
                }
            } else {
                InfoBanner(text: bzrString("request.staff.pricing"))
            }
            MaterialOptions(state: state, onSelect: onMaterialSelect)
        }
    }

    private var budgetCard: some View {
        VStack(alignment: .leading, spacing: DesignSpace.s) {
            HStack {
                BzrText(bzrString("request.budget.amount"), style: DesignType.caption)
                Spacer(minLength: DesignSpace.s)
                if !state.itemDetails.isEmpty { Badge(text: bzrString("request.budget.area_average"), kind: .status) }
            }
            HStack(spacing: DesignSpace.s) {
                Spacer(minLength: 0)
                TextField(
                    "",
                    text: Binding(
                        get: { state.fieldValues.indices.contains(1) ? state.fieldValues[1] : "" },
                        set: onBudgetChange),
                    prompt: Text(bzrString("request.budget.placeholder")).foregroundColor(DesignColors.slate400)
                )
                .keyboardType(.decimalPad)
                .multilineTextAlignment(.center)
                .font(.custom(DesignType.amountDisplay.fontName, size: DesignType.amountDisplay.size, relativeTo: .largeTitle))
                .foregroundStyle(DesignType.amountDisplay.color)
                .fixedSize()
                BzrText(bzrString("common.currency.short"), style: DesignType.sectionTitle.colored(DesignColors.slate500))
                Spacer(minLength: 0)
            }
            Rectangle().fill(DesignColors.border).frame(height: DesignBorder.width)
            HStack(spacing: DesignSpace.s) {
                ForEach(state.itemDetails, id: \.self) { amount in
                    let budget = state.fieldValues.indices.contains(1) ? state.fieldValues[1] : ""
                    Button(action: { onBudgetChange(amount) }) {
                        SelectableChip(
                            text: BzrFormatter.fill(bzrString("customer.home.offers.amount"), values: [("amount", amount)]),
                            state: budget == amount ? .selected : .unselected)
                    }
                    .buttonStyle(BzrPressStyle())
                    .frame(maxWidth: .infinity)
                }
            }
            if let error = state.fieldErrors.first {
                BzrText(bzrString(error), style: DesignType.caption.colored(DesignColors.danger))
            }
        }
        .padding(DesignSpace.l)
        .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(DesignColors.surface))
        .bzrShadow(DesignShadows.card)
    }

    private func pricingBody(_ code: String) -> String {
        switch code {
        case "EXECUTION": bzrString("request.pricing.body.EXECUTION")
        case "INSPECTION": bzrString("request.pricing.body.INSPECTION")
        default: ""
        }
    }

    private func heading(_ icon: BzrIconKey, _ title: String) -> some View {
        HStack(spacing: DesignSpace.xs) {
            BzrIcon(icon, tint: DesignColors.primary600, size: DesignSize.icon)
            ScreenHeading(title)
        }
    }

    private var primaryAction: some View {
        PrimaryButton(
            text: bzrString("request.step4.action"), state: state.canContinue ? .normal : .disabled, icon: .arrowForward,
            onClick: { onOpen("SCR-C05") })
    }
}

struct MaterialOptions: View {
    let state: CustomerUIState
    let onSelect: (Int) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: DesignSpace.m) {
            HStack(spacing: DesignSpace.xs) {
                BzrIcon(.construction, tint: DesignColors.primary600, size: DesignSize.icon)
                ScreenHeading(bzrString("request.materials.section"))
            }
            BzrText(bzrString("request.materials.body"), style: DesignType.secondary)
            ForEach(Array(state.materialOptions.enumerated()), id: \.offset) { index, label in
                let code = state.materialCodes.indices.contains(index) ? state.materialCodes[index] : ""
                Button(action: { onSelect(index) }) {
                    RadioCard(
                        title: label, body: materialBody(code),
                        selected: index == state.selectedMaterialIndex, icon: materialIcon(code))
                }.buttonStyle(BzrPressStyle())
            }
            InfoBanner(text: bzrString("request.materials.assurance"))
        }
    }

    private func materialBody(_ code: String) -> String {
        switch code {
        case "CUSTOMER_HAS": bzrString("request.materials.body.CUSTOMER_HAS")
        case "PROVIDER_SUPPLIES": bzrString("request.materials.body.PROVIDER_SUPPLIES")
        case "UNSURE": bzrString("request.materials.body.UNSURE")
        default: ""
        }
    }

    private func materialIcon(_ code: String) -> BzrIconKey {
        switch code {
        case "CUSTOMER_HAS": .inventory2
        case "PROVIDER_SUPPLIES": .receipt
        default: .construction
        }
    }

}
