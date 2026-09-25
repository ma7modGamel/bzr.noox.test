package noox.bzr.customer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import noox.bzr.design.BottomNav
import noox.bzr.design.EmptyState
import noox.bzr.design.OrderCard
import noox.bzr.design.PrimaryButton
import noox.bzr.design.R
import noox.bzr.design.SelectableTile
import noox.bzr.design.SelectionState

@Composable
fun C01HomeScreen(state: CustomerUiState, onOpen: (String) -> Unit = {}) {
    CustomerScreen(stringResource(R.string.nav_home), state) {
        ScreenHeading(stringResource(R.string.customer_greeting), stringResource(R.string.customer_home_subtitle))
        ScreenHeading(stringResource(R.string.customer_categories_title))
        TwoColumns(
            { SelectableTile(stringResource(R.string.tile_plumbing), R.drawable.ic_home, SelectionState.Selected) },
            { SelectableTile(stringResource(R.string.tile_electricity), R.drawable.ic_warning, SelectionState.Unselected) },
        )
        PrimaryButton(stringResource(R.string.customer_new_request), onClick = { onOpen("SCR-C03") })
        ScreenHeading(stringResource(R.string.customer_current_orders_title))
        if (state.phase == CustomerPhase.Empty) {
            EmptyState(stringResource(R.string.empty_title), stringResource(R.string.empty_body))
        } else {
            OrderCard(stringResource(R.string.order_title), stringResource(R.string.order_subtitle), stringResource(R.string.order_status), onClick = { onOpen("SCR-C09") })
        }
        BottomNav(
            listOf(
                R.drawable.ic_home to stringResource(R.string.nav_home),
                R.drawable.ic_orders to stringResource(R.string.nav_orders),
                R.drawable.ic_chat to stringResource(R.string.nav_messages),
                R.drawable.ic_account to stringResource(R.string.nav_account),
            ),
            0,
            onSelect = { index -> listOf("SCR-C01", "SCR-C25", "SCR-C18", "SCR-C02").getOrNull(index)?.let(onOpen) },
        )
    }
}
