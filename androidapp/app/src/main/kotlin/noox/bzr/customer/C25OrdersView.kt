package noox.bzr.customer

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import noox.bzr.design.BzrFormat
import noox.bzr.design.BadgeKind
import noox.bzr.design.R
import noox.bzr.design.generated.CategoryIcons
import noox.bzr.design.views.BremoLinearLayout
import noox.bzr.design.views.BadgeView
import noox.bzr.design.views.PrimaryButtonView
import noox.bzr.design.views.SecondaryButtonView
import noox.bzr.design.views.cardShadow
import noox.bzr.design.views.sizeCompoundIcons
import noox.bzr.gallery.databinding.ScreenC25OrdersBinding

/**
 * SCR-C25 — Figma «طلباتي» 3002:3889 (DEC-076): «الحالية / السابقة / الملغاة» segments, then one card per order —
 * status and number, the problem with its category well and place, the technician strip (call) or, while open, the
 * offer figures, then the approved total with chat and «متابعة الخدمة», or «عرض العروض». `options` rows are
 * "number|place|provider|verified|rating|amount|chat|phone".
 */
class C25OrdersView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC25OrdersBinding.inflate(inflater, content)
    var onTabSelect: (Int) -> Unit = {}
    var onOrderSelect: (Int) -> Unit = {}
    var onChat: (Int) -> Unit = {}

    init {
        binding.tabs.setBackgroundResource(R.drawable.bremo_bg_pill)
        val inset = px(R.dimen.bremo_space_xs)
        binding.tabs.setPadding(inset, inset, inset, inset)
    }

    override fun title(state: CustomerUiState) = string(R.string.orders_title)

    override fun renderContent(state: CustomerUiState) {
        val tab = state.selectedOptionIndex.coerceAtLeast(0)
        binding.tabs.removeAllViews()
        listOf(R.string.orders_current, R.string.orders_past, R.string.orders_cancelled).forEachIndexed { index, label ->
            val count = if (index == tab && state.items.isNotEmpty()) " (${BzrFormat.number(state.items.size)})" else ""
            binding.tabs.addView(text(string(label) + count, R.style.TextAppearance_Bremo_TileTitle).apply {
                gravity = Gravity.CENTER
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                minHeight = px(R.dimen.bremo_size_chip_height)
                setTextColor(context.getColor(if (index == tab) R.color.bremo_primary600 else R.color.bremo_slate500))
                background = if (index == tab) GradientDrawable().apply {
                    cornerRadius = px(R.dimen.bremo_radius_full).toFloat()
                    setColor(context.getColor(R.color.bremo_surface))
                } else null
                isSelected = index == tab
                isClickable = true
                isFocusable = true
                setOnClickListener { if (index != tab) onTabSelect(index) }
            }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        }
        binding.empty.isVisible = state.phase == CustomerPhase.Empty
        binding.empty.title = string(listOf(R.string.orders_current_empty_title, R.string.orders_past_empty_title, R.string.orders_cancelled_empty_title)[tab.coerceAtMost(2)])
        binding.empty.body = string(listOf(R.string.orders_current_empty_body, R.string.orders_past_empty_body, R.string.orders_cancelled_empty_body)[tab.coerceAtMost(2)])
        binding.orders.removeAllViews()
        state.items.indices.forEach { binding.orders.addView(orderCard(state, it)) }
        binding.loadingMore.isVisible = state.isBusy
    }

    private fun orderCard(state: CustomerUiState, index: Int): View {
        val row = state.options.getOrElse(index) { "" }.split('|')
        fun field(at: Int) = row.getOrElse(at) { "" }
        val status = state.optionCodes.getOrElse(index) { "" }
        val open = status == "OPEN"
        val offers = state.orderOfferCounts.getOrElse(index) { 0 }
        val icon = state.orderIcons.getOrElse(index) { "" }
        return column(R.drawable.bremo_gap_m).apply {
            setBackgroundResource(R.drawable.bremo_bg_card)
            cardShadow()
            setPadding(px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l))
            isClickable = true
            isFocusable = true
            setOnClickListener { onOrderSelect(index) }
            addView(row().apply {
                addView(BadgeView(context).apply {
                    text = state.itemStates.getOrElse(index) { "" }
                    kind = BadgeKind.forOrderStatus(status)
                    this.icon = when (kind) {
                        BadgeKind.Success -> R.drawable.ic_check
                        BadgeKind.Warning -> R.drawable.ic_clock
                        BadgeKind.Neutral -> R.drawable.ic_close
                        BadgeKind.Danger -> R.drawable.ic_warning
                        else -> R.drawable.ic_orders
                    }
                })
                addView(View(context), LinearLayout.LayoutParams(0, 0, 1f))
                if (field(0).isNotBlank()) addView(text(BzrFormat.fill(string(R.string.orders_number), "number" to field(0)), R.style.TextAppearance_Bremo_Hint))
            })
            addView(row().apply {
                addView(column(R.drawable.bremo_gap_xs).apply {
                    addView(text(state.items[index], R.style.TextAppearance_Bremo_HomeSection))
                    if (field(1).isNotBlank()) addView(withIcon(text(field(1), R.style.TextAppearance_Bremo_Hint), R.drawable.ic_location, R.color.bremo_slate400))
                }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                addView(FrameLayout(context).apply {
                    setBackgroundResource(R.drawable.bremo_bg_inset)
                    addView(ImageView(context).apply {
                        setImageResource(if (icon.isEmpty()) R.drawable.ic_wrench else CategoryIcons.drawable(icon))
                        ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(context.getColor(R.color.bremo_primary600)))
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }, FrameLayout.LayoutParams(px(R.dimen.bremo_size_icon), px(R.dimen.bremo_size_icon), Gravity.CENTER))
                }, LinearLayout.LayoutParams(px(R.dimen.bremo_size_home_tile_well), px(R.dimen.bremo_size_home_tile_well)))
            })
            if (field(2).isNotBlank()) addView(providerStrip(field(2), field(3) == "verified", field(4)))
            if (open && offers > 0) addView(offersStrip(offers, state.orderLowestOffers.getOrElse(index) { "" }))
            when {
                field(5).isNotBlank() -> addView(row().apply {
                    addView(column(0).apply {
                        addView(text(string(R.string.orders_total), R.style.TextAppearance_Bremo_Hint))
                        addView(text(BzrFormat.fill(string(R.string.customer_home_offers_amount), "amount" to field(5)), R.style.TextAppearance_Bremo_AmountSmall))
                    }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                    if (field(6) == "chat") addView(noox.bzr.design.views.IconSquareButtonView(context).apply {
                        this.icon = R.drawable.ic_messages
                        label = string(R.string.button_chat)
                        onClick = { onChat(index) }
                    }, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { marginEnd = px(R.dimen.bremo_space_s) })
                    if (status !in ENDED) addView(PrimaryButtonView(context).apply {
                        text = string(R.string.orders_follow)
                        this.icon = R.drawable.ic_arrow_forward
                        compact = true
                        onClick = { onOrderSelect(index) }
                    }, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
                })
                open -> addView(SecondaryButtonView(context).apply {
                    text = string(R.string.customer_home_offers_action)
                    this.icon = R.drawable.ic_arrow_forward
                    onClick = { onOrderSelect(index) }
                }, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.END })
            }
        }
    }

    private fun providerStrip(name: String, verified: Boolean, rating: String) = row().apply {
        setBackgroundResource(R.drawable.bremo_bg_inset)
        setPadding(px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_m))
        addView(column(0).apply {
            addView(text(name, R.style.TextAppearance_Bremo_TileTitle).apply {
                if (verified) {
                    setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_verified, 0)
                    TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(context.getColor(R.color.bremo_primary600)))
                    compoundDrawablePadding = px(R.dimen.bremo_space_xs)
                    sizeCompoundIcons(R.dimen.bremo_size_icon_small)
                }
            }, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
            if (rating.isNotBlank()) addView(withIcon(text(rating, R.style.TextAppearance_Bremo_DetailStrong), R.drawable.ic_star_filled, R.color.bremo_star))
        }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

    }

    private fun offersStrip(count: Int, lowest: String) = row().apply {
        setBackgroundResource(R.drawable.bremo_bg_inset)
        setPadding(px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_m))
        addView(ImageView(context).apply {
            setImageResource(R.drawable.ic_tag)
            ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(context.getColor(R.color.bremo_primary600)))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(px(R.dimen.bremo_size_icon), px(R.dimen.bremo_size_icon)))
        addView(column(0).apply {
            setPaddingRelative(px(R.dimen.bremo_space_s), 0, 0, 0)
            addView(text(BzrFormat.quantity(context, R.plurals.plural_customer_home_offers_new, count), R.style.TextAppearance_Bremo_TileTitle))
            if (lowest.isNotBlank()) addView(text(BzrFormat.fill(string(R.string.orders_offers_from), "amount" to lowest), R.style.TextAppearance_Bremo_Hint))
        }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        // BACKEND-NOTES B-16: no offer photos are loaded, so the count stands where Figma stacks the avatars.
        addView(text(BzrFormat.number(count), R.style.TextAppearance_Bremo_TileTitle).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(context.getColor(R.color.bremo_on_primary))
            setBackgroundResource(R.drawable.bremo_bg_button_primary)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            minWidth = px(R.dimen.bremo_size_nearby_avatar)
            minHeight = px(R.dimen.bremo_size_nearby_avatar)
        }, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
    }

    private fun withIcon(view: TextView, @DrawableRes icon: Int, tint: Int) = view.apply {
        setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
        TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(context.getColor(tint)))
        compoundDrawablePadding = px(R.dimen.bremo_space_xs)
        sizeCompoundIcons(R.dimen.bremo_size_icon_small)
    }

    private fun column(@DrawableRes gap: Int) = BremoLinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        if (gap != 0) {
            dividerDrawable = ContextCompat.getDrawable(context, gap)
            showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        }
    }

    private fun row() = BremoLinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun text(value: String, @StyleRes style: Int) = TextView(context).apply {
        text = value
        TextViewCompat.setTextAppearance(this, style)
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    }

    private fun px(id: Int) = resources.getDimensionPixelSize(id)

    private companion object {
        val ENDED = setOf("CLOSED", "CANCELLED", "EXPIRED")
    }
}
