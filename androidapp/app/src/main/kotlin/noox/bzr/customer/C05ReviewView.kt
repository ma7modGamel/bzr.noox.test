package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenC05ReviewBinding

/** SCR-C05 (DEC-047): request summary, terms, publish. */
class C05ReviewView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC05ReviewBinding.inflate(inflater, content)
    var onTermsChange: () -> Unit = {}
    var onPublish: () -> Unit = {}

    init {
        binding.summary.rows = listOf(
            R.drawable.ic_home to string(R.string.request_review_service),
            R.drawable.ic_edit to string(R.string.request_review_description),
            R.drawable.ic_location to string(R.string.request_address_value),
            R.drawable.ic_calendar to string(R.string.request_slot_value),
        )
        binding.terms.setOnClickListener { onTermsChange() }
        binding.publish.onClick = { onPublish() }
    }

    override fun title(state: CustomerUiState) = string(R.string.request_review_title)

    override fun renderContent(state: CustomerUiState) {
        binding.staffPricing.isVisible = !state.showPricing
        binding.terms.checked = state.termsError == null
        binding.termsRequired.isVisible = state.termsError != null
        binding.publish.state = continueState(state)
    }
}
