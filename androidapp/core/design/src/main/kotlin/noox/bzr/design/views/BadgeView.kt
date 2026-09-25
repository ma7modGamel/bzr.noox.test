package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.content.withStyledAttributes
import noox.bzr.design.BadgeKind
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewBadgeBinding

/** 43 §3 Badge: highlight (amber, optional star) or order status (teal). */
class BadgeView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val binding: ViewBadgeBinding

    var text: String = ""
        set(value) {
            field = value
            binding.text.text = value
        }
    var kind: BadgeKind = BadgeKind.Highlight
        set(value) {
            field = value
            render()
        }

    @DrawableRes
    var icon: Int? = null
        set(value) {
            field = value
            render()
        }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        gap(R.drawable.bremo_gap_xs)
        setPaddingRelative(px(R.dimen.bremo_space_s), px(R.dimen.bremo_space_xs), px(R.dimen.bremo_space_s), px(R.dimen.bremo_space_xs))
        binding = ViewBadgeBinding.inflate(inflater, this)
        context.withStyledAttributes(attrs, R.styleable.BadgeView) {
            text = getString(R.styleable.BadgeView_bremoText).orEmpty()
            kind = enumValue(R.styleable.BadgeView_badgeKind, BadgeKind.entries.toTypedArray(), BadgeKind.Highlight)
            icon = resource(R.styleable.BadgeView_bremoIcon)
        }
        render()
    }

    private fun render() {
        val highlight = kind == BadgeKind.Highlight
        setBackgroundResource(if (highlight) R.drawable.bremo_bg_badge_highlight else R.drawable.bremo_bg_badge_status)
        binding.text.setTextColor(color(if (highlight) R.color.bremo_badge_text else R.color.bremo_primary700))
        binding.icon.showIf(icon != null)
        icon?.let { binding.icon.icon(it, R.color.bremo_star) }
    }
}
