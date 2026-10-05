package noox.bzr.customer

import android.content.Context
import android.widget.LinearLayout
import androidx.core.view.isVisible
import noox.bzr.design.SelectionState
import noox.bzr.design.StatItem
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenC07ProviderBinding

/** SCR-C07 (DEC-047): provider profile. */
class C07ProviderView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC07ProviderBinding.inflate(inflater, content)
    var onAction: (String) -> Unit = {}

    init {
        binding.actions.onAction = { onAction(it) }
    }

    override fun title(state: CustomerUiState) = string(R.string.provider_profile_title)

    override fun renderContent(state: CustomerUiState) {
        val empty = state.phase == CustomerPhase.Empty
        binding.unavailable.isVisible = empty
        listOf(binding.header, binding.stats, binding.aboutTitle, binding.aboutBody, binding.specialties, binding.bars, binding.reviewsTitle, binding.reviewList)
            .forEach { it.isVisible = !empty }
        val values = state.fieldValues
        binding.header.name = values.getOrElse(0) { "" }
        binding.header.rating = values.getOrElse(1) { "" }
        binding.header.services = values.getOrElse(2) { "" }
        binding.header.verifiedLabel = string(R.string.provider_verified).takeIf { state.providerVerified }
        binding.stats.items = listOf(
            StatItem(R.drawable.ic_star, values.getOrElse(1) { "" }, string(R.string.stat_rating)),
            StatItem(R.drawable.ic_orders, values.getOrElse(2) { "" }, string(R.string.stat_services)),
            StatItem(R.drawable.ic_calendar, values.getOrElse(3) { "" }, string(R.string.stat_years)),
        )
        binding.aboutBody.text = values.getOrElse(4) { "" }
        binding.aboutBody.isVisible = values.getOrElse(4) { "" }.isNotBlank()
        binding.specialties.removeAllViews()
        state.options.forEach { label ->
            binding.specialties.addView(
                noox.bzr.design.views.SelectableChipView(context).apply {
                    text = label
                    this.state = SelectionState.Disabled
                },
                LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
            )
        }
        binding.bars.max = 5
        binding.bars.items = listOf(
            string(R.string.rating_quality), string(R.string.rating_commitment), string(R.string.rating_communication),
        ).zip(state.itemDetails.mapNotNull(String::toDoubleOrNull))
        binding.reviewList.removeAllViews()
        state.items.forEach { row ->
            val fields = row.split('|')
            if (fields.size < REVIEW_FIELDS) return@forEach
            binding.reviewList.addView(
                noox.bzr.design.views.ReviewCardView(context).apply {
                    name = fields[0]
                    rating = fields[1].toIntOrNull() ?: 0
                    body = fields[2]
                    time = fields[3]
                    verified = ""
                },
                LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
            )
        }
        binding.actions.actions = state.visibleActions
    }

    private companion object { const val REVIEW_FIELDS = 4 }
}
