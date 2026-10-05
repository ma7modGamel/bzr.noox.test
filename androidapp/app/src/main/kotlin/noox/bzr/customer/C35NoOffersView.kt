package noox.bzr.customer

import android.content.Context
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenC35NoOffersBinding

/** SCR-C35 (DEC-047): no offers — edit or republish. */
class C35NoOffersView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC35NoOffersBinding.inflate(inflater, content)
    var onAction: (String) -> Unit = {}

    init {
        binding.actions.onAction = { onAction(it) }
    }

    override fun title(state: CustomerUiState) = string(R.string.no_offers_title)

    override fun renderContent(state: CustomerUiState) {
        binding.actions.actions = state.visibleActions
    }
}
