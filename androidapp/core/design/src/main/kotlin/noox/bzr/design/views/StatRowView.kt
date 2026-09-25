package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.card.MaterialCardView
import noox.bzr.design.R
import noox.bzr.design.StatItem
import noox.bzr.design.databinding.ViewStatItemBinding
import noox.bzr.design.databinding.ViewStatRowBinding

/** 43 §3 StatRow: equal columns (icon, value, label) split by vertical lines, inside a card. */
class StatRowView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    MaterialCardView(context, attrs, com.google.android.material.R.attr.materialCardViewStyle) {
    private val binding = ViewStatRowBinding.inflate(inflater, this)

    var items: List<StatItem> = emptyList()
        set(value) {
            field = value
            render()
        }

    init {
        val padding = px(R.dimen.bremo_space_card_padding)
        setContentPadding(padding, padding, padding, padding)
    }

    private fun render() {
        val row = binding.items
        row.removeAllViews()
        items.forEachIndexed { index, item ->
            val column = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                gap(R.drawable.bremo_gap_xs)
            }
            val itemBinding = ViewStatItemBinding.inflate(inflater, column)
            itemBinding.icon.icon(item.icon, R.color.bremo_primary500)
            itemBinding.value.text = item.value
            itemBinding.label.text = item.label
            row.addView(column, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            if (index != items.lastIndex) {
                val line = View(context).apply { setBackgroundResource(R.color.bremo_border) }
                row.addView(line, LinearLayout.LayoutParams(px(R.dimen.bremo_border_width), px(R.dimen.bremo_size_stat_divider)))
            }
        }
    }
}
