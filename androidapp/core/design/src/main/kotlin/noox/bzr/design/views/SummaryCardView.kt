package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import com.google.android.material.card.MaterialCardView
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewSummaryCardBinding
import noox.bzr.design.databinding.ViewSummaryRowBinding

/** 43 §3 SummaryCard: title, optional "edit" pill, rows of (icon, text) split by lines. */
class SummaryCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    MaterialCardView(context, attrs, com.google.android.material.R.attr.materialCardViewStyle) {
    private val binding = ViewSummaryCardBinding.inflate(inflater, this)
    private val rowViews = mutableListOf<View>()

    var title: String = ""
        set(value) {
            field = value
            binding.title.text = value
        }
    var rows: List<Pair<Int, String>> = emptyList()
        set(value) {
            field = value
            renderRows()
        }
    var editText: String? = null
        set(value) {
            field = value
            binding.edit.text = value.orEmpty()
            binding.edit.showIf(value != null)
        }
    var onEdit: () -> Unit = {}

    init {
        val padding = px(R.dimen.bremo_space_card_padding)
        setContentPadding(padding, padding, padding, padding)
        binding.edit.setOnClickListener { onEdit() }
        context.withStyledAttributes(attrs, R.styleable.SummaryCardView) {
            title = getString(R.styleable.SummaryCardView_bremoTitle).orEmpty()
            editText = getString(R.styleable.SummaryCardView_editText)
        }
    }

    private fun renderRows() {
        rowViews.forEach { binding.content.removeView(it) }
        rowViews.clear()
        rows.forEachIndexed { index, (icon, text) ->
            if (index > 0) {
                val line = View(context).apply { setBackgroundResource(R.color.bremo_border) }
                binding.content.addView(line, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(R.dimen.bremo_border_width)))
                rowViews += line
            }
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                gap(R.drawable.bremo_gap_m)
            }
            val rowBinding = ViewSummaryRowBinding.inflate(inflater, row)
            rowBinding.icon.icon(icon, R.color.bremo_primary600)
            rowBinding.text.text = text
            binding.content.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            rowViews += row
        }
    }
}
