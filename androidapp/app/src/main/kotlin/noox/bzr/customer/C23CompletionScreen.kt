package noox.bzr.customer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.InfoBanner
import noox.bzr.design.PrimaryButton
import noox.bzr.design.R
import noox.bzr.design.SecondaryButton
import noox.bzr.design.SummaryCard

@Composable
fun C23CompletionScreen(state: CustomerUiState, onAction: (String) -> Unit = {}) {
    CustomerScreen(stringResource(R.string.completion_confirm_title), state) {
        ScreenHeading(stringResource(R.string.completion_confirm_heading))
        SummaryCard(
            stringResource(R.string.payment_summary_heading),
            listOf(
                R.drawable.ic_account to "${stringResource(R.string.completion_provider)} · ${state.items.getOrElse(0) { "" }}",
                R.drawable.ic_orders to "${stringResource(R.string.payment_amount_labor)} · ${state.items.getOrElse(1) { "" }}",
                R.drawable.ic_info to "${stringResource(R.string.payment_amount_materials)} · ${state.items.getOrElse(2) { "" }}",
                R.drawable.ic_check to "${stringResource(R.string.payment_amount_total)} · ${state.items.getOrElse(3) { "" }}",
            ),
            null,
        )
        InfoBanner(stringResource(R.string.completion_confirm_help))
        if ("confirm_completion" in state.visibleActions) {
            PrimaryButton(
                stringResource(R.string.action_confirm_completion),
                if (state.isBusy) ButtonVisualState.Loading else if (state.canContinue) ButtonVisualState.Normal else ButtonVisualState.Disabled,
                onClick = { onAction("confirm_completion") },
            )
        }
        if ("open_dispute" in state.visibleActions) {
            SecondaryButton(stringResource(R.string.completion_report_problem), onClick = { onAction("open_dispute") })
        }
    }
}
