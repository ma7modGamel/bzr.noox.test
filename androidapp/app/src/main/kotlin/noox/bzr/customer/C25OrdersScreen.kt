package noox.bzr.customer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import noox.bzr.design.BzrText
import noox.bzr.design.EmptyState
import noox.bzr.design.OrderCard
import noox.bzr.design.R
import noox.bzr.design.SortChips
import noox.bzr.design.generated.DesignType

@Composable
fun C25OrdersScreen(
    state: CustomerUiState,
    onTabSelect: (Int) -> Unit = {},
    onOrderSelect: (Int) -> Unit = {},
) {
    CustomerScreen(stringResource(R.string.orders_title), state) {
        SortChips(
            null, listOf(stringResource(R.string.orders_current), stringResource(R.string.orders_past)),
            state.selectedOptionIndex, onTabSelect,
        )
        if (state.phase == CustomerPhase.Empty) {
            val past = state.selectedOptionIndex == 1
            EmptyState(
                stringResource(if (past) R.string.orders_past_empty_title else R.string.orders_current_empty_title),
                stringResource(if (past) R.string.orders_past_empty_body else R.string.orders_current_empty_body),
            )
        }
        state.items.indices.forEach { index ->
            OrderCard(
                state.items[index], state.itemDetails.getOrElse(index) { "" },
                state.itemStates.getOrElse(index) { "" }, onClick = { onOrderSelect(index) },
            )
        }
        if (state.isBusy) BzrText(stringResource(R.string.orders_loading_more), DesignType.secondary)
    }
}
