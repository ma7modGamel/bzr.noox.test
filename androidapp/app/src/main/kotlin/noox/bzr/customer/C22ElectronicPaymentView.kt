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
    var onReceipt: () -> Unit = {}
    var onReferenceChange: (String) -> Unit = {}
    var onRefresh: () -> Unit = {}
    private val channels = RadioOptions(binding.channels, resources.getDimensionPixelSize(R.dimen.bremo_space_s), string(R.string.payment_option_body)) { onChannelSelect(it) }

    init {
        pinAction(binding.create)
        binding.instapayDetails.editText = null
        binding.receipt.onClick = { onReceipt() }
        binding.transferReference.onValueChange = { onReferenceChange(it) }
        binding.refresh.onClick = { onRefresh() }
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
        channels.submit(state.options, state.selectedOptionIndex, (status.isNullOrBlank() || status in listOf("FAILED", "EXPIRED")) && !state.isBusy)
        val editable = status.isNullOrBlank() || status in listOf("FAILED", "EXPIRED")
        val form = state.isInstapay && editable
        binding.instapayDetails.isVisible = state.isInstapay
        binding.instapayDetails.rows = state.fieldValues.drop(1).take(3).filter(String::isNotBlank).map { R.drawable.ic_info to it }
        binding.instapayInstructions.isVisible = form
        binding.transferReference.isVisible = form
        binding.transferReference.value = state.fieldValues.firstOrNull().orEmpty()
        binding.receipt.isVisible = form
        binding.receipt.text = string(if (state.hasPhoto) R.string.payment_instapay_receipt_attached else R.string.payment_instapay_receipt)
        binding.receipt.enabled = !state.isBusy
        binding.transferPending.isVisible = status == "PENDING_VERIFICATION"
        binding.refresh.enabled = !state.isBusy
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
        binding.failed.isVisible = status == "FAILED" || state.messageKey == "media.upload_error"
        binding.failed.text = string(if (state.messageKey == "media.upload_error") R.string.media_upload_error else if (state.isInstapay) R.string.payment_instapay_retry else R.string.payment_failed)
        if (status == "FAILED" && state.fieldValues.getOrElse(4) { "" }.isNotBlank()) binding.failed.text = state.fieldValues[4] + "\n" + binding.failed.text
        binding.expired.isVisible = status == "EXPIRED"
        binding.create.isVisible = ("pay_electronic" in state.visibleActions || "submit_instapay_transfer" in state.visibleActions) && editable
        binding.create.text = string(if (state.isInstapay) R.string.payment_instapay_submit else R.string.payment_create)
        binding.create.state = if (state.isBusy) ButtonVisualState.Loading else continueState(state)
    }
}
