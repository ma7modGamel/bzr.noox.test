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
        binding.terms.setOnClickListener { onTermsChange() }
        binding.publish.onClick = { onPublish() }
    }

    override fun title(state: CustomerUiState) = string(R.string.request_review_title)

    override fun renderContent(state: CustomerUiState) {
        binding.summary.rows = buildList {
            state.items.forEachIndexed { index, value -> add((if (index == 0) R.drawable.ic_home else R.drawable.ic_edit) to value) }
            state.itemDetails.forEachIndexed { index, value -> add((if (index == 0) R.drawable.ic_location else R.drawable.ic_calendar) to value) }
            state.itemStates.forEach { value -> add(R.drawable.ic_orders to value) }
        }
        binding.staffPricing.isVisible = !state.showPricing
        binding.terms.checked = state.termsError == null
        binding.termsRequired.isVisible = state.termsError != null
        // A refused publish shows the server's reason here (SCR-C05 §الأخطاء).
        binding.refused.isVisible = state.messageKey == "request.publish.error"
        binding.refused.text = state.fieldValues.firstOrNull().orEmpty().ifBlank { string(R.string.error_body) }
        binding.publish.state = if (state.isBusy) noox.bzr.design.ButtonVisualState.Loading else continueState(state)
    }
}
