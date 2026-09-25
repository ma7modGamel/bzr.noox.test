package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenC26OrderHistoryBinding

/** SCR-C26 (DEC-047): past order details and receipt. */
class C26OrderHistoryView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC26OrderHistoryBinding.inflate(inflater, content)
    var onAction: (String) -> Unit = {}

    init {
        listOf(binding.service, binding.receipt, binding.proposals).forEach { it.editText = null }
        binding.provider.rating = ""
        binding.provider.services = null
        binding.actions.onAction = { onAction(it) }
    }

    override fun title(state: CustomerUiState) = string(R.string.order_details_title)

    override fun renderContent(state: CustomerUiState) {
        binding.status.text = state.displayStatus
        val serviceLabels = listOf(R.string.receipt_number, R.string.receipt_category, R.string.receipt_problem, R.string.receipt_area, R.string.receipt_visit).map(::string)
        val receiptLabels = listOf(R.string.receipt_labor, R.string.receipt_materials, R.string.receipt_total, R.string.receipt_payment_method, R.string.receipt_payment_status).map(::string)
        binding.service.rows = historyRows(state.items.take(5), serviceLabels)
        val providerName = state.options.firstOrNull()?.takeIf(String::isNotBlank)
        binding.providerTitle.isVisible = providerName != null
        binding.provider.isVisible = providerName != null
        binding.provider.name = providerName.orEmpty()
        binding.receipt.isVisible = state.items.size > 5
        if (state.items.size > 5) binding.receipt.rows = historyRows(state.items.drop(5), receiptLabels)
        binding.reason.isVisible = state.items.size <= 5 && state.messageKey != null
        if (binding.reason.isVisible) {
            val label = string(if (state.messageKey == "order.cancelled.reason") R.string.order_cancelled_reason else R.string.order_expired_reason)
            binding.reason.text = "$label: ${state.options.getOrElse(1) { "" }}"
        }
        binding.proposals.isVisible = state.itemDetails.isNotEmpty()
        binding.proposals.rows = state.itemDetails.map { R.drawable.ic_info to it }
        binding.actions.actions = state.visibleActions
    }

    private fun historyRows(values: List<String>, labels: List<String>): List<Pair<Int, String>> =
        values.mapIndexed { index, value -> R.drawable.ic_info to "${labels.getOrElse(index) { "" }}: $value" }
}
