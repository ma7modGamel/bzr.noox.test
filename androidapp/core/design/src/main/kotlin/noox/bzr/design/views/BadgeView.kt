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

/** Informational badges share compact dimensions and semantic palette colours. */
class BadgeView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : BremoLinearLayout(context, attrs) {
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
        minimumHeight = px(R.dimen.bremo_size_badge_height)
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
        val (background, foreground) = when (kind) {
            BadgeKind.Highlight -> R.drawable.bremo_bg_badge_highlight to R.color.bremo_badge_text
            BadgeKind.Status -> R.drawable.bremo_bg_badge_status to R.color.bremo_primary600
            BadgeKind.Success -> R.drawable.bremo_bg_badge_success to R.color.bremo_success
            BadgeKind.Warning -> R.drawable.bremo_bg_badge_warning to R.color.bremo_warning_text
            BadgeKind.Neutral -> R.drawable.bremo_bg_badge_neutral to R.color.bremo_neutral_text
            BadgeKind.Danger -> R.drawable.bremo_bg_badge_danger to R.color.bremo_danger
        }
        setBackgroundResource(background)
        binding.text.setTextColor(color(foreground))
        binding.icon.showIf(icon != null)
        icon?.let { binding.icon.icon(it, foreground) }
    }
}
