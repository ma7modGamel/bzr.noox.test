package noox.bzr.provider

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import noox.bzr.customer.RowAdapter
import noox.bzr.customer.rows
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.R
import noox.bzr.design.views.AppBottomSheetView
import noox.bzr.design.views.OrderCardView
import noox.bzr.design.views.SecondaryButtonView
import noox.bzr.gallery.databinding.ScreenP11OffersBinding

class P11OffersView(context: Context) : ProviderScreenView(context) {
    private val binding = ScreenP11OffersBinding.inflate(inflater, content)
    private val sheet = AppBottomSheetView(context)
    var onOfferAction: (Int, String) -> Unit = { _, _ -> }
    var onConfirmWithdraw: () -> Unit = {}
    var onCancelWithdraw: () -> Unit = {}

    private data class Row(val index: Int, val title: String, val detail: String, val status: String, val actions: List<String>)
    private val adapter = RowAdapter<Row, OfferRowView>(
        create = { OfferRowView(it.context) },
        bind = { view, row, _ -> view.bind(row) },
        key = Row::index,
    )

    init {
        binding.offers.rows(adapter, resources.getDimensionPixelSize(R.dimen.bremo_space_m))
        sheet.title = string(R.string.provider_offers_withdraw_title)
        sheet.body = string(R.string.provider_offers_withdraw_body)
        sheet.action = string(R.string.provider_offers_withdraw_action)
        sheet.secondaryAction = string(R.string.common_cancel)
        sheet.onAction = { onConfirmWithdraw() }
        sheet.onSecondary = { onCancelWithdraw() }
        sheet.isVisible = false
        overlay.addView(sheet)
    }

    override fun title() = string(R.string.provider_offers_title)

    override fun renderContent(state: ProviderUiState) {
        binding.empty.isVisible = state.phase == ProviderPhase.Empty
        binding.empty.title = string(R.string.provider_offers_empty_title)
        binding.empty.body = string(R.string.provider_offers_empty_body)
        binding.offers.isVisible = state.items.isNotEmpty()
        adapter.submitList(state.items.indices.map { index ->
            Row(
                index, state.items[index], state.itemDetails.getOrElse(index) { "" },
                state.itemStates.getOrElse(index) { "" },
                state.secondaryOptions.getOrElse(index) { "" }.split(',').filter(String::isNotBlank),
            )
        })
        binding.message.isVisible = state.messageKey != null
        binding.message.text = string(
            if (state.messageKey == "provider.offers.withdraw.success") R.string.provider_offers_withdraw_success
            else R.string.provider_offers_withdraw_changed,
        )
        sheet.isVisible = state.currentAction == "withdraw_offer"
        sheet.actionState = if (state.isBusy) ButtonVisualState.Loading else ButtonVisualState.Normal
    }

    private inner class OfferRowView(context: Context) : LinearLayout(context) {
        private val card = OrderCardView(context)
        private val action = SecondaryButtonView(context)

        init {
            orientation = VERTICAL
            addView(card, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(action, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        fun bind(row: Row) {
            val title = row.title.split('|')
            val detail = row.detail.split('|')
            card.title = if (title.size >= 2) context.getString(R.string.provider_offers_card_title, title[0], title[1]) else row.title
            card.subtitle = if (detail.size >= 3) context.getString(
                R.string.provider_offers_card_detail, detail[0], detail[1], detail[2],
            ) else row.detail
            card.status = row.status
            val selected = when {
                "withdraw_offer" in row.actions -> "withdraw_offer"
                "open_available_request" in row.actions -> "open_available_request"
                "open_assigned_order" in row.actions -> "open_assigned_order"
                else -> null
            }
            action.isVisible = selected != null
            action.text = string(
                when (selected) {
                    "withdraw_offer" -> R.string.action_withdraw_offer
                    "open_assigned_order" -> R.string.action_open_assigned_order
                    else -> R.string.action_open_available_request
                },
            )
            action.onClick = { selected?.let { onOfferAction(row.index, it) } }
            card.onClick = { row.actions.firstOrNull()?.let { onOfferAction(row.index, it) } }
        }
    }
}
