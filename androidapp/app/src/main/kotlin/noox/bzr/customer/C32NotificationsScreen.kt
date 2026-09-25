package noox.bzr.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import noox.bzr.design.EmptyState
import noox.bzr.design.R
import noox.bzr.design.SummaryCard

@Composable
fun C32NotificationsScreen(state: CustomerUiState, onOpen: (Int) -> Unit = {}) {
    CustomerScreen(stringResource(R.string.notifications_title), state) {
        if (state.items.isEmpty()) {
            EmptyState(
                stringResource(R.string.notifications_empty_title),
                stringResource(R.string.notifications_empty_body),
            )
        } else {
            state.items.forEachIndexed { index, title ->
                Box(Modifier.clickable(enabled = !state.isBusy) { onOpen(index) }) {
                    SummaryCard(
                        title,
                        listOf(
                            R.drawable.ic_messages to state.itemDetails.getOrElse(index) { "" },
                            R.drawable.ic_clock to state.fieldValues.getOrElse(index) { "" },
                        ),
                        if (state.itemStates.getOrNull(index) == "unread") stringResource(R.string.notifications_unread) else null,
                    )
                }
            }
        }
    }
}
