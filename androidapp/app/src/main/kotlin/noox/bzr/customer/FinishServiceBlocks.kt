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
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import noox.bzr.design.BzrFormat
import noox.bzr.design.R
import noox.bzr.design.views.BremoLinearLayout
import noox.bzr.design.views.cardShadow
import noox.bzr.design.views.sizeCompoundIcons

/**
 * The shared blocks of Figma «إنهاء الخدمة والدفع» 3002:4134 (DEC-076) for SCR-C21 (payment) and SCR-C23 (confirm):
 * the completion hero, the provider and job card, and the invoice. Before/after photos are left out — the API has none
 * and a stock picture would misrepresent the customer's own job (BACKEND-NOTES B-05).
 */
internal class FinishServiceBlocks(private val context: Context) {
    private val resources = context.resources

    fun hero(name: String): View = column(Gravity.CENTER_HORIZONTAL).apply {
        addView(FrameLayout(context).apply {
            setBackgroundResource(R.drawable.bremo_bg_success_ring)
            addView(FrameLayout(context).apply {
                setBackgroundResource(R.drawable.bremo_bg_success_core)
                addView(icon(R.drawable.ic_check, R.color.bremo_on_primary), FrameLayout.LayoutParams(px(R.dimen.bremo_size_icon), px(R.dimen.bremo_size_icon), Gravity.CENTER))
            }, FrameLayout.LayoutParams(px(R.dimen.bremo_size_success_core), px(R.dimen.bremo_size_success_core), Gravity.CENTER))
        }, LinearLayout.LayoutParams(px(R.dimen.bremo_size_success_ring), px(R.dimen.bremo_size_success_ring)))
        addView(text(context.getString(R.string.completion_done), R.style.TextAppearance_Bremo_StatusPill).apply {
            setTextColor(context.getColor(R.color.bremo_success))
            setBackgroundResource(R.drawable.bremo_bg_badge_success)
            setPaddingRelative(px(R.dimen.bremo_space_s), 0, px(R.dimen.bremo_space_s), 0)
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.bremo_bg_online_dot, 0, 0, 0)
            compoundDrawablePadding = px(R.dimen.bremo_space_xs)
            sizeCompoundIcons(R.dimen.bremo_size_status_dot)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        })
        if (name.isNotEmpty()) addView(text(BzrFormat.fill(context.getString(R.string.completion_heading), "name" to name.substringBefore(' ')), R.style.TextAppearance_Bremo_ScreenTitle).apply { gravity = Gravity.CENTER })
        addView(text(context.getString(R.string.completion_body), R.style.TextAppearance_Bremo_Subtitle).apply { gravity = Gravity.CENTER })
    }

    fun providerCard(name: String, verified: Boolean, title: String, address: String): View = card().apply {
        addView(row().apply {
            addView(column(Gravity.START).apply {
                addView(row().apply {
                    addView(text(name, R.style.TextAppearance_Bremo_OfferName), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    if (verified) addView(text(context.getString(R.string.completion_certified), R.style.TextAppearance_Bremo_StatusPill).apply {
                        setTextColor(context.getColor(R.color.bremo_verified_text))
                        setBackgroundResource(R.drawable.bremo_bg_verified_pill)
                        setPaddingRelative(px(R.dimen.bremo_space_s), 0, px(R.dimen.bremo_space_s), 0)
                    })
                })
                if (title.isNotEmpty()) addView(text(title, R.style.TextAppearance_Bremo_Secondary))
                if (address.isNotEmpty()) addView(text(address, R.style.TextAppearance_Bremo_Hint).apply {
                    setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_location, 0, 0, 0)
                    compoundDrawablePadding = px(R.dimen.bremo_space_xs)
                    sizeCompoundIcons(R.dimen.bremo_size_icon_small)
                })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        })
    }

    fun invoice(labor: String, materials: String, total: String): View = card().apply {
        addView(row().apply {
            addView(icon(R.drawable.ic_receipt, R.color.bremo_decorative_icon, R.dimen.bremo_size_icon_small))
            addView(text(context.getString(R.string.completion_invoice), R.style.TextAppearance_Bremo_HomeSection).apply {
                setPaddingRelative(px(R.dimen.bremo_space_s), 0, 0, 0)
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        })
        addView(line(context.getString(R.string.proposal_base), amount(labor), R.style.TextAppearance_Bremo_TileTitle))
        if (materials.isNotEmpty() && materials != "0") addView(line(context.getString(R.string.completion_extra), amount(materials), R.style.TextAppearance_Bremo_TileTitle))
        addView(View(context).apply { setBackgroundResource(R.drawable.bremo_bg_divider) }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(R.dimen.bremo_border_width)))
        addView(line(context.getString(R.string.completion_total), amount(total), R.style.TextAppearance_Bremo_AmountDisplay, R.color.bremo_amount_text))
    }

    fun heading(value: String): View = text(value, R.style.TextAppearance_Bremo_HomeSection)

    fun link(value: String, @DrawableRes icon: Int, onClick: () -> Unit): View = text(value, R.style.TextAppearance_Bremo_Caption).apply {
        gravity = Gravity.CENTER
        minHeight = px(R.dimen.bremo_size_touch_target_min)
        setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
        TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(context.getColor(R.color.bremo_slate500)))
        compoundDrawablePadding = px(R.dimen.bremo_space_xs)
        sizeCompoundIcons(R.dimen.bremo_size_icon_small)
        setOnClickListener { onClick() }
    }

    fun amount(value: String) = BzrFormat.fill(context.getString(R.string.customer_home_offers_amount), "amount" to value)

    private fun line(label: String, value: String, @StyleRes style: Int, colour: Int = R.color.bremo_navy900) = row().apply {
        addView(text(label, R.style.TextAppearance_Bremo_Caption), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        addView(text(value, style).apply { setTextColor(context.getColor(colour)) })
    }

    private fun card() = BremoLinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.bremo_bg_card)
        cardShadow()
        dividerDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bremo_gap_m)
        showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        setPadding(px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l))
    }

    private fun column(gravity: Int) = BremoLinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        this.gravity = gravity
        dividerDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bremo_gap_xs)
        showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
    }

    private fun row() = BremoLinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun icon(@DrawableRes icon: Int, tint: Int, size: Int = R.dimen.bremo_size_icon) = ImageView(context).apply {
        setImageResource(icon)
        ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(context.getColor(tint)))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        layoutParams = LinearLayout.LayoutParams(px(size), px(size))
    }

    private fun text(value: String, @StyleRes style: Int) = TextView(context).apply {
        text = value
        TextViewCompat.setTextAppearance(this, style)
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    }

    private fun px(id: Int) = resources.getDimensionPixelSize(id)
}
