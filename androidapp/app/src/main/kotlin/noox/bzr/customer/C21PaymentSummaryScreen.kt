package noox.bzr.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.InfoBanner
import noox.bzr.design.PrimaryButton
import noox.bzr.design.R
import noox.bzr.design.RadioCard
import noox.bzr.design.SummaryCard
import noox.bzr.design.generated.DesignSpace

@Composable
fun C21PaymentSummaryScreen(
    state: CustomerUiState,
    onMethodSelect: (Int) -> Unit = {},
    onPay: () -> Unit = {},
    onAction: (String) -> Unit = {},
) {
    CustomerScreen(stringResource(R.string.payment_summary_title), state) {
        ScreenHeading(stringResource(R.string.payment_summary_heading))
        SummaryCard(
            stringResource(R.string.payment_summary_heading),
            listOf(
                R.drawable.ic_orders to "${stringResource(R.string.payment_amount_labor)} · ${state.items.getOrElse(0) { "" }}",
                R.drawable.ic_info to "${stringResource(R.string.payment_amount_materials)} · ${state.items.getOrElse(1) { "" }}",
                R.drawable.ic_check to "${stringResource(R.string.payment_amount_total)} · ${state.items.getOrElse(2) { "" }}",
            ),
            null,
        )
        ScreenHeading(stringResource(R.string.payment_method_title))
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(DesignSpace.s)) {
            state.options.forEachIndexed { index, label ->
                Column(Modifier.clickable(enabled = "change_payment_method" in state.visibleActions && !state.isBusy) { onMethodSelect(index) }) {
                    RadioCard(label, stringResource(R.string.payment_option_body), state.selectedOptionIndex == index)
                }
            }
        }
        InfoBanner(stringResource(R.string.payment_option_body))
        if ("pay_electronic" in state.visibleActions) {
            PrimaryButton(
                stringResource(R.string.action_pay_electronic),
                if (state.isBusy) ButtonVisualState.Loading else if (state.canContinue) ButtonVisualState.Normal else ButtonVisualState.Disabled,
                onClick = onPay,
            )
        }
        ActionButtons(state.visibleActions.filter { it == "open_dispute" }, onAction)
    }
}
