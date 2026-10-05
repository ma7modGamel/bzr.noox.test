package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenC23CompletionBinding

/** SCR-C23 (DEC-047): completion summary and confirmation. */
class C23CompletionView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC23CompletionBinding.inflate(inflater, content)
    var onAction: (String) -> Unit = {}

    init {
        binding.amounts.editText = null
        binding.confirm.onClick = { onAction("confirm_completion") }
        binding.report.onClick = { onAction("open_dispute") }
    }

    override fun title(state: CustomerUiState) = string(R.string.completion_confirm_title)

    override fun renderContent(state: CustomerUiState) {
        binding.amounts.rows = listOf(
            R.drawable.ic_account to "${string(R.string.completion_provider)} · ${state.items.getOrElse(0) { "" }}",
            R.drawable.ic_orders to "${string(R.string.payment_amount_labor)} · ${state.items.getOrElse(1) { "" }}",
            R.drawable.ic_info to "${string(R.string.payment_amount_materials)} · ${state.items.getOrElse(2) { "" }}",
            R.drawable.ic_check to "${string(R.string.payment_amount_total)} · ${state.items.getOrElse(3) { "" }}",
        )
        binding.confirm.isVisible = "confirm_completion" in state.visibleActions
        binding.confirm.state = if (state.isBusy) ButtonVisualState.Loading else continueState(state)
        binding.report.isVisible = "open_dispute" in state.visibleActions
    }
}
