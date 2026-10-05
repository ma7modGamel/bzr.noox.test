package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import noox.bzr.design.BzrFormat
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewRatingBarRowBinding

/** 43 §3 RatingBars: label, proportional bar, value. */
class RatingBarsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : BremoLinearLayout(context, attrs) {
    var items: List<Pair<String, Double>> = emptyList()
        set(value) {
            field = value
            render()
        }
    var max: Int = 1
        set(value) {
            field = value
            render()
        }

    init {
        orientation = VERTICAL
        gap(R.drawable.bremo_gap_s)
    }

    private fun render() {
        removeAllViews()
        items.forEach { (label, value) ->
            val row = BremoLinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                gap(R.drawable.bremo_gap_m)
            }
            val rowBinding = ViewRatingBarRowBinding.inflate(inflater, row)
            rowBinding.label.text = label
            rowBinding.value.text = BzrFormat.number(value)
            val fraction = (value / max).toFloat().coerceIn(0f, 1f)
            rowBinding.track.weightSum = 1f
            rowBinding.fill.layoutParams = LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, fraction)
            addView(row, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }
}
