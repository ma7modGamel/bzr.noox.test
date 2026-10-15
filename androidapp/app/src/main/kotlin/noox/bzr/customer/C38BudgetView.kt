package noox.bzr.customer

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.LinearLayout
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.design.views.RadioCardView
import noox.bzr.design.views.SnapshotMode
import noox.bzr.design.views.cardShadow
import noox.bzr.gallery.databinding.ScreenC38BudgetBinding

/** SCR-C38, step 4 of 5 (DEC-071): materials; in marketplace the pricing mode and an optional budget (07, BR-015). */
class C38BudgetView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC38BudgetBinding.inflate(inflater, content)
    var onMaterialSelect: (Int) -> Unit = {}
    var onPricingSelect: (Int) -> Unit = {}
    var onBudgetChange: (String) -> Unit = {}
    var onOpen: (String) -> Unit = {}
    private var syncing = false

    init {
        pinAction(binding.next)
        addRequestProgress(4)
        binding.budgetCard.cardShadow()
        binding.areaAverage.text = string(R.string.request_budget_area_average)
        binding.staffPricing.text = string(R.string.request_staff_pricing)
        binding.assurance.text = string(R.string.request_materials_assurance)
        if (SnapshotMode.enabled) binding.budget.setHorizontallyScrolling(false)
        binding.budget.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (!syncing) onBudgetChange(s?.toString().orEmpty())
            }
        })
        binding.next.onClick = { onOpen("SCR-C05") }
    }

    override fun title(state: CustomerUiState) = string(R.string.request_budget_title)

    override fun renderContent(state: CustomerUiState) {
        binding.budgetSection.isVisible = state.showPricing
        binding.staffPricing.isVisible = !state.showPricing
        val budget = state.fieldValues.getOrElse(1) { "" }
        if (binding.budget.text.toString() != budget) {
            syncing = true
            binding.budget.setText(budget)
            syncing = false
        }
        binding.budgetError.isVisible = state.fieldErrors.isNotEmpty()
        binding.areaAverage.isVisible = state.itemDetails.isNotEmpty()
        renderQuickAmounts(budget, state.itemDetails)
        binding.pricing.removeAllViews()
        state.pricingOptions.forEachIndexed { index, label ->
            val code = state.pricingCodes.getOrElse(index) { "" }
            binding.pricing.addView(
                option(label, index == state.selectedPricingIndex, { onPricingSelect(index) }) {
                    icon = if (code == "INSPECTION") R.drawable.ic_search else R.drawable.ic_payments
                    body = when (code) {
                        "EXECUTION" -> string(R.string.request_pricing_body_execution)
                        "INSPECTION" -> string(R.string.request_pricing_body_inspection)
                        else -> ""
                    }
                },
                LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
            )
        }
        renderMaterialOptions(binding.materials, state, onMaterialSelect)
        binding.next.state = continueState(state)
    }

    private fun renderQuickAmounts(budget: String, amounts: List<String>) {
        binding.quickAmounts.removeAllViews()
        amounts.forEach { amount ->
            binding.quickAmounts.addView(
                noox.bzr.design.views.SelectableChipView(context).apply {
                    text = noox.bzr.design.BzrFormat.fill(string(R.string.customer_home_offers_amount), "amount" to amount)
                    state = if (budget == amount) noox.bzr.design.SelectionState.Selected else noox.bzr.design.SelectionState.Unselected
                    setOnClickListener { onBudgetChange(amount) }
                },
                LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f),
            )
        }
    }


    private fun option(label: String, chosen: Boolean, onSelect: () -> Unit, style: RadioCardView.() -> Unit) =
        RadioCardView(context).apply {
            title = label
            style()
            selected = chosen
            setOnClickListener { onSelect() }
        }
}

/** Shared material choices; labels and stable codes come from the API. */
internal fun renderMaterialOptions(container: LinearLayout, state: CustomerUiState, onSelect: (Int) -> Unit) {
    container.removeAllViews()
    state.materialOptions.forEachIndexed { index, label ->
        val code = state.materialCodes.getOrElse(index) { "" }
        container.addView(RadioCardView(container.context).apply {
            title = label
            selected = index == state.selectedMaterialIndex
            icon = when (code) {
                "CUSTOMER_HAS" -> R.drawable.ic_inventory_2
                "PROVIDER_SUPPLIES" -> R.drawable.ic_receipt
                else -> R.drawable.ic_construction
            }
            body = when (code) {
                "CUSTOMER_HAS" -> context.getString(R.string.request_materials_body_customer_has)
                "PROVIDER_SUPPLIES" -> context.getString(R.string.request_materials_body_provider_supplies)
                "UNSURE" -> context.getString(R.string.request_materials_body_unsure)
                else -> ""
            }
            setOnClickListener { onSelect(index) }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }
}
