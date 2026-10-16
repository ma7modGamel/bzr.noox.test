package noox.bzr.customer

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.BzrFormat
import noox.bzr.design.R
import noox.bzr.design.views.AppBottomSheetView
import noox.bzr.gallery.databinding.ItemAddressBinding
import noox.bzr.gallery.databinding.ScreenC14AddressesBinding

/** Saved address cards keep primary status separate from selection for the current request. */
class C14AddressesView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC14AddressesBinding.inflate(inflater, content)
    private val sheet = AppBottomSheetView(context)
    private var busy = false
    private val expanded = mutableSetOf<String>()
    var onSelect: (Int) -> Unit = {}
    var onEdit: (Int) -> Unit = {}
    var onDelete: (Int) -> Unit = {}
    var onMakeDefault: (Int) -> Unit = {}
    var onCancelDelete: () -> Unit = {}
    var onConfirmDelete: () -> Unit = {}
    var onAdd: () -> Unit = {}

    private data class Row(
        val id: String, val index: Int, val label: String, val detail: String,
        val primary: Boolean, val selected: Boolean, val selectionMode: Boolean,
        val busy: Boolean, val pending: Boolean, val actions: List<CustomerOption>,
    )

    private val adapter = RowAdapter<Row, LinearLayout>(
        create = { parent -> LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            ItemAddressBinding.inflate(inflater, this)
        } },
        bind = { view, row, _ ->
            val item = ItemAddressBinding.bind(view)
            item.card.isSelected = row.primary
            item.card.isActivated = row.selected && row.selectionMode
            item.card.isEnabled = !row.busy
            item.label.text = row.label
            item.primaryBadge.isVisible = row.primary
            item.details.text = row.detail
            item.details.maxLines = if (row.id in expanded) Int.MAX_VALUE else if (resources.configuration.fontScale > 1.3f) 4 else 2
            item.locationIcon.setImageResource(if (row.selectionMode && row.selected) R.drawable.ic_check else R.drawable.ic_location)
            item.card.contentDescription = listOf(row.label, if (row.primary) string(R.string.address_default_badge) else "", row.detail).filter(String::isNotBlank).joinToString(". ")
            ViewCompat.setAccessibilityDelegate(item.card, object : androidx.core.view.AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(host: View, info: androidx.core.view.accessibility.AccessibilityNodeInfoCompat) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.hintText = if (row.selectionMode) BzrFormat.fill(string(R.string.address_use_for), "label" to row.label) else string(R.string.address_details)
                    info.isCheckable = row.selectionMode
                    info.isChecked = row.selectionMode && row.selected
                    if (row.selectionMode) info.className = "android.widget.RadioButton"
                }
            })
            item.card.setOnClickListener {
                if (!row.busy) {
                    if (row.selectionMode) onSelect(row.index) else {
                        if (!expanded.add(row.id)) expanded.remove(row.id)
                        item.details.maxLines = if (row.id in expanded) Int.MAX_VALUE else 2
                    }
                }
            }
            item.actions.icon = R.drawable.ic_more_horizontal
            item.actions.label = BzrFormat.fill(string(R.string.address_actions_for), "label" to row.label)
            item.actions.isVisible = row.actions.isNotEmpty() && !row.pending
            item.actions.alpha = if (row.busy) 0.5f else 1f
            item.actions.findViewById<View>(R.id.button).isEnabled = !row.busy
            item.actions.onClick = {
                if (!row.busy) PopupMenu(context, item.actions).apply {
                    row.actions.forEachIndexed { actionIndex, action -> menu.add(0, actionIndex, actionIndex, action.label) }
                    setOnMenuItemClickListener { selected ->
                        when (row.actions.getOrNull(selected.itemId)?.code) {
                            "edit" -> onEdit(row.index)
                            "set_default" -> onMakeDefault(row.index)
                            "delete" -> onDelete(row.index)
                        }
                        true
                    }
                    show()
                }
            }
            item.progress.isVisible = row.pending
        },
        key = Row::id,
    )

    init {
        binding.list.rows(adapter, resources.getDimensionPixelSize(R.dimen.bremo_space_s))
        binding.add.onClick = { if (!busy) onAdd() }
        binding.retry.onClick = { if (!busy) onReload?.invoke() }
        pinAction(binding.add)
        overlay.layoutParams = (overlay.layoutParams as FrameLayout.LayoutParams).apply { height = ViewGroup.LayoutParams.MATCH_PARENT }
        overlay.setBackgroundResource(R.drawable.bremo_bg_scrim)
        overlay.setOnClickListener { if (!busy) onCancelDelete() }
        sheet.isClickable = true
        sheet.action = string(R.string.address_delete_confirm_action)
        sheet.secondaryAction = string(R.string.common_cancel)
        sheet.onAction = { if (!busy) onConfirmDelete() }
        sheet.onSecondary = { if (!busy) onCancelDelete() }
        overlay.addView(sheet, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
    }

    override fun title(state: CustomerUiState) = string(R.string.address_list_title)

    override fun renderContent(state: CustomerUiState) {
        busy = state.isBusy
        binding.heading.isVisible = state.items.isNotEmpty()
        binding.heading.text = BzrFormat.quantity(context, R.plurals.plural_account_addresses_count, state.items.size)
        binding.body.text = string(if (state.selectionMode) R.string.address_list_selection_body else R.string.address_list_body)
        binding.noPrimary.isVisible = state.items.isNotEmpty() && state.primaryAddressIndex < 0
        binding.empty.isVisible = state.items.isEmpty()
        binding.list.isVisible = state.items.isNotEmpty()
        binding.message.isVisible = state.messageKey != null
        val failed = state.messageKey in setOf("address.delete.failed", "address.default.failed")
        binding.retry.isVisible = failed
        binding.message.setTextColor(context.getColor(if (failed) R.color.bremo_danger else R.color.bremo_success))
        binding.message.text = when (state.messageKey) {
            "address.delete.failed" -> string(R.string.address_delete_failed)
            "address.default.failed" -> string(R.string.address_default_failed)
            "address.default.saved" -> string(R.string.address_default_saved)
            "address.deleted" -> string(R.string.address_deleted)
            else -> ""
        }
        adapter.submitList(state.items.mapIndexed { index, label -> Row(
            id = state.optionCodes.getOrElse(index) { index.toString() }, index = index, label = label,
            detail = state.itemDetails.getOrElse(index) { "" }, primary = state.primaryAddressIndex == index,
            selected = state.selectedIndex == index, selectionMode = state.selectionMode,
            busy = state.isBusy, pending = state.isBusy && state.pendingIndex == index,
            actions = state.addressActions.getOrElse(index) { emptyList() },
        ) }.sortedByDescending(Row::primary))
        binding.add.state = if (state.isBusy) ButtonVisualState.Disabled else ButtonVisualState.Normal
    }

    override fun renderOverlay(state: CustomerUiState) {
        busy = state.isBusy
        overlay.isVisible = state.showConfirmation && state.phase != CustomerPhase.Error
        content.importantForAccessibility = if (overlay.isVisible) View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS else View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
        sheet.title = BzrFormat.fill(string(R.string.address_delete_named), "label" to state.items.getOrElse(state.pendingIndex) { "" })
        sheet.body = string(if (state.pendingIndex == state.primaryAddressIndex) R.string.address_delete_primary_body else R.string.address_delete_confirm_body)
        ViewCompat.setAccessibilityPaneTitle(sheet, sheet.title)
        sheet.actionState = if (state.isBusy) ButtonVisualState.Loading else ButtonVisualState.Normal
        sheet.findViewById<noox.bzr.design.views.SecondaryButtonView>(R.id.secondary).enabled = !state.isBusy
    }
}
