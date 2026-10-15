package noox.bzr.customer

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.design.views.cardShadow
import noox.bzr.design.views.sizeCompoundIcons
import noox.bzr.gallery.databinding.ItemReviewCardBinding
import noox.bzr.gallery.databinding.ScreenC05ReviewBinding

/**
 * SCR-C05, step 5 of 5 (DEC-071): one card per part of the draft, each with «تعديل» back to its step, then terms and
 * publish. Rows come from the draft (DEC-059): `items` = service, description…; `itemDetails` = address, time,
 * materials; `itemStates` = pricing rows (marketplace only).
 */
class C05ReviewView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC05ReviewBinding.inflate(inflater, content)
    var onEdit: (String) -> Unit = {}
    var onTermsChange: () -> Unit = {}
    var onPublish: () -> Unit = {}

    init {
        pinAction(binding.publish)
        pinAction(binding.secure)
        binding.secure.sizeCompoundIcons(R.dimen.bremo_size_icon_small)
        addRequestProgress(5, string(R.string.request_review_final))
        binding.terms.setOnClickListener { onTermsChange() }
        binding.publish.onClick = { onPublish() }
    }

    override fun title(state: CustomerUiState) = string(R.string.request_review_title)

    override fun renderContent(state: CustomerUiState) {
        binding.cards.removeAllViews()
        val (category, problem) = state.items.firstOrNull().orEmpty().split(" — ", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        card(R.drawable.ic_wrench, category, problem, extra = "", step = "SCR-C03")
        val details = state.items.drop(1)
        if (details.isNotEmpty()) card(R.drawable.ic_orders, string(R.string.request_review_details), details.joinToString("\n"), "", "SCR-C37")
        state.itemDetails.getOrNull(0)?.let { address ->
            card(R.drawable.ic_location, address.substringBefore(" · "), address.substringAfter(" · ", ""), "", "SCR-C04")
        }
        state.itemDetails.getOrNull(1)?.let { time -> card(R.drawable.ic_bolt, string(R.string.request_review_visit), time, "", "SCR-C04") }
        val pricing = state.itemStates
        val materials = state.itemDetails.getOrNull(2).orEmpty()
        if (pricing.isNotEmpty() || materials.isNotEmpty()) {
            card(
                R.drawable.ic_payments,
                pricing.lastOrNull().takeIf { pricing.size > 1 } ?: string(R.string.request_budget_title),
                pricing.firstOrNull().orEmpty(),
                materials,
                if (state.requestSteps == 4) "SCR-C04" else "SCR-C38",
            )
        }
        binding.staffPricing.isVisible = !state.showPricing
        binding.terms.checked = state.termsError == null
        binding.termsRequired.isVisible = state.termsError != null
        // A refused publish shows the server's reason here (SCR-C05 §الأخطاء).
        binding.refused.isVisible = state.messageKey == "request.publish.error"
        binding.refused.text = state.fieldValues.firstOrNull().orEmpty().ifBlank { string(R.string.error_body) }
        binding.publish.state = if (state.isBusy) noox.bzr.design.ButtonVisualState.Loading else continueState(state)
    }

    private fun card(@DrawableRes icon: Int, title: String, body: String, extra: String, step: String) {
        val card = ItemReviewCardBinding.inflate(inflater, binding.cards, true)
        card.root.cardShadow()
        card.icon.setImageResource(icon)
        card.title.text = title
        card.body.text = body
        card.body.isVisible = body.isNotBlank()
        card.extra.text = extra
        card.extra.isVisible = extra.isNotBlank()
        card.edit.contentDescription = "${string(R.string.common_edit)} $title"
        card.edit.setOnClickListener { onEdit(step) }
    }
}
