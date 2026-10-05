package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import noox.bzr.design.R
import noox.bzr.design.SelectionState
import noox.bzr.design.databinding.ViewSelectableChipBinding

/**
 * 43 §3 SelectableChip: selected, unselected, disabled. Chips inside a row share its width with centred
 * text, which Material Chip does not allow (it forces start gravity), so this one is a framed TextView;
 * SortChips uses Material Chip/ChipGroup.
 */
class SelectableChipView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val binding = ViewSelectableChipBinding.inflate(inflater, this)

    var text: String = ""
        set(value) {
            field = value
            binding.chip.text = value
            contentDescription = value
        }
    var state: SelectionState = SelectionState.Unselected
        set(value) {
            field = value
            render()
        }

    @get:JvmName("chipHeight")
    @set:JvmName("setChipHeight")
    var height: Int = px(R.dimen.bremo_size_chip_height)
        set(value) {
            field = value
            binding.chip.minimumHeight = maxOf(value, px(R.dimen.bremo_size_touch_target_min))
            binding.chip.layoutParams = binding.chip.layoutParams.apply { this.height = LayoutParams.WRAP_CONTENT }
            requestLayout()
        }

    init {
        context.withStyledAttributes(attrs, R.styleable.SelectableChipView) {
            text = getString(R.styleable.SelectableChipView_bremoText).orEmpty()
            state = enumValue(R.styleable.SelectableChipView_selectionState, SelectionState.entries.toTypedArray(), SelectionState.Unselected)
            height = getDimensionPixelSize(R.styleable.SelectableChipView_bremoHeight, height)
        }
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        binding.chip.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        isFocusable = true
        render()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        minimumHeight = maxOf(height, px(R.dimen.bremo_size_touch_target_min))
        // In a TileGridLayout row every chip gets the row height, so the frames line up (DEC-063).
        val fill = MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY
        val wanted = if (fill) LayoutParams.MATCH_PARENT else LayoutParams.WRAP_CONTENT
        if (binding.chip.layoutParams.height != wanted) binding.chip.layoutParams = binding.chip.layoutParams.apply { this.height = wanted }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = Button::class.java.name
        info.contentDescription = text
        info.isCheckable = true
        info.isChecked = state == SelectionState.Selected
    }

    private fun render() {
        val selected = state == SelectionState.Selected
        val enabled = state != SelectionState.Disabled
        isSelected = selected
        isEnabled = enabled
        contentDescription = text
        binding.chip.isSelected = selected
        binding.chip.isEnabled = enabled
        applyEnabledAlpha(enabled)
    }
}

/** Selected chips get the 1.5 selection stroke (DesignBorder.selectedWidth), as in Compose and SwiftUI. */
internal fun styleSelectable(chip: com.google.android.material.chip.Chip, selected: Boolean) {
    chip.isSelected = selected
    chip.isChecked = selected
    chip.chipStrokeWidth = chip.resources.getDimension(if (selected) R.dimen.bremo_border_selected_width else R.dimen.bremo_border_width)
}
