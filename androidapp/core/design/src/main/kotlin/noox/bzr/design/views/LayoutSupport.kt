package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.content.withStyledAttributes
import noox.bzr.design.R

/** Spacing between children through a generated transparent gap drawable (Compose spacedBy / SwiftUI spacing). */
internal fun LinearLayout.gap(@DrawableRes gap: Int) {
    dividerDrawable = drawable(gap)
    showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
}

/** LinearLayout that never grows past [maxWidth] (ChatBubble: DesignSize.chatBubbleMaxWidth). */
class MaxWidthLinearLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    var maxWidth: Int = Int.MAX_VALUE
        set(value) {
            field = value
            requestLayout()
        }

    init {
        context.withStyledAttributes(attrs, R.styleable.MaxWidthLinearLayout) {
            maxWidth = getDimensionPixelSize(R.styleable.MaxWidthLinearLayout_bremoMaxWidth, Int.MAX_VALUE)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = MeasureSpec.getSize(widthMeasureSpec)
        val mode = MeasureSpec.getMode(widthMeasureSpec)
        val limit = if (mode == MeasureSpec.UNSPECIFIED) maxWidth else minOf(available, maxWidth)
        super.onMeasure(MeasureSpec.makeMeasureSpec(limit, if (mode == MeasureSpec.EXACTLY) MeasureSpec.EXACTLY else MeasureSpec.AT_MOST), heightMeasureSpec)
    }
}

/** Fixed-height components (43 §3 sizes) measure to exactly their token height. */
internal fun fixedHeight(height: Int): Int = android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY)

/**
 * Set by the Paparazzi tests only. layoutlib scrolls single-line RTL EditText content out of view,
 * so snapshot rendering turns horizontal scrolling off; devices keep the standard behaviour.
 */
object SnapshotMode {
    @Volatile
    var enabled: Boolean = false
}
