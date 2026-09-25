package noox.bzr.customer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import noox.bzr.design.Countdown
import noox.bzr.design.EmptyState
import noox.bzr.design.OfferCard
import noox.bzr.design.OfferVariant
import noox.bzr.design.R
import noox.bzr.design.SortChips

@Composable
fun C06OffersScreen(state: CustomerUiState, onAction: (String) -> Unit = {}) {
    CustomerScreen(stringResource(R.string.offers_title), state) {
        if (state.phase == CustomerPhase.Empty) {
            EmptyState(stringResource(R.string.offers_empty_title), stringResource(R.string.offers_empty_body))
            ActionButtons(state.visibleActions, onAction)
        } else {
            ScreenHeading(stringResource(R.string.offers_count))
            Countdown(1320, stringResource(R.string.offers_expires))
            SortChips(stringResource(R.string.sort_title), listOf(stringResource(R.string.sort_top_rated), stringResource(R.string.sort_lowest_price), stringResource(R.string.sort_fastest)), 0)
            OfferCard(stringResource(R.string.offer_provider), "4.9", "86", stringResource(R.string.offer_price), "25 " + stringResource(R.string.unit_minute), stringResource(R.string.badge_top_rated), stringResource(R.string.offer_select), stringResource(R.string.action_chat), OfferVariant.Execution)
            OfferCard(stringResource(R.string.drawer_name), "4.8", "54", stringResource(R.string.offer_inspection_price), stringResource(R.string.offer_scheduled), stringResource(R.string.badge_best_price), stringResource(R.string.offer_select), stringResource(R.string.action_chat), OfferVariant.Inspection, stringResource(R.string.offer_inspection_fee))
            ActionButtons(state.visibleActions.filter { it != "accept_offer" }, onAction)
        }
    }
}
