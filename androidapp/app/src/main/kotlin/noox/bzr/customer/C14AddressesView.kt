package noox.bzr.customer

import android.content.Context
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.R
import noox.bzr.design.SelectionState
import noox.bzr.design.views.AppBottomSheetView
import noox.bzr.gallery.databinding.ItemAddressBinding
import noox.bzr.gallery.databinding.ScreenC14AddressesBinding

/** SCR-C14 (DEC-047): saved addresses, selection mode, delete confirmation sheet. */
class C14AddressesView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC14AddressesBinding.inflate(inflater, content)
    private val sheet = AppBottomSheetView(context)
    var onSelect: (Int) -> Unit = {}
    var onEdit: (Int) -> Unit = {}
    var onDelete: (Int) -> Unit = {}
    var onCancelDelete: () -> Unit = {}
    var onConfirmDelete: () -> Unit = {}
    var onAdd: () -> Unit = {}

    private data class Row(val index: Int, val label: String, val detail: String, val selectionMode: Boolean, val selected: Boolean)

    private val adapter = RowAdapter<Row, LinearLayout>(
        create = { parent ->
            LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                dividerDrawable = ContextCompat.getDrawable(context, R.drawable.bremo_gap_s)
                showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
                ItemAddressBinding.inflate(inflater, this)
            }
        },
        bind = { view, row, _ ->
            val item = ItemAddressBinding.bind(view)
            item.card.title = row.label
            item.card.rows = listOf(R.drawable.ic_location to row.detail)
            item.card.onEdit = { onEdit(row.index) }
            item.select.isVisible = row.selectionMode
            item.select.state = if (row.selected) SelectionState.Selected else SelectionState.Unselected
            item.select.setOnClickListener { onSelect(row.index) }
            item.delete.onClick = { onDelete(row.index) }
        },
        key = { it.index },
    )

    init {
        binding.list.rows(adapter, resources.getDimensionPixelSize(R.dimen.bremo_space_m))
        binding.add.onClick = { onAdd() }
        sheet.title = string(R.string.address_delete_confirm_title)
        sheet.body = string(R.string.address_delete_confirm_body)
        sheet.action = string(R.string.address_delete_confirm_action)
        sheet.secondaryAction = string(R.string.common_cancel)
        sheet.onAction = { onConfirmDelete() }
        sheet.onSecondary = { onCancelDelete() }
        overlay.addView(sheet)
    }

    override fun title(state: CustomerUiState) = string(R.string.address_list_title)

    override fun renderContent(state: CustomerUiState) {
        binding.empty.isVisible = state.items.isEmpty()
        binding.list.isVisible = state.items.isNotEmpty()
        adapter.submitList(
            state.items.mapIndexed { index, label ->
                Row(index, label, state.itemDetails.getOrElse(index) { "" }, state.selectionMode, state.selectedIndex == index)
            },
        )
        binding.add.state = if (state.isBusy) ButtonVisualState.Disabled else ButtonVisualState.Normal
    }

    override fun renderOverlay(state: CustomerUiState) {
        sheet.isVisible = state.showConfirmation
    }
}
