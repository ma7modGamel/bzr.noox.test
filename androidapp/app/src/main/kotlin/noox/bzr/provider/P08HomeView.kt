package noox.bzr.provider

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
        binding.availability.setOnClickListener {
            if (binding.availability.isEnabled) onAvailabilityChange(!binding.availability.checked)
        }
        binding.order.onClick = { onAction("open_assigned_order") }
        binding.openOrder.onClick = { onAction("open_assigned_order") }
        binding.notifications.onClick = { onAction("open_notifications") }
        binding.messages.onClick = { onAction("open_messages") }
        binding.earnings.onClick = { onAction("open_earnings") }
        binding.profile.onClick = { onAction("open_provider_profile") }
        binding.myOffers.onClick = { onAction("open_my_offers") }
        binding.marketRequests.rows(marketAdapter, resources.getDimensionPixelSize(R.dimen.bremo_space_m))
    }

    override fun title() = string(R.string.provider_home_title)

    override fun renderContent(state: ProviderUiState) {
        binding.availability.checked = state.availableNow
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
        binding.notifications.isVisible = "open_notifications" in state.visibleActions
        binding.notifications.text = if (state.unreadCount > 0) {
            context.getString(R.string.action_open_notifications_count, state.unreadCount)
        } else {
            string(R.string.action_open_notifications)
        }
        binding.messages.isVisible = "open_messages" in state.visibleActions
        binding.earnings.isVisible = "open_earnings" in state.visibleActions
        binding.profile.isVisible = "open_provider_profile" in state.visibleActions
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
