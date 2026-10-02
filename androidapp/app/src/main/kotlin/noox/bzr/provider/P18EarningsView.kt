package noox.bzr.provider

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.design.StatItem
import noox.bzr.design.views.SummaryCardView
import noox.bzr.gallery.databinding.ScreenP18EarningsBinding

class P18EarningsView(context: Context) : ProviderScreenView(context) {
    private val binding = ScreenP18EarningsBinding.inflate(inflater, content)

    override fun title() = string(R.string.provider_earnings_title)

    override fun renderContent(state: ProviderUiState) {
        binding.summary.items = listOf(
            StatItem(R.drawable.ic_orders, state.secondaryOptions.getOrElse(0) { "" }, string(R.string.provider_earnings_balance)),
            StatItem(R.drawable.ic_check, state.secondaryOptions.getOrElse(1) { "" }, string(R.string.provider_earnings_available)),
            StatItem(R.drawable.ic_clock, state.secondaryOptions.getOrElse(2) { "" }, string(R.string.provider_earnings_pending)),
        )
        binding.employeeInfo.isVisible = state.messageKey == "provider.earnings.employee.info"
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
