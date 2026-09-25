package noox.bzr.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.Countdown
import noox.bzr.design.InfoBanner
import noox.bzr.design.PrimaryButton
import noox.bzr.design.R
import noox.bzr.design.RadioCard
import noox.bzr.design.SecondaryButton
import noox.bzr.design.SummaryCard
import noox.bzr.design.WarningBox
import noox.bzr.design.generated.DesignSpace

@Composable
fun C22ElectronicPaymentScreen(
    state: CustomerUiState,
    onChannelSelect: (Int) -> Unit = {},
    onCreate: () -> Unit = {},
    onOpenCheckout: () -> Unit = {},
    onCopyCode: () -> Unit = {},
) {
    CustomerScreen(stringResource(R.string.payment_electronic_title), state) {
        ScreenHeading(stringResource(R.string.payment_electronic_heading))
        SummaryCard(stringResource(R.string.payment_amount_total), listOf(R.drawable.ic_check to state.items.getOrElse(0) { "" }), null)
        ScreenHeading(stringResource(R.string.payment_channel_title))
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(DesignSpace.s)) {
            state.options.forEachIndexed { index, label ->
                Column(Modifier.clickable(enabled = state.itemStates.firstOrNull().isNullOrBlank() && !state.isBusy) { onChannelSelect(index) }) {
                    RadioCard(label, stringResource(R.string.payment_option_body), state.selectedOptionIndex == index)
                }
            }
        }
        if (state.messageKey == "payment.creating") InfoBanner(stringResource(R.string.payment_creating))
        PaymentResult(state, onOpenCheckout, onCopyCode)
        if (
            "pay_electronic" in state.visibleActions &&
            (state.itemStates.firstOrNull().isNullOrBlank() || state.itemStates.firstOrNull() in listOf("FAILED", "EXPIRED"))
        ) {
            PrimaryButton(
                stringResource(R.string.payment_create),
                if (state.isBusy) ButtonVisualState.Loading else if (state.canContinue) ButtonVisualState.Normal else ButtonVisualState.Disabled,
                onClick = onCreate,
            )
        }
    }
}

@Composable
private fun PaymentResult(state: CustomerUiState, onOpenCheckout: () -> Unit, onCopyCode: () -> Unit) {
    when (state.itemStates.firstOrNull()) {
        "PENDING" -> if (state.items.getOrElse(1) { "" }.isNotBlank()) {
            InfoBanner(stringResource(R.string.payment_kiosk_ready))
            SummaryCard(stringResource(R.string.payment_kiosk_reference), listOf(R.drawable.ic_info to state.items[1]), null)
            SecondaryButton(stringResource(R.string.payment_kiosk_copy), onClick = onCopyCode)
            Countdown(state.countdownSeconds, stringResource(R.string.payment_expires))
        } else {
            InfoBanner(stringResource(R.string.payment_checkout_ready))
            SecondaryButton(stringResource(R.string.payment_checkout_open), onClick = onOpenCheckout)
        }
        "SUCCEEDED" -> InfoBanner(stringResource(R.string.payment_success))
        "FAILED" -> WarningBox(stringResource(R.string.payment_failed))
        "EXPIRED" -> WarningBox(stringResource(R.string.payment_expired))
    }
}
