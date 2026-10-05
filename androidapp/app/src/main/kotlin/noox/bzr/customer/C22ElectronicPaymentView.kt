package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenC22ElectronicPaymentBinding

/** SCR-C22 (DEC-047): channel, create payment, checkout link or kiosk code, result. */
class C22ElectronicPaymentView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC22ElectronicPaymentBinding.inflate(inflater, content)
    var onChannelSelect: (Int) -> Unit = {}
    var onCreate: () -> Unit = {}
    var onOpenCheckout: () -> Unit = {}
    var onCopyCode: () -> Unit = {}
    private val channels = RadioOptions(binding.channels, resources.getDimensionPixelSize(R.dimen.bremo_space_s), string(R.string.payment_option_body)) { onChannelSelect(it) }

    init {
        binding.total.editText = null
        binding.kioskReference.editText = null
        binding.copyCode.onClick = { onCopyCode() }
        binding.openCheckout.onClick = { onOpenCheckout() }
        binding.create.onClick = { onCreate() }
    }

    override fun title(state: CustomerUiState) = string(R.string.payment_electronic_title)

    override fun renderContent(state: CustomerUiState) {
        val status = state.itemStates.firstOrNull()
        binding.total.rows = listOf(R.drawable.ic_check to state.items.getOrElse(0) { "" })
        binding.channels.isVisible = state.options.isNotEmpty()
        channels.submit(state.options, state.selectedOptionIndex, status.isNullOrBlank() && !state.isBusy)
        binding.creating.isVisible = state.messageKey == "payment.creating"
        val kiosk = status == "PENDING" && state.items.getOrElse(1) { "" }.isNotBlank()
        binding.kioskReady.isVisible = kiosk
        binding.kioskReference.isVisible = kiosk
        binding.copyCode.isVisible = kiosk
        binding.expires.isVisible = kiosk
        if (kiosk) {
            binding.kioskReference.rows = listOf(R.drawable.ic_info to state.items[1])
            binding.expires.seconds = state.countdownSeconds
        }
        binding.checkoutReady.isVisible = status == "PENDING" && !kiosk
        binding.openCheckout.isVisible = status == "PENDING" && !kiosk
        binding.success.isVisible = status == "SUCCEEDED"
        binding.failed.isVisible = status == "FAILED"
        binding.expired.isVisible = status == "EXPIRED"
        binding.create.isVisible = "pay_electronic" in state.visibleActions && (status.isNullOrBlank() || status in listOf("FAILED", "EXPIRED"))
        binding.create.state = if (state.isBusy) ButtonVisualState.Loading else continueState(state)
    }
}
