package noox.bzr.design.views

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import noox.bzr.design.R

/**
 * Stable destinations, readable labels and a selection capsule (DEC-063: soft gradient capsule, gradient icon,
 * bold label, a short grow-in that is skipped when the system turns animations off). RTL follows localization.
 */
class BottomNavView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : BremoLinearLayout(context, attrs) {
    var items: List<Pair<Int, String>> = emptyList()
        set(value) {
            if (field == value) return
            field = value
            render()
        }
    var selectedIndex: Int = 0
        set(value) {
            if (field == value) return
            field = value
            itemViews.forEachIndexed { index, item -> item.select(index == value, isLaidOut) }
        }
    var onSelect: (Int) -> Unit = {}
    private val itemViews = mutableListOf<NavItemView>()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.TOP
        minimumHeight = px(R.dimen.bremo_size_bottom_nav_height)
        setBackgroundResource(R.color.bremo_surface)
        cardShadow()
        val padding = px(R.dimen.bremo_space_s)
        setPaddingRelative(padding, padding, padding, padding)
        gap(R.drawable.bremo_gap_xs)
    }

    private fun render() {
        removeAllViews()
        itemViews.clear()
        items.forEachIndexed { index, (icon, label) ->
            val item = NavItemView(context, icon, label).apply {
                select(index == selectedIndex, false)
                setOnClickListener { onSelect(index) }
            }
            itemViews += item
            addView(item, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        }
    }
}

private class NavItemView(context: Context, icon: Int, label: String) : BremoLinearLayout(context) {
    private val iconContainer = BremoLinearLayout(context).apply {
        gravity = Gravity.CENTER
        val horizontal = px(R.dimen.bremo_space_l)
        val vertical = px(R.dimen.bremo_space_xs)
        setPaddingRelative(horizontal, vertical, horizontal, vertical)
    }
    private val iconView = GradientIconView(context)
    private val labelView = TextView(context).apply {
        appearance(R.style.TextAppearance_Bremo_Caption)
        gravity = Gravity.CENTER
        text = label
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        minimumHeight = px(R.dimen.bremo_size_touch_target_min)
        contentDescription = label
        isFocusable = true
        iconView.setIcon(icon)
        val size = px(R.dimen.bremo_size_icon)
        iconContainer.addView(iconView, LayoutParams(size, size))
        addView(iconContainer, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        addView(labelView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            topMargin = px(R.dimen.bremo_space_xs)
        })
        foreground = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground)).let { attributes ->
            try { attributes.getDrawable(0) } finally { attributes.recycle() }
        }
    }

    fun select(selected: Boolean, animate: Boolean) {
        isSelected = selected
        if (selected) {
            iconView.setGradient(color(R.color.bremo_gradient_icon_brand_from), color(R.color.bremo_gradient_icon_brand_to))
            iconContainer.setBackgroundResource(R.drawable.bremo_bg_nav_pill)
        } else {
            iconView.setGradient(color(R.color.bremo_slate400), color(R.color.bremo_slate400))
            iconContainer.background = null
        }
        labelView.setTextColor(color(if (selected) R.color.bremo_primary700 else R.color.bremo_slate400))
        labelView.setTypeface(labelView.typeface, if (selected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        iconContainer.animate().cancel()
        if (animate && selected && ValueAnimator.areAnimatorsEnabled() && !SnapshotMode.enabled) {
            iconContainer.alpha = fraction(R.dimen.bremo_ratio_press_feedback)
            iconContainer.scaleX = NAV_GROW_FROM
            iconContainer.scaleY = NAV_GROW_FROM
            iconContainer.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setInterpolator(android.view.animation.OvershootInterpolator())
                .setDuration(resources.getInteger(R.integer.bremo_motion_nav_bar_ms).toLong()).start()
        } else {
            iconContainer.alpha = 1f
            iconContainer.scaleX = 1f
            iconContainer.scaleY = 1f
        }
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = "android.widget.Tab"
        info.isSelected = isSelected
    }

    private companion object {
        /** The capsule grows in from this scale when a tab is chosen. */
        const val NAV_GROW_FROM = 0.86f
    }
}
