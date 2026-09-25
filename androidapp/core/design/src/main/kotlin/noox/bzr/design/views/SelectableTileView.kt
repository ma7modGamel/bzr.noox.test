package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.content.withStyledAttributes
import noox.bzr.design.R
import noox.bzr.design.SelectionState
import noox.bzr.design.databinding.ViewSelectableTileBinding

/** 43 §3 SelectableTile: category tile; selected, unselected, disabled. */
class SelectableTileView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val binding: ViewSelectableTileBinding

    var text: String = ""
        set(value) {
            field = value
            binding.text.text = value
        }

    @DrawableRes
    var icon: Int = R.drawable.ic_home
        set(value) {
            field = value
            render()
        }
    var state: SelectionState = SelectionState.Unselected
        set(value) {
            field = value
            render()
        }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        gap(R.drawable.bremo_gap_s)
        setBackgroundResource(R.drawable.bremo_bg_selectable_card)
        val padding = px(R.dimen.bremo_space_s)
        setPadding(padding, padding, padding, padding)
        binding = ViewSelectableTileBinding.inflate(inflater, this)
        context.withStyledAttributes(attrs, R.styleable.SelectableTileView) {
            text = getString(R.styleable.SelectableTileView_bremoText).orEmpty()
            resource(R.styleable.SelectableTileView_bremoIcon)?.let { icon = it }
            state = enumValue(R.styleable.SelectableTileView_selectionState, SelectionState.entries.toTypedArray(), SelectionState.Unselected)
        }
        render()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) =
        super.onMeasure(widthMeasureSpec, fixedHeight(px(R.dimen.bremo_size_category_tile_h)))

    private fun render() {
        isSelected = state == SelectionState.Selected
        isEnabled = state != SelectionState.Disabled
        applyEnabledAlpha(state != SelectionState.Disabled)
        binding.icon.icon(icon, if (isSelected) R.color.bremo_primary600 else R.color.bremo_navy800)
    }
}
