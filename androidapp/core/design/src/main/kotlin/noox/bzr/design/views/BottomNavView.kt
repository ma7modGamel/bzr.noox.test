package noox.bzr.design.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import noox.bzr.design.R

/** Stable destinations, readable labels and a quiet selection capsule. RTL follows resource localization. */
class BottomNavView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
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

private class NavItemView(context: Context, icon: Int, label: String) : LinearLayout(context) {
    private val iconContainer = LinearLayout(context).apply {
        gravity = Gravity.CENTER
        val horizontal = px(R.dimen.bremo_space_l)
        val vertical = px(R.dimen.bremo_space_xs)
        setPaddingRelative(horizontal, vertical, horizontal, vertical)
        background = GradientDrawable().apply {
            cornerRadius = resources.getDimension(R.dimen.bremo_radius_full)
        }
    }
    private val iconView = ImageView(context)
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
        iconView.setImageResource(icon)
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
        val tint = color(if (selected) R.color.bremo_primary700 else R.color.bremo_slate400)
        iconView.setColorFilter(tint)
        labelView.setTextColor(tint)
        (iconContainer.background as GradientDrawable).setColor(
            if (selected) color(R.color.bremo_primary50) else ContextCompat.getColor(context, android.R.color.transparent),
        )
        iconContainer.animate().cancel()
        if (animate && selected && ValueAnimator.areAnimatorsEnabled() && !SnapshotMode.enabled) {
            iconContainer.alpha = fraction(R.dimen.bremo_ratio_press_feedback)
            iconContainer.animate().alpha(1f).setDuration(resources.getInteger(R.integer.bremo_motion_nav_bar_ms).toLong()).start()
        } else {
            iconContainer.alpha = 1f
        }
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = "android.widget.Tab"
        info.isSelected = isSelected
    }
}
