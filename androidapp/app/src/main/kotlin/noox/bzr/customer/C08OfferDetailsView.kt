package noox.bzr.customer

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import noox.bzr.design.BzrFormat
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.R
import noox.bzr.design.views.RadioCardView
import noox.bzr.design.views.cardShadow
import noox.bzr.design.views.sizeCompoundIcons
import noox.bzr.gallery.databinding.ScreenC08OfferDetailsBinding

/**
 * SCR-C08 — Figma «Primo App» «تأكيد اختيار الفني» 3002:1757 (DEC-073): technician card, offer details (price, what it
 * includes, materials, assurance), request summary, payment method, then «تأكيد اختيار …» and «الرجوع للعروض».
 * `fieldValues`: name, rating, completed, eta, category, materials; `items`: amount, eta or slot, includes;
 * `itemDetails`: problem, address, slot (blank for «الآن»).
 */
class C08OfferDetailsView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC08OfferDetailsBinding.inflate(inflater, content)
    var onAction: (String) -> Unit = {}
    var onPaymentSelect: (Int) -> Unit = {}

    init {
        pinAction(binding.dock)
        binding.confirm.onClick = { onAction("accept_offer") }
        binding.back.setOnClickListener { onBack() }
    }

    override fun title(state: CustomerUiState) = string(R.string.confirm_title)

    override fun renderContent(state: CustomerUiState) {
        val values = state.fieldValues
        fun value(index: Int) = values.getOrElse(index) { "" }
        val name = value(0)
        val eta = value(3).toIntOrNull()?.let { BzrFormat.quantity(context, R.plurals.plural_offer_eta, it) }.orEmpty()
        binding.body.removeAllViews()
        binding.body.addView(providerCard(state, name, eta))
        binding.body.addView(heading(string(R.string.confirm_offer_details), string(R.string.confirm_fixed)))
        binding.body.addView(offerCard(state))
        binding.body.addView(text(string(R.string.confirm_assurance), R.style.TextAppearance_Bremo_Hint).apply {
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_shield, 0, 0, 0)
            tint(this)
            compoundDrawablePadding = px(R.dimen.bremo_space_xs)
            sizeCompoundIcons(R.dimen.bremo_size_icon_small)
        })
        binding.body.addView(heading(string(R.string.confirm_summary)))
        binding.body.addView(summaryCard(state, eta))
        if (state.options.isNotEmpty()) {
            binding.body.addView(heading(string(R.string.confirm_payment)))
            state.options.forEachIndexed { index, label ->
                binding.body.addView(RadioCardView(context).apply {
                    title = label
                    icon = R.drawable.ic_payments
                    selected = index == state.selectedOptionIndex
                    setOnClickListener { onPaymentSelect(index) }
                }, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            }
        }
        binding.confirm.text = BzrFormat.fill(string(R.string.confirm_action), "name" to name.substringBefore(' '))
        binding.confirm.isVisible = "accept_offer" in state.visibleActions
        binding.confirm.state = if (state.isBusy) ButtonVisualState.Loading else continueState(state)
    }

    private fun providerCard(state: CustomerUiState, name: String, eta: String): View {
        val card = card()
        val top = row()
        top.addView(text(name, R.style.TextAppearance_Bremo_OfferName), LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        state.fieldValues.getOrNull(1)?.takeIf(String::isNotEmpty)?.let { rating ->
            top.addView(text(rating, R.style.TextAppearance_Bremo_DetailStrong).apply {
                setBackgroundResource(R.drawable.bremo_bg_info_pill)
                setPaddingRelative(px(R.dimen.bremo_space_s), 0, px(R.dimen.bremo_space_s), 0)
                setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_star_filled, 0, 0, 0)
                TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(context.getColor(R.color.bremo_star)))
                compoundDrawablePadding = px(R.dimen.bremo_space_xs)
                sizeCompoundIcons(R.dimen.bremo_size_icon_small)
            })
        }
        card.addView(top)
        val category = state.fieldValues.getOrElse(4) { "" }
        val meta = listOfNotNull(
            BzrFormat.fill(string(R.string.provider_verified_title), "category" to category).takeIf { state.providerVerified && category.isNotEmpty() },
            state.fieldValues.getOrNull(2)?.takeIf(String::isNotEmpty)?.let { BzrFormat.quantity(context, R.plurals.plural_confirm_completed, (it).toIntOrNull() ?: 0) },
        ).joinToString(" • ")
        if (meta.isNotEmpty()) card.addView(text(meta, R.style.TextAppearance_Bremo_Hint).apply { setTextColor(context.getColor(R.color.bremo_primary600)) })
        if (state.showEta && eta.isNotEmpty()) {
            card.addView(text(eta, R.style.TextAppearance_Bremo_LinkSmall).apply {
                setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_bolt, 0, 0, 0)
                tint(this)
                compoundDrawablePadding = px(R.dimen.bremo_space_xs)
                sizeCompoundIcons(R.dimen.bremo_size_icon_small)
            })
        }
        return card
    }

    private fun offerCard(state: CustomerUiState): View {
        val card = card()
        val priceRow = row()
        priceRow.addView(text(string(R.string.confirm_offer_price), R.style.TextAppearance_Bremo_Caption), LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        priceRow.addView(text(BzrFormat.fill(string(R.string.customer_home_offers_amount), "amount" to state.items.getOrElse(0) { "" }), R.style.TextAppearance_Bremo_AmountDisplay).apply {
            setTextColor(context.getColor(R.color.bremo_primary600))
        })
        card.addView(priceRow)
        val includes = state.items.getOrElse(2) { "" }.let { if (it == "deductible") string(R.string.offer_inspection_deductible) else it }
        if (includes.isNotEmpty()) card.addView(check(R.drawable.ic_check, includes))
        state.fieldValues.getOrNull(5)?.takeIf(String::isNotEmpty)?.let { card.addView(check(R.drawable.ic_info, it)) }
        return card
    }

    private fun summaryCard(state: CustomerUiState, eta: String): View {
        val card = card()
        val rows = state.itemDetails
        // STATIC (DEC-074, BACKEND-NOTES B-15) only when the offer has no ETA: «الآن» then the offer's own window.
        val time = rows.getOrElse(2) { "" }.ifEmpty { listOf(string(R.string.provider_home_timing_now), eta).filter(String::isNotEmpty).joinToString(" — ") }
        listOf(
            Triple(R.drawable.ic_wrench, R.string.confirm_summary_problem, rows.getOrElse(0) { "" }),
            Triple(R.drawable.ic_location, R.string.confirm_summary_address, rows.getOrElse(1) { "" }),
            Triple(R.drawable.ic_clock, R.string.confirm_summary_time, time),
        ).filter { it.third.isNotEmpty() }.forEach { (icon, label, value) ->
            val line = row()
            line.addView(FrameLayout(context).apply {
                setBackgroundResource(R.drawable.bremo_bg_order_well)
                addView(icon(icon), FrameLayout.LayoutParams(px(R.dimen.bremo_size_icon_small), px(R.dimen.bremo_size_icon_small), Gravity.CENTER))
            }, LinearLayout.LayoutParams(px(R.dimen.bremo_size_order_well), px(R.dimen.bremo_size_order_well)))
            line.addView(noox.bzr.design.views.BremoLinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPaddingRelative(px(R.dimen.bremo_space_m), 0, 0, 0)
                addView(text(string(label), R.style.TextAppearance_Bremo_Hint))
                addView(text(value, R.style.TextAppearance_Bremo_TileTitle))
            }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            card.addView(line)
        }
        return card
    }

    private fun heading(title: String, badge: String? = null): View {
        val line = row()
        line.addView(text(title, R.style.TextAppearance_Bremo_HomeSection), LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        badge?.let {
            line.addView(text(it, R.style.TextAppearance_Bremo_StatusPill).apply {
                setBackgroundResource(R.drawable.bremo_bg_pill)
                setPaddingRelative(px(R.dimen.bremo_space_s), 0, px(R.dimen.bremo_space_s), 0)
            })
        }
        return line
    }

    private fun check(@DrawableRes icon: Int, value: String) = text(value, R.style.TextAppearance_Bremo_Secondary).apply {
        setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
        tint(this)
        compoundDrawablePadding = px(R.dimen.bremo_space_s)
        sizeCompoundIcons(R.dimen.bremo_size_icon_small)
    }

    private fun card() = noox.bzr.design.views.BremoLinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.bremo_bg_card)
        cardShadow()
        dividerDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bremo_gap_s)
        showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        setPadding(px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l))
    }

    private fun row() = noox.bzr.design.views.BremoLinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun icon(@DrawableRes icon: Int) = ImageView(context).apply {
        setImageResource(icon)
        ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(context.getColor(R.color.bremo_primary600)))
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private fun tint(view: TextView) =
        TextViewCompat.setCompoundDrawableTintList(view, ColorStateList.valueOf(context.getColor(R.color.bremo_primary600)))

    private fun text(value: String, @StyleRes style: Int) = TextView(context).apply {
        text = value
        TextViewCompat.setTextAppearance(this, style)
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    }

    private fun px(id: Int) = resources.getDimensionPixelSize(id)
}
