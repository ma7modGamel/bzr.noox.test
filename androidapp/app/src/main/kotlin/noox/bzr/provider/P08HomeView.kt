package noox.bzr.provider

import noox.bzr.design.views.SelectableTileView
import android.content.Context
import android.os.Build
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import noox.bzr.customer.RowAdapter
import noox.bzr.customer.rows
import noox.bzr.design.R
import noox.bzr.design.views.OrderCardView
import noox.bzr.gallery.databinding.ScreenP08HomeBinding

class P08HomeView(context: Context) : ProviderScreenView(context) {
    private val binding = ScreenP08HomeBinding.inflate(inflater, content)
    var onAvailabilityChange: (Boolean) -> Unit = {}
    var onAction: (String) -> Unit = {}
    var onRequest: (Int) -> Unit = {}

    private data class MarketRow(val index: Int, val title: String, val detail: String)
    private val marketAdapter = RowAdapter<MarketRow, OrderCardView>(
        create = { OrderCardView(it.context) },
        bind = { card, row, _ ->
            card.title = row.title
            card.subtitle = row.detail
            card.status = string(R.string.order_status_provider_open)
            card.onClick = { onRequest(row.index) }
        },
        key = MarketRow::index,
    )

    init {
        binding.availability.setOnClickListener { if (canToggle) onAvailabilityChange(!available) }
        binding.availabilityIcon.setIcon(R.drawable.ic_check)
        binding.order.onClick = { onAction("open_assigned_order") }
        binding.openOrder.onClick = { onAction("open_assigned_order") }
        binding.myOffers.onClick = { onAction("open_my_offers") }
        binding.marketRequests.rows(marketAdapter, resources.getDimensionPixelSize(R.dimen.bremo_space_m))
    }

    private var available = false
    private var canToggle = false

    override fun title() = string(R.string.provider_home_title)

    /** DEC-063: on = the brand gradient card with white text; off = a white card. */
    private fun renderAvailability(state: ProviderUiState) {
        available = state.availableNow
        canToggle = state.canContinue
        val onBrand = androidx.core.content.ContextCompat.getColor(context, R.color.bremo_on_primary)
        binding.availability.setBackgroundResource(if (available) R.drawable.bremo_bg_hero else R.drawable.bremo_bg_menu_group)
        binding.availabilityWell.setBackgroundResource(if (available) R.drawable.bremo_bg_hero_well else R.drawable.bremo_bg_icon_well)
        if (available) {
            binding.availabilityIcon.setGradient(onBrand, onBrand)
        } else {
            binding.availabilityIcon.setGradient(
                androidx.core.content.ContextCompat.getColor(context, R.color.bremo_gradient_icon_brand_from),
                androidx.core.content.ContextCompat.getColor(context, R.color.bremo_gradient_icon_brand_to),
            )
        }
        binding.availabilityTitle.setTextColor(if (available) onBrand else androidx.core.content.ContextCompat.getColor(context, R.color.bremo_navy900))
        binding.availabilityBody.setTextColor(
            androidx.core.content.ContextCompat.getColor(context, if (available) R.color.bremo_on_hero_body else R.color.bremo_slate500),
        )
        binding.availabilityState.text = string(if (available) R.string.provider_home_available_on_state else R.string.provider_home_available_off_state)
        binding.availabilityState.setTextColor(if (available) androidx.core.content.ContextCompat.getColor(context, R.color.bremo_primary700) else onBrand)
        binding.availabilityState.setBackgroundResource(if (available) R.drawable.bremo_bg_nav_pill else R.drawable.bremo_bg_button_primary)
    }

    /** Shortcuts the state allows, as gradient-icon tiles (offers stay with the market list). */
    private fun renderShortcuts(state: ProviderUiState) {
        val shortcuts = listOf(
            Triple("open_notifications", R.drawable.ic_bell, if (state.unreadCount > 0) {
                context.getString(R.string.action_open_notifications_count, state.unreadCount)
            } else {
                string(R.string.action_open_notifications)
            }),
            Triple("open_messages", R.drawable.ic_messages, string(R.string.action_open_messages)),
            Triple("open_earnings", R.drawable.ic_orders, string(R.string.action_open_earnings)),
            Triple("open_provider_profile", R.drawable.ic_account, string(R.string.action_open_provider_profile)),
        ).filter { it.first in state.visibleActions }
        binding.shortcutsTitle.isVisible = shortcuts.isNotEmpty()
        binding.shortcuts.isVisible = shortcuts.isNotEmpty()
        binding.shortcuts.removeAllViews()
        shortcuts.forEach { (action, icon, label) ->
            binding.shortcuts.addView(
                SelectableTileView(context).apply {
                    this.icon = icon
                    text = label
                    setOnClickListener { onAction(action) }
                },
            )
        }
    }

    override fun renderContent(state: ProviderUiState) {
        renderAvailability(state)
        renderShortcuts(state)
        binding.availability.isClickable = state.canContinue
        binding.availability.alpha = if (state.canContinue) {
            1f
        } else {
            ResourcesCompat.getFloat(resources, R.dimen.bremo_opacity_disabled)
        }
        binding.availability.contentDescription = string(R.string.provider_home_available_title)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            binding.availability.stateDescription = string(
                if (state.availableNow) R.string.provider_home_available_on_state
                else R.string.provider_home_available_off_state,
            )
        }
        binding.saved.isVisible = state.messageKey == "provider.home.availability.saved"

        val hasOrder = state.activeOrderId != null && state.items.size >= 4
        binding.order.isVisible = hasOrder
        binding.empty.isVisible = !hasOrder
        binding.empty.title = string(R.string.provider_home_no_assignment_title)
        binding.empty.body = string(R.string.provider_home_no_assignment_body)
        binding.openOrder.isVisible = hasOrder && "open_assigned_order" in state.visibleActions
        val marketplace = "open_my_offers" in state.visibleActions
        binding.marketTitle.isVisible = marketplace
        binding.marketRequests.isVisible = marketplace && state.options.isNotEmpty()
        binding.marketEmpty.isVisible = marketplace && state.options.isEmpty()
        binding.marketEmpty.title = string(R.string.provider_market_requests_empty)
        binding.marketEmpty.body = ""
        binding.myOffers.isVisible = marketplace
        marketAdapter.submitList(state.options.indices.map { index ->
            MarketRow(index, state.options[index], state.secondaryOptions.getOrElse(index) { "" })
        })

        if (hasOrder) {
            binding.order.title = context.getString(R.string.provider_home_order_number, state.items[0])
            val timing = if (state.items[3] == "provider.home.timing.now") {
                string(R.string.provider_home_timing_now)
            } else {
                state.items[3]
            }
            binding.order.subtitle = state.items[1] + "\n" + listOf(state.items[2], timing)
                .filter(String::isNotBlank)
                .joinToString(" — ")
            binding.order.status = providerDisplayStatus(state.displayStatus)
        }
    }
}
