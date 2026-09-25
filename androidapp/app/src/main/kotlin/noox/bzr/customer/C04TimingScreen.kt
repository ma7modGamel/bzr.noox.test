package noox.bzr.customer

import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import noox.bzr.design.AppTextField
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.FieldVisualState
import noox.bzr.design.InfoBanner
import noox.bzr.design.PrimaryButton
import noox.bzr.design.R
import noox.bzr.design.RadioCard
import noox.bzr.design.SummaryCard

@Composable
fun C04TimingScreen(state: CustomerUiState, onOpen: (String) -> Unit = {}) {
    CustomerScreen(stringResource(R.string.request_timing_title), state) {
        SummaryCard(stringResource(R.string.request_address_title), listOf(R.drawable.ic_location to stringResource(R.string.request_address_value)), stringResource(R.string.common_change), onEdit = { onOpen("SCR-C14") })
        RadioCard(state.options.getOrElse(0) { "" }, stringResource(R.string.radio_now_body), state.selectedOptionIndex == 0)
        Box(Modifier.clickable { onOpen("SCR-C16") }) {
            RadioCard(state.options.getOrElse(1) { "" }, stringResource(R.string.request_slot_value), state.selectedOptionIndex == 1)
        }
        state.messageKey?.let {
            val message = when (it) {
                "request.address.required" -> R.string.request_address_required
                "request.timing.now_unavailable" -> R.string.request_timing_now_unavailable
                else -> R.string.request_slot_required
            }
            InfoBanner(stringResource(message))
        }
        if (state.showPricing) AppTextField(stringResource(R.string.request_budget_label), stringResource(R.string.field_amount_placeholder), "450", FieldVisualState.Filled)
        else InfoBanner(stringResource(R.string.request_staff_pricing))
        PrimaryButton(stringResource(R.string.common_next), if (state.canContinue) ButtonVisualState.Normal else ButtonVisualState.Disabled, onClick = { onOpen("SCR-C05") })
    }
}
