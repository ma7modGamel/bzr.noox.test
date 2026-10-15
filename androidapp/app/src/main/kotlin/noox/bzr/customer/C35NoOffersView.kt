package noox.bzr.customer

import android.content.Context
import android.content.res.ColorStateList
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import noox.bzr.design.BzrFormat
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.R
import noox.bzr.design.generated.CategoryIcons
import noox.bzr.design.views.cardShadow
import noox.bzr.design.views.sizeCompoundIcons

/**
 * SCR-C35 — Figma «Primo App» «طلبك اتنشر» 3002:1519 (DEC-073): the open request while no offer has arrived. Before the
 * offer window closes (edit allowed) it reads as published; once only republish is left it says no offers came.
 * Buttons come from `available_actions`; «متابعة حالة الطلب» reloads the order (and so its offers).
 */
class C35NoOffersView(context: Context) : CustomerScreenView(context) {
    private val binding = noox.bzr.gallery.databinding.ScreenC35NoOffersBinding.inflate(inflater, content)
    var onAction: (String) -> Unit = {}

    init {
        binding.orderCard.cardShadow()
        binding.edit.onClick = { onAction("edit_request") }
        binding.cancel.onClick = { onAction("cancel") }
        binding.waitingStatus.sizeCompoundIcons(R.dimen.bremo_size_status_dot)
    }

    override fun title(state: CustomerUiState) = string(R.string.waiting_title)

    override fun renderContent(state: CustomerUiState) {
        val waiting = "republish" !in state.visibleActions
        binding.heading.text = string(if (waiting) R.string.waiting_heading else R.string.offers_empty_title)
        binding.body.text = string(if (waiting) R.string.waiting_body else R.string.offers_empty_body)
        binding.waitingInfo.isVisible = state.waitingInfo.isNotBlank()
        binding.waitingInfo.text = state.waitingInfo
        val summary = state.itemDetails
        binding.kind.text = string(if (state.showEta) R.string.waiting_kind_now else R.string.waiting_kind_scheduled)
        binding.orderTitle.text = summary.getOrElse(0) { "" }
        val icon = state.optionIcons.firstOrNull().orEmpty()
        binding.orderIcon.setImageResource(if (icon.isEmpty()) R.drawable.ic_wrench else CategoryIcons.drawable(icon))
        ImageViewCompat.setImageTintList(binding.orderIcon, ColorStateList.valueOf(context.getColor(R.color.bremo_primary600)))
        binding.infoRows.removeAllViews()
        summary.getOrNull(1)?.takeIf(String::isNotEmpty)?.let { info(R.drawable.ic_location, it) }
        val time = if (state.showEta) string(R.string.provider_home_timing_now) else summary.getOrElse(2) { "" }
        if (time.isNotEmpty()) info(R.drawable.ic_clock, time)
        summary.getOrNull(3)?.takeIf(String::isNotEmpty)?.let { info(R.drawable.ic_payments, BzrFormat.fill(string(R.string.waiting_budget), "amount" to it)) }
        val republish = "republish" in state.visibleActions
        binding.primary.text = string(if (republish) R.string.action_republish else R.string.waiting_follow)
        binding.primary.state = if (state.isBusy) ButtonVisualState.Loading else ButtonVisualState.Normal
        binding.primary.onClick = { onAction(if (republish) "republish" else "follow") }
        binding.edit.isVisible = "edit_request" in state.visibleActions
        binding.cancel.isVisible = "cancel" in state.visibleActions
    }

    private fun info(@DrawableRes icon: Int, text: String) {
        val row = TextView(context).apply {
            this.text = text
            TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Bremo_Caption)
            setTextColor(context.getColor(R.color.bremo_navy900))
            setBackgroundResource(R.drawable.bremo_bg_info_pill)
            val horizontal = resources.getDimensionPixelSize(R.dimen.bremo_space_m)
            val vertical = resources.getDimensionPixelSize(R.dimen.bremo_space_xs)
            setPaddingRelative(horizontal, vertical, horizontal, vertical)
            compoundDrawablePadding = vertical
            setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
            TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(context.getColor(R.color.bremo_primary600)))
            sizeCompoundIcons(R.dimen.bremo_size_icon_small)
        }
        binding.infoRows.addView(row, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
    }

}
