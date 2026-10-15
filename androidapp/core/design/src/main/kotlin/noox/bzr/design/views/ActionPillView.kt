package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.withStyledAttributes
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import noox.bzr.design.R

/** DEC-070 ActionPill (Figma «Primo App» 3002:144): a compact white card button — icon then label. */
class ActionPillView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : BremoLinearLayout(context, attrs) {
    private val iconView = ImageView(context)
    private val label = TextView(context)

    var text: String = ""
        set(value) {
            field = value
            label.text = value
            contentDescription = value
        }

    @DrawableRes
    var icon: Int = 0
        set(value) {
            field = value
            iconView.showIf(value != 0)
            if (value != 0) iconView.setImageResource(value)
        }

    var onClick: () -> Unit = {}

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER
        gap(R.drawable.bremo_gap_xs)
        setBackgroundResource(R.drawable.bremo_bg_card)
        cardShadow()
        isClickable = true
        isFocusable = true
        foreground = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground)).let { attributes ->
            try { attributes.getDrawable(0) } finally { attributes.recycle() }
        }
        setPaddingRelative(px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_xs), px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_xs))
        minimumHeight = px(R.dimen.bremo_size_action_pill_height)
        iconView.importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        ImageViewCompat.setImageTintList(iconView, android.content.res.ColorStateList.valueOf(color(R.color.bremo_primary600)))
        addView(iconView, LayoutParams(px(R.dimen.bremo_size_icon_small), px(R.dimen.bremo_size_icon_small)))
        TextViewCompat.setTextAppearance(label, R.style.TextAppearance_Bremo_Caption)
        label.setTextColor(color(R.color.bremo_navy900))
        label.maxLines = 2
        label.gravity = Gravity.CENTER
        addView(label, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        setOnClickListener { onClick() }
        context.withStyledAttributes(attrs, R.styleable.ActionPillView) {
            text = getString(R.styleable.ActionPillView_bremoText).orEmpty()
            icon = getResourceId(R.styleable.ActionPillView_bremoIcon, 0)
        }
    }
}
