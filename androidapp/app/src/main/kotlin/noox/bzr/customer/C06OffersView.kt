package noox.bzr.customer

import android.content.Context
import android.widget.LinearLayout
import androidx.core.view.isVisible
import noox.bzr.design.OfferVariant
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenC06OffersBinding

/** SCR-C06 (DEC-047): offers with countdown and sorting. */
class C06OffersView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC06OffersBinding.inflate(inflater, content)
    var onAction: (String) -> Unit = {}
    var onSort: (Int) -> Unit = {}
    var onSelect: (Int) -> Unit = {}
    var onProvider: (Int) -> Unit = {}

    init {
        binding.sort.title = string(R.string.sort_title)
        binding.sort.labels = listOf(string(R.string.sort_top_rated), string(R.string.sort_lowest_price), string(R.string.sort_fastest))
        binding.sort.onSelect = { onSort(it) }
        binding.actions.onAction = { onAction(it) }
    }

    override fun title(state: CustomerUiState) = string(R.string.offers_title)

    override fun renderContent(state: CustomerUiState) {
        val empty = state.phase == CustomerPhase.Empty
        binding.empty.isVisible = empty
        listOf(binding.count, binding.countdown, binding.sort, binding.offerList).forEach { it.isVisible = !empty }
        binding.count.text = state.fieldValues.firstOrNull().orEmpty()
        binding.countdown.seconds = state.countdownSeconds
        binding.sort.selectedIndex = state.selectedIndex
        binding.sort.labels = if (state.showEta) {
            listOf(string(R.string.sort_top_rated), string(R.string.sort_lowest_price), string(R.string.sort_fastest))
        } else {
            listOf(string(R.string.sort_top_rated), string(R.string.sort_lowest_price))
        }
        binding.offerList.removeAllViews()
        state.items.forEachIndexed { index, row ->
            val values = row.split('|')
            if (values.size < OFFER_FIELDS) return@forEachIndexed
            binding.offerList.addView(
                noox.bzr.design.views.OfferCardView(context).apply {
                    provider = values[1]
                    rating = values[2]
                    services = values[3]
                    price = values[4]
                    detail = if (state.showEta && values[5].toIntOrNull() != null) {
                        "${values[5]} ${string(R.string.unit_minute)}"
                    } else {
                        values[5]
                    }
                    badge = values[6]
                    button = string(R.string.offer_select)
                    chatLabel = string(R.string.action_chat)
                    variant = when {
                        !state.showEta -> OfferVariant.Scheduled
                        state.pricingOptions.firstOrNull() == "inspection" -> OfferVariant.Inspection
                        else -> OfferVariant.Execution
                    }
                    priceCaption = if (variant == OfferVariant.Inspection) string(R.string.offer_inspection_fee) else null
                    showSelect = "accept_offer" in state.visibleActions
                    showChat = "chat" in state.visibleActions
                    onOpen = { onProvider(index) }
                    onSelect = { this@C06OffersView.onSelect(index) }
                    onChat = { onAction("chat") }
                },
                LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
            )
        }
        binding.actions.actions = if (empty) state.visibleActions else state.visibleActions.filter { it != "accept_offer" }
    }

    private companion object {
        const val OFFER_FIELDS = 8
    }
}
