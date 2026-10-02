package noox.bzr.customer

import android.content.Context
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import noox.bzr.design.ChatKind
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ItemChatMessageBinding
import noox.bzr.gallery.databinding.ScreenC19ChatBinding

/** SCR-C19 (DEC-047): conversation with masking notice, images, read-only state. */
class C19ChatView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC19ChatBinding.inflate(inflater, content)
    var onDraftChange: (String) -> Unit = {}
    var onSend: () -> Unit = {}
    var onAddPhoto: () -> Unit = {}

    private data class Row(val index: Int, val text: String, val descriptor: String, val time: String)

    private val adapter = RowAdapter<Row, LinearLayout>(
        create = { parent ->
            LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                dividerDrawable = ContextCompat.getDrawable(context, R.drawable.bremo_gap_xs)
                showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
                ItemChatMessageBinding.inflate(inflater, this)
            }
        },
        bind = { view, row, _ ->
            val item = ItemChatMessageBinding.bind(view)
            item.bubble.text = row.text.ifBlank { if (row.descriptor.endsWith("_image")) string(R.string.chat_image) else "" }
            item.bubble.sent = row.descriptor.startsWith("sent_")
            item.bubble.kind = when {
                row.descriptor.endsWith("_image") -> ChatKind.Image
                row.descriptor.endsWith("_blocked") -> ChatKind.Blocked
                else -> ChatKind.Text
            }
            item.time.text = row.time
        },
        key = { it.index },
    )

    init {
        binding.header.rating = ""
        binding.header.services = null
        binding.messages.rows(adapter, resources.getDimensionPixelSize(R.dimen.bremo_space_m))
        binding.input.onValueChange = { onDraftChange(it) }
        binding.input.onSend = { onSend() }
        binding.addPhoto.onClick = { onAddPhoto() }
    }

    override fun title(state: CustomerUiState) = state.options.firstOrNull().orEmpty().ifBlank { string(R.string.messages_title) }

    override fun renderContent(state: CustomerUiState) {
        binding.header.name = state.options.firstOrNull().orEmpty()
        binding.header.verifiedLabel = string(R.string.provider_verified).takeUnless {
            state.options.getOrElse(3) { "CUSTOMER" } == "PROVIDER"
        }
        binding.subtitle.text = state.options.getOrElse(1) { "" }
        binding.masking.isVisible = state.messageKey == "chat.masking.notice"
        binding.empty.isVisible = state.phase == CustomerPhase.Empty
        binding.messages.isVisible = state.items.isNotEmpty()
        adapter.submitList(
            state.items.indices.map { index ->
                Row(index, state.items[index], state.itemStates.getOrElse(index) { "received_text" }, state.itemDetails.getOrElse(index) { "" })
            },
        )
        val readOnly = state.options.getOrElse(2) { "" } == "READ_ONLY"
        binding.readOnly.isVisible = readOnly
        binding.input.isVisible = !readOnly
        binding.addPhoto.isVisible = !readOnly
        binding.input.value = state.fieldValues.firstOrNull().orEmpty()
    }
}
