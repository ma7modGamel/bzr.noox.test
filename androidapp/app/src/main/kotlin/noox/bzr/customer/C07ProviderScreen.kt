package noox.bzr.customer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import noox.bzr.design.AvatarSize
import noox.bzr.design.EmptyState
import noox.bzr.design.ProviderHeader
import noox.bzr.design.R
import noox.bzr.design.RatingBars
import noox.bzr.design.ReviewCard
import noox.bzr.design.StatItem
import noox.bzr.design.StatRow

@Composable
fun C07ProviderScreen(state: CustomerUiState, onAction: (String) -> Unit = {}) {
    CustomerScreen(stringResource(R.string.provider_profile_title), state) {
        if (state.phase == CustomerPhase.Empty) {
            EmptyState(stringResource(R.string.provider_unavailable_title), stringResource(R.string.provider_unavailable_body), stringResource(R.string.common_back_to_offers))
        } else {
            ProviderHeader(stringResource(R.string.offer_provider), "4.9", "86", AvatarSize.Large, stringResource(R.string.provider_verified))
            StatRow(listOf(StatItem(R.drawable.ic_star, "4.9", stringResource(R.string.stat_rating)), StatItem(R.drawable.ic_orders, "86", stringResource(R.string.stat_services)), StatItem(R.drawable.ic_calendar, "7", stringResource(R.string.stat_years))))
            ScreenHeading(stringResource(R.string.provider_about_title), stringResource(R.string.provider_about_body))
            RatingBars(listOf(stringResource(R.string.rating_quality) to 4.9, stringResource(R.string.rating_commitment) to 4.8, stringResource(R.string.rating_communication) to 4.9), 5)
            ScreenHeading(stringResource(R.string.provider_reviews_title))
            ReviewCard(stringResource(R.string.review_name), stringResource(R.string.review_body), stringResource(R.string.review_verified), stringResource(R.string.review_time), stringResource(R.string.review_tag), 5)
        }
        ActionButtons(state.visibleActions, onAction)
    }
}
