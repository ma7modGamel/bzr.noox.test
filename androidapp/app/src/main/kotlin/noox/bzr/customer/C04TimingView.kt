package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenC04TimingBinding

/** SCR-C04 (DEC-047): address, now / scheduled, budget or staff pricing. */
class C04TimingView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC04TimingBinding.inflate(inflater, content)
    var onOpen: (String) -> Unit = {}

    init {
        binding.address.rows = listOf(R.drawable.ic_location to string(R.string.request_address_value))
        binding.address.onEdit = { onOpen("SCR-C14") }
        binding.slot.setOnClickListener { onOpen("SCR-C16") }
        binding.budget.value = "450"
        binding.next.onClick = { onOpen("SCR-C05") }
    }

    override fun title(state: CustomerUiState) = string(R.string.request_timing_title)

    override fun renderContent(state: CustomerUiState) {
        binding.now.title = state.options.getOrElse(0) { "" }
        binding.now.selected = state.selectedOptionIndex == 0
        binding.slot.title = state.options.getOrElse(1) { "" }
        binding.slot.selected = state.selectedOptionIndex == 1
        binding.message.isVisible = state.messageKey != null
        state.messageKey?.let {
            binding.message.text = string(
                when (it) {
                    "request.address.required" -> R.string.request_address_required
                    "request.timing.now_unavailable" -> R.string.request_timing_now_unavailable
                    else -> R.string.request_slot_required
                },
            )
        }
        binding.budget.isVisible = state.showPricing
        binding.staffPricing.isVisible = !state.showPricing
        binding.next.state = continueState(state)
    }
}
