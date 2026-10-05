package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.design.views.SummaryCardView
import noox.bzr.gallery.databinding.ScreenC32NotificationsBinding

/** SCR-C32 (DEC-047): notifications, unread marked; a settings link when the device blocks them (DEC-058). */
class C32NotificationsView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC32NotificationsBinding.inflate(inflater, content)
    var onOpen: (Int) -> Unit = {}
    var onOpenSettings: () -> Unit = {}

    private data class Row(val index: Int, val title: String, val body: String, val time: String, val unread: Boolean, val enabled: Boolean)

    private val adapter = RowAdapter<Row, SummaryCardView>(
        create = { parent -> SummaryCardView(parent.context) },
        bind = { card, row, _ ->
            card.title = row.title
            card.rows = listOf(R.drawable.ic_messages to row.body, R.drawable.ic_clock to row.time)
            card.editText = if (row.unread) string(R.string.notifications_unread) else null
            card.setOnClickListener(if (row.enabled) OnClickListener { onOpen(row.index) } else null)
            card.isClickable = row.enabled
        },
        key = { it.index },
    )

    init {
        binding.notifications.rows(adapter, resources.getDimensionPixelSize(R.dimen.bremo_space_m))
        binding.permissionOff.text = string(R.string.notifications_permission_off)
        binding.openSettings.onClick = { onOpenSettings() }
    }

    override fun title(state: CustomerUiState) = string(R.string.notifications_title)

    override fun renderContent(state: CustomerUiState) {
        binding.permissionOff.isVisible = state.notificationsDenied
        binding.openSettings.isVisible = state.notificationsDenied
        binding.empty.isVisible = state.items.isEmpty()
        binding.notifications.isVisible = state.items.isNotEmpty()
        adapter.submitList(
            state.items.mapIndexed { index, title ->
                Row(
                    index, title, state.itemDetails.getOrElse(index) { "" }, state.fieldValues.getOrElse(index) { "" },
                    state.itemStates.getOrNull(index) == "unread", !state.isBusy,
                )
            },
        )
    }
}
