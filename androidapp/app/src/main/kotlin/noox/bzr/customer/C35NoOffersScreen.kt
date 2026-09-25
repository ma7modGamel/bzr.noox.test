package noox.bzr.customer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import noox.bzr.design.EmptyState
import noox.bzr.design.R

@Composable
fun C35NoOffersScreen(state: CustomerUiState, onAction: (String) -> Unit = {}) {
    CustomerScreen(stringResource(R.string.no_offers_title), state) {
        EmptyState(stringResource(R.string.no_offers_title), stringResource(R.string.no_offers_body))
        ActionButtons(state.visibleActions, onAction)
    }
}
