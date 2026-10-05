package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewCheckRowBinding

/** 43 §3 CheckRow: checked or unchecked. */
class CheckRowView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : BremoLinearLayout(context, attrs) {
    private val binding: ViewCheckRowBinding

    var text: String = ""
        set(value) {
            field = value
            binding.text.text = value
        }
    var checked: Boolean = false
        set(value) {
            field = value
            render()
        }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        gap(R.drawable.bremo_gap_m)
        val padding = px(R.dimen.bremo_space_m)
        setPadding(padding, padding, padding, padding)
        binding = ViewCheckRowBinding.inflate(inflater, this)
        context.withStyledAttributes(attrs, R.styleable.CheckRowView) {
            text = getString(R.styleable.CheckRowView_bremoText).orEmpty()
            checked = getBoolean(R.styleable.CheckRowView_bremoChecked, false)
        }
        render()
    }

    private fun render() {
        setBackgroundResource(if (checked) R.drawable.bremo_bg_check_row_checked else R.drawable.bremo_bg_check_row)
        binding.box.setBackgroundResource(if (checked) R.drawable.bremo_bg_checkbox_checked else R.drawable.bremo_bg_checkbox)
        binding.check.showIf(checked)
    }
}
