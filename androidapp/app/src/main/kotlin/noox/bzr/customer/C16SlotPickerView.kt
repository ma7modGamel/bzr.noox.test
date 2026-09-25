package noox.bzr.customer

import android.content.Context
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import noox.bzr.design.R
import noox.bzr.design.SelectionState
import noox.bzr.design.views.SelectableChipView
import noox.bzr.gallery.databinding.ScreenC16SlotsBinding

/** SCR-C16 (DEC-047): day chips (horizontal) and time slots (two columns). */
class C16SlotPickerView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC16SlotsBinding.inflate(inflater, content)
    var onDaySelect: (Int) -> Unit = {}
    var onSlotSelect: (Int) -> Unit = {}
    var onConfirm: () -> Unit = {}

    private data class Chip(val index: Int, val label: String, val selected: Boolean)

    private val days = RowAdapter<Chip, SelectableChipView>(
        create = { parent ->
            SelectableChipView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        },
        bind = { chip, row, _ -> chip.render(row); chip.setOnClickListener { onDaySelect(row.index) } },
        key = { it.index },
    )
    private val slots = RowAdapter<Chip, SelectableChipView>(
        create = { parent ->
            SelectableChipView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        },
        bind = { chip, row, _ -> chip.render(row); chip.setOnClickListener { onSlotSelect(row.index) } },
        key = { it.index },
    )

    init {
        binding.days.rows(days, resources.getDimensionPixelSize(R.dimen.bremo_space_s), horizontal = true)
        binding.slots.rows(slots, resources.getDimensionPixelSize(R.dimen.bremo_space_m), columns = 2)
        binding.confirm.onClick = { onConfirm() }
    }

    override fun title(state: CustomerUiState) = string(R.string.slot_title)

    override fun renderContent(state: CustomerUiState) {
        days.submitList(state.options.mapIndexed { index, day -> Chip(index, day, state.selectedOptionIndex == index) })
        binding.empty.isVisible = state.items.isEmpty()
        binding.slots.isVisible = state.items.isNotEmpty()
        slots.submitList(state.items.map { label -> state.items.indexOf(label) }.map { index -> Chip(index, state.items[index], state.selectedIndex == index) })
        binding.confirm.state = continueState(state)
    }

    private fun SelectableChipView.render(row: Chip) {
        text = row.label
        state = if (row.selected) SelectionState.Selected else SelectionState.Unselected
    }
}
