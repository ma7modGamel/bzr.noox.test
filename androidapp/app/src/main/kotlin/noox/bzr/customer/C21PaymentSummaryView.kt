package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenC21PaymentSummaryBinding

/** SCR-C21 (DEC-047): amounts, payment method, electronic payment. */
class C21PaymentSummaryView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC21PaymentSummaryBinding.inflate(inflater, content)
    var onMethodSelect: (Int) -> Unit = {}
    var onPay: () -> Unit = {}
    var onAction: (String) -> Unit = {}
    private val methods = RadioOptions(binding.methods, resources.getDimensionPixelSize(R.dimen.bremo_space_s), string(R.string.payment_option_body)) { onMethodSelect(it) }

    init {
        binding.amounts.editText = null
        binding.pay.onClick = { onPay() }
        binding.actions.onAction = { onAction(it) }
    }

    override fun title(state: CustomerUiState) = string(R.string.payment_summary_title)

    override fun renderContent(state: CustomerUiState) {
        binding.amounts.rows = listOf(
            R.drawable.ic_orders to "${string(R.string.payment_amount_labor)} · ${state.items.getOrElse(0) { "" }}",
            R.drawable.ic_info to "${string(R.string.payment_amount_materials)} · ${state.items.getOrElse(1) { "" }}",
            R.drawable.ic_check to "${string(R.string.payment_amount_total)} · ${state.items.getOrElse(2) { "" }}",
        )
        binding.methods.isVisible = state.options.isNotEmpty()
        methods.submit(state.options, state.selectedOptionIndex, "change_payment_method" in state.visibleActions && !state.isBusy)
        binding.pay.isVisible = "pay_electronic" in state.visibleActions
        binding.pay.state = if (state.isBusy) ButtonVisualState.Loading else continueState(state)
        binding.actions.actions = state.visibleActions.filter { it == "open_dispute" }
    }
}
