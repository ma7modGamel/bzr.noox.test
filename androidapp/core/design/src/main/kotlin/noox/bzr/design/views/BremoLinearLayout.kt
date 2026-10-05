package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatViewInflater
import noox.bzr.design.R

/**
 * LinearLayout whose spacing never shifts a row in RTL.
 *
 * Up to Android 13, a horizontal LinearLayout that spaces children with `showDividers` places each divider on the
 * wrong side of its child when the layout is right-to-left: the whole row moves toward the start edge by the
 * divider widths and its first item is cut off (seen on a Redmi Note 11, Android 13). Our dividers are only
 * transparent gaps (`bremo_gap_*`), so a horizontal row turns them into margins between its visible children:
 * the same spacing, laid out by the platform's correct margin logic on every Android version. Vertical layouts
 * keep their dividers.
 */
open class BremoLinearLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    /** The divider flags a horizontal row asked for, now applied as gaps. */
    private var gapFlags = SHOW_DIVIDER_NONE

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (orientation == HORIZONTAL) {
            // Taken lazily here, after the constructor and any later gap() call have set the dividers.
            if (showDividers != SHOW_DIVIDER_NONE) {
                gapFlags = showDividers
                super.setShowDividers(SHOW_DIVIDER_NONE)
            }
            applyGaps()
        } else if (gapFlags != SHOW_DIVIDER_NONE && showDividers == SHOW_DIVIDER_NONE) {
            super.setShowDividers(gapFlags)
            gapFlags = SHOW_DIVIDER_NONE
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /** Puts one divider width between visible children (and at the ends if asked), on the start side of each. */
    private fun applyGaps() {
        val gap = dividerDrawable?.intrinsicWidth?.coerceAtLeast(0) ?: 0
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val visible = (0 until childCount).map(::getChildAt).filter { it.visibility != GONE }
        visible.forEachIndexed { index, child ->
            val params = child.layoutParams as? MarginLayoutParams ?: return@forEachIndexed
            val base = baseMargins(child, params)
            var before = 0
            var after = 0
            if (gap > 0) {
                if (index > 0 && gapFlags and SHOW_DIVIDER_MIDDLE != 0) before = gap
                if (index == 0 && gapFlags and SHOW_DIVIDER_BEGINNING != 0) before = gap
                if (index == visible.lastIndex && gapFlags and SHOW_DIVIDER_END != 0) after = gap
            }
            val start = base.first + before
            val end = base.second + after
            if (params.marginStart != start || params.marginEnd != end) {
                params.marginStart = start
                params.marginEnd = end
                // Resolve now: this pass reads left/right margins before the child resolves its own params.
                params.resolveLayoutDirection(if (rtl) LAYOUT_DIRECTION_RTL else LAYOUT_DIRECTION_LTR)
            }
        }
    }

    /** The child's own start/end margins, remembered before any gap was added. */
    private fun baseMargins(child: View, params: MarginLayoutParams): Pair<Int, Int> {
        @Suppress("UNCHECKED_CAST")
        val stored = child.getTag(R.id.bremo_base_margins) as? Pair<Int, Int>
        if (stored != null) return stored
        val base = params.marginStart to params.marginEnd
        child.setTag(R.id.bremo_base_margins, base)
        return base
    }

    override fun onViewRemoved(child: View) {
        super.onViewRemoved(child)
        child.setTag(R.id.bremo_base_margins, null)
    }
}

/**
 * Inflates every `<LinearLayout>` in the app as [BremoLinearLayout] (Theme.Bremo `viewInflaterClass`), so XML rows
 * get the RTL-safe gaps without each layout naming the class.
 */
class BremoViewInflater : AppCompatViewInflater() {
    override fun createView(context: Context, name: String, attrs: AttributeSet): View? =
        if (name == "LinearLayout") BremoLinearLayout(context, attrs) else null
}
