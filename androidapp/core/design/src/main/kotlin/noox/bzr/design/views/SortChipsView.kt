package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import com.google.android.material.chip.Chip
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewSortChipsBinding

/** 43 §3 SortChips on a single-selection ChipGroup; scrolls when the chips do not fit (C06 at 400dp). */
class SortChipsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : HorizontalScrollView(context, attrs) {
    private val binding = ViewSortChipsBinding.inflate(inflater, this)
    private var syncing = false

    var title: String? = null
        set(value) {
            field = value
            binding.title.text = value.orEmpty()
            binding.title.showIf(value != null)
        }
    var labels: List<String> = emptyList()
        set(value) {
            field = value
            render()
        }
    var selectedIndex: Int = 0
        set(value) {
            field = value
            render()
        }
    var onSelect: (Int) -> Unit = {}

    init {
        isHorizontalScrollBarEnabled = false
        isFillViewport = true
        binding.chips.setOnCheckedStateChangeListener { group, checked ->
            if (syncing) return@setOnCheckedStateChangeListener
            val index = checked.firstOrNull()?.let { id -> (0 until group.childCount).firstOrNull { group.getChildAt(it).id == id } }
            if (index != null && index != selectedIndex) onSelect(index)
        }
    }

    private fun render() {
        syncing = true
        val group = binding.chips
        group.removeAllViews()
        labels.forEachIndexed { index, label ->
            val chip = Chip(context, null, com.google.android.material.R.attr.chipStyle).apply {
                id = generateViewId()
                text = label
                isCheckable = true
                maxLines = 1
                chipMinHeight = resources.getDimension(R.dimen.bremo_size_touch_target_min)
                minimumHeight = px(R.dimen.bremo_size_touch_target_min)
            }
            group.addView(chip, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            styleSelectable(chip, index == selectedIndex)
            if (index == selectedIndex) group.check(chip.id)
        }
        syncing = false
    }
}
