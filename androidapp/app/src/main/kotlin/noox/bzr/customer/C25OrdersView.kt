package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.design.views.OrderCardView
import noox.bzr.gallery.databinding.ScreenC25OrdersBinding

/** SCR-C25 (DEC-047): current / past orders. */
class C25OrdersView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC25OrdersBinding.inflate(inflater, content)
    var onTabSelect: (Int) -> Unit = {}
    var onOrderSelect: (Int) -> Unit = {}

    private data class Row(val index: Int, val title: String, val subtitle: String, val status: String)

    private val adapter = RowAdapter<Row, OrderCardView>(
        create = { parent -> OrderCardView(parent.context) },
        bind = { card, row, _ ->
            card.title = row.title
            card.subtitle = row.subtitle
            card.status = row.status
            card.onClick = { onOrderSelect(row.index) }
        },
        key = { it.index },
    )

    init {
        binding.tabs.title = null
        binding.tabs.labels = listOf(string(R.string.orders_current), string(R.string.orders_past))
        binding.tabs.onSelect = { onTabSelect(it) }
        binding.orders.rows(adapter, resources.getDimensionPixelSize(R.dimen.bremo_space_m))
    }

    override fun title(state: CustomerUiState) = string(R.string.orders_title)

    override fun renderContent(state: CustomerUiState) {
        binding.tabs.selectedIndex = state.selectedOptionIndex
        val past = state.selectedOptionIndex == 1
        binding.empty.isVisible = state.phase == CustomerPhase.Empty
        binding.empty.title = string(if (past) R.string.orders_past_empty_title else R.string.orders_current_empty_title)
        binding.empty.body = string(if (past) R.string.orders_past_empty_body else R.string.orders_current_empty_body)
        binding.orders.isVisible = state.items.isNotEmpty()
        adapter.submitList(
            state.items.indices.map { index ->
                Row(index, state.items[index], state.itemDetails.getOrElse(index) { "" }, state.itemStates.getOrElse(index) { "" })
            },
        )
        binding.loadingMore.isVisible = state.isBusy
    }
}
