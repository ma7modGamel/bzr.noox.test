package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.StatItem
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenC07ProviderBinding

/** SCR-C07 (DEC-047): provider profile. */
class C07ProviderView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC07ProviderBinding.inflate(inflater, content)
    var onAction: (String) -> Unit = {}

    init {
        binding.header.rating = "4.9"
        binding.header.services = "86"
        binding.stats.items = listOf(
            StatItem(R.drawable.ic_star, "4.9", string(R.string.stat_rating)),
            StatItem(R.drawable.ic_orders, "86", string(R.string.stat_services)),
            StatItem(R.drawable.ic_calendar, "7", string(R.string.stat_years)),
        )
        binding.bars.max = 5
        binding.bars.items = listOf(
            string(R.string.rating_quality) to 4.9,
            string(R.string.rating_commitment) to 4.8,
            string(R.string.rating_communication) to 4.9,
        )
        binding.review.apply {
            name = string(R.string.review_name); body = string(R.string.review_body); verified = string(R.string.review_verified)
            time = string(R.string.review_time); tag = string(R.string.review_tag); rating = 5
        }
        binding.actions.onAction = { onAction(it) }
    }

    override fun title(state: CustomerUiState) = string(R.string.provider_profile_title)

    override fun renderContent(state: CustomerUiState) {
        val empty = state.phase == CustomerPhase.Empty
        binding.unavailable.isVisible = empty
        listOf(binding.header, binding.stats, binding.aboutTitle, binding.aboutBody, binding.bars, binding.reviewsTitle, binding.review)
            .forEach { it.isVisible = !empty }
        binding.actions.actions = state.visibleActions
    }
}
