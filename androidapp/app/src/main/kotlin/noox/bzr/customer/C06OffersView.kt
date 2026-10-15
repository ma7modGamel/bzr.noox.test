package noox.bzr.customer

import android.content.Context
import android.widget.LinearLayout
import androidx.core.view.isVisible
import noox.bzr.design.BzrFormat
import noox.bzr.design.R
import noox.bzr.design.views.OfferCardView
import noox.bzr.design.views.sizeCompoundIcons

/**
 * SCR-C06 — Figma «Primo App» offers 3002:446 (DEC-073): count and request pill, headline and payment note, sort
 * chips, then one card per offer. Rows: id|name|rating|reviews|amount|includes|eta or slot|materials|note|verified|
 * provider id|subtitle.
 */
class C06OffersView(context: Context) : CustomerScreenView(context) {
    private val binding = noox.bzr.gallery.databinding.ScreenC06OffersBinding.inflate(inflater, content)
    private val waitingInfo = noox.bzr.design.views.InfoBannerView(context)
    var onAction: (String) -> Unit = {}
    var onSort: (Int) -> Unit = {}
    var onSelect: (Int) -> Unit = {}
    var onProvider: (Int) -> Unit = {}


    init {
        content.addView(waitingInfo, 0)
        binding.sort.onSelect = { index -> onSort(index) }
        binding.actions.onAction = { onAction(it) }
        binding.requestPill.sizeCompoundIcons(R.dimen.bremo_size_icon_xs)
        binding.paymentNote.sizeCompoundIcons(R.dimen.bremo_size_icon_small)
    }

    override fun title(state: CustomerUiState) = string(R.string.offers_title)

    override fun renderContent(state: CustomerUiState) {
        waitingInfo.isVisible = state.waitingInfo.isNotBlank()
        waitingInfo.text = state.waitingInfo
        val empty = state.phase == CustomerPhase.Empty
        binding.empty.isVisible = empty
        listOf(binding.header, binding.sort, binding.offerList).forEach { it.isVisible = !empty }
        binding.count.text = BzrFormat.quantity(context, R.plurals.plural_offers_available, state.items.size)
        val problem = state.itemDetails.firstOrNull().orEmpty()
        binding.requestPill.text = BzrFormat.fill(string(R.string.offers_request), "title" to problem)
        binding.requestPill.isVisible = problem.isNotEmpty()
        renderSort(state)
        binding.offerList.removeAllViews()
        state.items.forEachIndexed { index, row ->
            val values = row.split('|')
            if (values.size < OFFER_FIELDS) return@forEachIndexed
            binding.offerList.addView(card(state, values, index), LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
        binding.actions.actions = if (empty) state.visibleActions else state.visibleActions.filter { it != "accept_offer" }
    }

    private fun renderSort(state: CustomerUiState) {
        binding.sort.isVisible = state.options.isNotEmpty()
        binding.sort.labels = state.options
        binding.sort.icons = emptyList()
        binding.sort.selectedIndex = state.selectedIndex
    }

    private fun card(state: CustomerUiState, values: List<String>, index: Int) = OfferCardView(context).apply {
        provider = values[1]
        subtitle = values[11]
        rating = values[2]
        reviews = values[3].takeIf(String::isNotEmpty)?.let { BzrFormat.fill(string(R.string.offer_reviews), "count" to it) }.orEmpty()
        price = BzrFormat.fill(string(R.string.customer_home_offers_amount), "amount" to values[4])
        priceCaption = values[5].ifEmpty { if (state.pricingOptions.firstOrNull() == "inspection") string(R.string.offer_inspection_fee) else "" }
        detail = values[6].toIntOrNull()?.let { minutes ->
            if (minutes >= MINUTES_PER_HOUR && minutes % MINUTES_PER_HOUR == 0) {
                BzrFormat.quantity(context, R.plurals.plural_offer_eta_hours, minutes / MINUTES_PER_HOUR)
            } else {
                BzrFormat.quantity(context, R.plurals.plural_offer_eta, minutes)
            }
        } ?: values[6]
        materials = values[7]
        note = values[8].takeIf(String::isNotEmpty)?.let { "“$it”" }.orEmpty()
        verified = values[9] == "true"
        availableNow = values.getOrNull(12) == "true"
        verifiedLabel = string(R.string.offer_verified)
        button = string(R.string.offer_select)
        profileLabel = string(R.string.offer_profile)
        chatLabel = string(R.string.button_chat)
        showSelect = "accept_offer" in state.visibleActions
        showChat = "chat" in state.visibleActions
        onOpen = { onProvider(index) }
        onSelect = { this@C06OffersView.onSelect(index) }
        onChat = { onAction("chat") }
    }

    private companion object {
        const val OFFER_FIELDS = 12
        const val MINUTES_PER_HOUR = 60
    }
}
