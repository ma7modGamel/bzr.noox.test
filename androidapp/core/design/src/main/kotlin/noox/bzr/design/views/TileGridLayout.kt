package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import kotlin.math.max
import noox.bzr.design.R

/**
 * DEC-063: an equal-width tile grid. Every column gets exactly (width − gaps) / columns, so a long label or a
 * wide tile can never push a row past the screen edge, and every row is as tall as its tallest tile. The column
 * count adapts to the width and the font scale (at least [R.dimen.bremo_size_category_column_min_width] each),
 * and items flow from the start edge (right in Arabic).
 */
class TileGridLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ViewGroup(context, attrs) {
    private val gap = px(R.dimen.bremo_space_m)
    private val maxColumns = resources.getInteger(R.integer.bremo_size_home_service_tile_columns)
    private var columns = 1

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val inner = width - paddingStart - paddingEnd
        val minCell = resources.getDimension(R.dimen.bremo_size_category_column_min_width) * resources.configuration.fontScale
        columns = ((inner + gap) / (minCell + gap)).toInt().coerceIn(1, maxColumns)
        val cell = (inner - gap * (columns - 1)) / columns
        val visible = (0 until childCount).map(::getChildAt).filter { it.visibility != GONE }
        var height = paddingTop + paddingBottom
        visible.chunked(columns).forEachIndexed { row, children ->
            children.forEach { it.measure(MeasureSpec.makeMeasureSpec(cell, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)) }
            val rowHeight = children.maxOf(View::getMeasuredHeight)
            // Equal heights in a row, so tiles with a two-line label line up with their neighbours.
            children.forEach { it.measure(MeasureSpec.makeMeasureSpec(cell, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(rowHeight, MeasureSpec.EXACTLY)) }
            height += rowHeight + if (row > 0) gap else 0
        }
        setMeasuredDimension(width, max(height, suggestedMinimumHeight))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val visible = (0 until childCount).map(::getChildAt).filter { it.visibility != GONE }
        var top = paddingTop
        visible.chunked(columns).forEach { children ->
            children.forEachIndexed { column, child ->
                val offset = column * (child.measuredWidth + gap)
                val left = if (rtl) width - paddingStart - offset - child.measuredWidth else paddingStart + offset
                child.layout(left, top, left + child.measuredWidth, top + child.measuredHeight)
            }
            top += children.maxOf(View::getMeasuredHeight) + gap
        }
    }
}
