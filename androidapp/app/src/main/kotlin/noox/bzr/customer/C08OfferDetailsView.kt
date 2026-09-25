package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenC08OfferDetailsBinding

/** SCR-C08 (DEC-047): offer details. */
class C08OfferDetailsView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC08OfferDetailsBinding.inflate(inflater, content)
    var onAction: (String) -> Unit = {}

    init {
        binding.header.rating = "4.9"
        binding.header.services = "86"
        binding.eta.apply {
            value = "25"; unit = string(R.string.unit_minute); title = string(R.string.eta_title); subtitle = string(R.string.eta_subtitle)
        }
        binding.actions.onAction = { onAction(it) }
    }

    override fun title(state: CustomerUiState) = string(R.string.offer_details_title)

    override fun renderContent(state: CustomerUiState) {
        binding.details.rows = listOf(
            R.drawable.ic_orders to string(R.string.offer_price),
            R.drawable.ic_clock to if (state.showEta) "25 " + string(R.string.unit_minute) else string(R.string.offer_appointment),
            R.drawable.ic_shield to string(R.string.offer_inspection_deductible),
        )
        binding.eta.isVisible = state.showEta
        binding.actions.actions = state.visibleActions
    }
}
