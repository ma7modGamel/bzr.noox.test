package noox.bzr.customer

import android.content.Context
import android.widget.LinearLayout
import androidx.core.view.isVisible
import noox.bzr.design.FieldVisualState
import noox.bzr.design.R
import noox.bzr.design.SelectionState
import noox.bzr.gallery.databinding.ScreenC04TimingBinding

/** SCR-C04 (DEC-047): address, now / scheduled, budget or staff pricing. */
class C04TimingView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC04TimingBinding.inflate(inflater, content)
    var onOpen: (String) -> Unit = {}
    var onTimingSelect: (Int) -> Unit = {}
    var onMaterialSelect: (Int) -> Unit = {}
    var onPricingSelect: (Int) -> Unit = {}
    var onBudgetChange: (String) -> Unit = {}

    init {
        pinAction(binding.next)
        addRequestProgress(2)
        binding.address.onEdit = { onOpen("SCR-C14") }
        binding.now.setOnClickListener { onTimingSelect(0) }
        binding.slot.setOnClickListener { onOpen("SCR-C16") }
        binding.next.onClick = { onOpen("SCR-C05") }
        binding.budget.onValueChange = { onBudgetChange(it) }
    }

    override fun title(state: CustomerUiState) = string(R.string.request_timing_title)

    override fun renderContent(state: CustomerUiState) {
        val address = state.fieldValues.firstOrNull().orEmpty()
        binding.address.rows = if (address.isBlank()) emptyList() else listOf(R.drawable.ic_location to address)
        binding.now.title = state.options.getOrElse(0) { "" }
        binding.now.selected = state.selectedOptionIndex == 0
        binding.slot.title = state.options.getOrElse(1) { "" }
        binding.slot.selected = state.selectedOptionIndex == 1
        // The chosen slot, or a prompt — never a sample date (6ب).
        binding.slot.body = state.fieldValues.getOrNull(2).orEmpty().ifBlank { string(R.string.request_slot_choose) }
        binding.message.isVisible = state.messageKey != null
        state.messageKey?.let {
            binding.message.text = string(
                when (it) {
                    "request.address.required" -> R.string.request_address_required
                    "request.timing.now_unavailable" -> R.string.request_timing_now_unavailable
                    "request.category.unavailable" -> R.string.request_category_unavailable
                    else -> R.string.request_slot_required
                },
            )
        }
        binding.budget.isVisible = state.showPricing
        binding.budget.value = state.fieldValues.getOrElse(1) { "" }
        binding.budget.state = if (state.fieldErrors.isEmpty()) FieldVisualState.Empty else FieldVisualState.Error
        binding.budget.error = state.fieldErrors.firstOrNull()?.let { string(R.string.request_budget_invalid) }
        binding.pricingTitle.isVisible = state.showPricing
        binding.pricing.isVisible = state.showPricing
        binding.staffPricing.isVisible = !state.showPricing
        binding.materials.removeAllViews()
        state.materialOptions.forEachIndexed { index, label ->
            binding.materials.addView(
                noox.bzr.design.views.SelectableChipView(context).apply {
                    text = label
                    this.state = if (index == state.selectedMaterialIndex) SelectionState.Selected else SelectionState.Unselected
                    setOnClickListener { onMaterialSelect(index) }
                },
                LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
            )
        }
        binding.pricing.removeAllViews()
        state.pricingOptions.forEachIndexed { index, label ->
            binding.pricing.addView(
                noox.bzr.design.views.RadioCardView(context).apply {
                    title = label
                    body = ""
                    selected = index == state.selectedPricingIndex
                    setOnClickListener { onPricingSelect(index) }
                },
                LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
            )
        }
        binding.next.state = continueState(state)
    }
}
