package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.OfferVariant
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenC06OffersBinding

/** SCR-C06 (DEC-047): offers with countdown and sorting. */
class C06OffersView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC06OffersBinding.inflate(inflater, content)
    var onAction: (String) -> Unit = {}

    init {
        binding.countdown.seconds = OFFERS_COUNTDOWN_SECONDS
        binding.sort.title = string(R.string.sort_title)
        binding.sort.labels = listOf(string(R.string.sort_top_rated), string(R.string.sort_lowest_price), string(R.string.sort_fastest))
        binding.sort.selectedIndex = 0
        binding.execution.apply {
            provider = string(R.string.offer_provider); rating = "4.9"; services = "86"; price = string(R.string.offer_price)
            detail = "25 " + string(R.string.unit_minute); badge = string(R.string.badge_top_rated)
            button = string(R.string.offer_select); chatLabel = string(R.string.action_chat); variant = OfferVariant.Execution
        }
        binding.inspection.apply {
            provider = string(R.string.drawer_name); rating = "4.8"; services = "54"; price = string(R.string.offer_inspection_price)
            detail = string(R.string.offer_scheduled); badge = string(R.string.badge_best_price)
            button = string(R.string.offer_select); chatLabel = string(R.string.action_chat); variant = OfferVariant.Inspection
            priceCaption = string(R.string.offer_inspection_fee)
        }
        binding.actions.onAction = { onAction(it) }
    }

    override fun title(state: CustomerUiState) = string(R.string.offers_title)

    override fun renderContent(state: CustomerUiState) {
        val empty = state.phase == CustomerPhase.Empty
        binding.empty.isVisible = empty
        listOf(binding.count, binding.countdown, binding.sort, binding.execution, binding.inspection).forEach { it.isVisible = !empty }
        binding.actions.actions = if (empty) state.visibleActions else state.visibleActions.filter { it != "accept_offer" }
    }

    private companion object {
        const val OFFERS_COUNTDOWN_SECONDS = 1320
    }
}
