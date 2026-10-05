import BzrCore
import DesignSystem
import SwiftUI

public struct C04TimingView: View {
    let state: CustomerUIState
    let onOpen: (String) -> Void
    let onTimingSelect: (Int) -> Void
    let onMaterialSelect: (Int) -> Void
    let onPricingSelect: (Int) -> Void
    let onBudgetChange: (String) -> Void

    public init(
        state: CustomerUIState, onOpen: @escaping (String) -> Void = { _ in },
        onTimingSelect: @escaping (Int) -> Void = { _ in },
        onMaterialSelect: @escaping (Int) -> Void = { _ in },
        onPricingSelect: @escaping (Int) -> Void = { _ in },
        onBudgetChange: @escaping (String) -> Void = { _ in }
    ) {
        self.state = state
        self.onOpen = onOpen
        self.onTimingSelect = onTimingSelect
        self.onMaterialSelect = onMaterialSelect
        self.onPricingSelect = onPricingSelect
        self.onBudgetChange = onBudgetChange
    }

    public var body: some View {
        CustomerScreen(title: bzrString("request.timing.title"), state: state, bottomBar: AnyView(primaryAction)) {
            StepIndicator(
                current: 2, total: 3,
                label: BzrFormatter.fill(
                    bzrString("format.step"), values: [("current", "2"), ("total", "3")]))
            SummaryCard(
                title: bzrString("request.address.title"),
                rows: (state.fieldValues.first ?? "").isEmpty ? [] : [(.location, state.fieldValues[0])],
                editText: bzrString("common.change"),
                onEdit: { onOpen("SCR-C14") })
            Button(
                action: { onTimingSelect(0) },
                label: {
                    RadioCard(
                        title: state.options.first ?? "", body: bzrString("radio.now.body"),
                        selected: state.selectedOptionIndex == 0)
                }
            )
            .buttonStyle(BzrPressStyle())
            Button(
                action: { onOpen("SCR-C16") },
                label: {
                    RadioCard(
                        title: state.options.indices.contains(1) ? state.options[1] : "",
                        body: slotBody, selected: state.selectedOptionIndex == 1)
                }
            )
            .buttonStyle(BzrPressStyle())
            if let message = state.messageKey { InfoBanner(text: bzrString(message)) }
            ScreenHeading(bzrString("request.materials.title"))
            // DEC-063: options in equal columns, like the category tiles.
            BzrServiceGrid {
                ForEach(Array(state.materialOptions.enumerated()), id: \.offset) { index, label in
                    Button(
                        action: { onMaterialSelect(index) },
                        label: {
                            SelectableChip(
                                text: label,
                                state: index == state.selectedMaterialIndex ? .selected : .unselected)
                        }
                    )
                    .buttonStyle(BzrPressStyle())
                }
            }
            if state.showPricing {
                ScreenHeading(bzrString("request.pricing.title"))
                ForEach(Array(state.pricingOptions.enumerated()), id: \.offset) { index, label in
                    Button(
                        action: { onPricingSelect(index) },
                        label: {
                            RadioCard(title: label, body: "", selected: index == state.selectedPricingIndex)
                        }
                    )
                    .buttonStyle(BzrPressStyle())
                }
                AppTextField(
                    label: bzrString("request.budget.label"),
                    placeholder: bzrString("field.amount.placeholder"),
                    value: state.fieldValues.indices.contains(1) ? state.fieldValues[1] : "",
                    state: state.fieldErrors.isEmpty ? .empty : .error,
                    error: state.fieldErrors.first.map(bzrString), onValueChange: onBudgetChange)
            } else {
                InfoBanner(text: bzrString("request.staff.pricing"))
            }

        }
    }

    /// The chosen slot, or a prompt — never a sample date (6ب).
    private var primaryAction: some View {
        PrimaryButton(
            text: bzrString("common.next"), state: state.canContinue ? .normal : .disabled,
            onClick: { onOpen("SCR-C05") })
    }

    private var slotBody: String {
        let label = state.fieldValues.count > 2 ? state.fieldValues[2] : ""
        return label.isEmpty ? bzrString("request.slot.choose") : label
    }
}
