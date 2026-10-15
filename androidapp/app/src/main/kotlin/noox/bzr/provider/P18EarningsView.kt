package noox.bzr.provider

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.design.StatItem
import noox.bzr.design.views.SummaryCardView
import noox.bzr.gallery.databinding.ScreenP18EarningsBinding

class P18EarningsView(context: Context) : ProviderScreenView(context) {
    private val binding = ScreenP18EarningsBinding.inflate(inflater, content)

    var onType: (Int) -> Unit = {}
    var onFrom: (String) -> Unit = {}
    var onTo: (String) -> Unit = {}
    var onApply: () -> Unit = {}
    var onReset: () -> Unit = {}
    var onExport: () -> Unit = {}
    var onMore: () -> Unit = {}
    init {
        binding.filterTypes.maxColumns = 2
        binding.dateFrom.onValueChange = { onFrom(it) }
        binding.dateTo.onValueChange = { onTo(it) }
        binding.applyFilters.onClick = { onApply() }
        binding.resetFilters.onClick = { onReset() }
        binding.exportStatement.onClick = { onExport() }
        binding.loadMore.onClick = { onMore() }
    }

    override fun title() = string(R.string.provider_earnings_title)

    override fun renderContent(state: ProviderUiState) {
        binding.filterTypes.removeAllViews()
        state.earningsFilterLabels.forEachIndexed { index, label ->
            binding.filterTypes.addView(noox.bzr.design.views.SelectableChipView(context).apply {
                text = label
                this.state = if (state.isBusy) noox.bzr.design.SelectionState.Disabled else if (index == state.earningsTypeIndex) noox.bzr.design.SelectionState.Selected else noox.bzr.design.SelectionState.Unselected
                setOnClickListener { onType(index) }
            })
        }
        binding.dateFrom.findViewById<android.widget.EditText>(R.id.edit).isEnabled = !state.isBusy
        binding.dateTo.findViewById<android.widget.EditText>(R.id.edit).isEnabled = !state.isBusy
        binding.dateFrom.value = state.earningsDateFrom
        binding.dateTo.value = state.earningsDateTo
        binding.applyFilters.state = if (state.isBusy) noox.bzr.design.ButtonVisualState.Loading else noox.bzr.design.ButtonVisualState.Normal
        binding.resetFilters.enabled = !state.isBusy
        binding.exportStatement.enabled = !state.isBusy && !state.earningsFiltersDirty
        binding.loadMore.isVisible = state.earningsHasMore
        binding.loadMore.enabled = !state.isBusy && !state.earningsFiltersDirty
        binding.filterError.isVisible = state.messageKey in listOf("provider.earnings.filter_error", "provider.earnings.export_error")
        binding.filterError.text = string(if (state.messageKey == "provider.earnings.export_error") R.string.provider_earnings_export_error else R.string.provider_earnings_filter_error)
        binding.summary.items = listOf(
            StatItem(R.drawable.ic_orders, state.secondaryOptions.getOrElse(0) { "" }, string(R.string.provider_earnings_balance)),
            StatItem(R.drawable.ic_check, state.secondaryOptions.getOrElse(1) { "" }, string(R.string.provider_earnings_available)),
            StatItem(R.drawable.ic_clock, state.secondaryOptions.getOrElse(2) { "" }, string(R.string.provider_earnings_pending)),
        )
        // DEC-068: new offers paused by dues (BR-063); debt and limit follow the three summary values.
        binding.duesBlocked.isVisible = state.earningsDuesBlocked || state.messageKey == "provider.earnings.dues_blocked"
        if (binding.duesBlocked.isVisible) {
            binding.duesBlocked.text = context.getString(
                R.string.provider_earnings_dues_blocked,
                state.secondaryOptions.getOrElse(3) { "" },
                state.secondaryOptions.getOrElse(4) { "" },
            )
        }
        binding.employeeInfo.isVisible = state.earningsEmployee || state.messageKey == "provider.earnings.employee.info"
        binding.empty.isVisible = state.phase == ProviderPhase.Empty
        binding.empty.title = string(R.string.provider_earnings_empty)
        binding.empty.body = ""
        binding.transactions.isVisible = state.items.isNotEmpty()
        binding.transactions.replace(
            state.items.mapIndexed { index, title ->
                SummaryCardView(context).apply {
                    this.title = title
                    editText = null
                    rows = listOf(
                        transactionIcon(state.itemStates.getOrElse(index) { "" }) to
                            state.itemDetails.getOrElse(index) { "" },
                    )
                }
            },
        )
        binding.loadingMore.isVisible = state.isBusy
    }

    private fun transactionIcon(type: String) = when (type) {
        "PAYOUT", "REMITTANCE" -> R.drawable.ic_check
        else -> R.drawable.ic_orders
    }
}
