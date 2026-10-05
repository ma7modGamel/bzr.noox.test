package noox.bzr.customer

import android.content.Context
import android.widget.LinearLayout
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenC08OfferDetailsBinding

/** SCR-C08 (DEC-047): offer details. */
class C08OfferDetailsView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC08OfferDetailsBinding.inflate(inflater, content)
    var onAction: (String) -> Unit = {}
    var onPaymentSelect: (Int) -> Unit = {}

    init {
        pinAction(binding.actions)
        binding.actions.onAction = { onAction(it) }
    }

    override fun title(state: CustomerUiState) = string(R.string.offer_details_title)

    override fun renderContent(state: CustomerUiState) {
        binding.header.name = state.fieldValues.getOrElse(0) { "" }
        binding.header.rating = state.fieldValues.getOrElse(1) { "" }
        binding.header.services = state.fieldValues.getOrElse(2) { "" }
        binding.header.verifiedLabel = string(R.string.provider_verified).takeIf { state.providerVerified }
        binding.details.rows = state.items.mapIndexed { index, value ->
            listOf(R.drawable.ic_orders, R.drawable.ic_clock, R.drawable.ic_shield).getOrElse(index) { R.drawable.ic_info } to value
        } + state.itemDetails.mapIndexed { index, value ->
            (if (index == 0) R.drawable.ic_home else R.drawable.ic_location) to value
        }
        binding.eta.isVisible = state.showEta
        binding.eta.value = state.fieldValues.getOrElse(3) { "" }
        binding.eta.unit = string(R.string.unit_minute)
        binding.eta.title = string(R.string.eta_title)
        binding.eta.subtitle = string(R.string.eta_subtitle)
        binding.paymentMethods.removeAllViews()
        state.options.forEachIndexed { index, label ->
            binding.paymentMethods.addView(
                noox.bzr.design.views.RadioCardView(context).apply {
                    title = label
                    body = ""
                    selected = index == state.selectedOptionIndex
                    setOnClickListener { onPaymentSelect(index) }
                },
                LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
            )
        }
        binding.actions.actions = state.visibleActions
    }
}
