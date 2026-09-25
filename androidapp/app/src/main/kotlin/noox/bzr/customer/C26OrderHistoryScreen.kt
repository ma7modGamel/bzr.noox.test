package noox.bzr.customer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import noox.bzr.design.AvatarSize
import noox.bzr.design.Badge
import noox.bzr.design.BzrText
import noox.bzr.design.InfoBanner
import noox.bzr.design.ProviderHeader
import noox.bzr.design.R
import noox.bzr.design.SummaryCard
import noox.bzr.design.generated.DesignType

@Composable
fun C26OrderHistoryScreen(state: CustomerUiState, onAction: (String) -> Unit = {}) {
    CustomerScreen(stringResource(R.string.order_details_title), state) {
        Badge(state.displayStatus)
        SummaryCard(
            stringResource(R.string.order_details_service),
            historyRows(state.items.take(5), serviceLabels()), null,
        )
        state.options.firstOrNull()?.takeIf(String::isNotBlank)?.let {
            BzrText(stringResource(R.string.order_details_provider), DesignType.sectionTitle)
            ProviderHeader(it, "", null, AvatarSize.Small, stringResource(R.string.provider_verified))
        }
        if (state.items.size > 5) {
            SummaryCard(stringResource(R.string.receipt_title), historyRows(state.items.drop(5), receiptLabels()), null)
        } else if (state.messageKey != null) {
            InfoBanner("${stringResource(if (state.messageKey == "order.cancelled.reason") R.string.order_cancelled_reason else R.string.order_expired_reason)}: ${state.options.getOrElse(1) { "" }}")
        }
        if (state.itemDetails.isNotEmpty()) {
            SummaryCard(
                stringResource(R.string.receipt_proposals),
                state.itemDetails.map { R.drawable.ic_info to it }, null,
            )
        }
        ActionButtons(state.visibleActions, onAction)
    }
}

@Composable
private fun serviceLabels(): List<String> = listOf(
    stringResource(R.string.receipt_number), stringResource(R.string.receipt_category),
    stringResource(R.string.receipt_problem), stringResource(R.string.receipt_area),
    stringResource(R.string.receipt_visit),
)

@Composable
private fun receiptLabels(): List<String> = listOf(
    stringResource(R.string.receipt_labor), stringResource(R.string.receipt_materials),
    stringResource(R.string.receipt_total), stringResource(R.string.receipt_payment_method),
    stringResource(R.string.receipt_payment_status),
)

private fun historyRows(values: List<String>, labels: List<String>): List<Pair<Int, String>> =
    values.mapIndexed { index, value -> R.drawable.ic_info to "${labels.getOrElse(index) { "" }}: $value" }
