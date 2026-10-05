package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ItemConversationBinding
import noox.bzr.gallery.databinding.ScreenC18MessagesBinding

/** SCR-C18 (DEC-047): conversation list. */
class C18MessagesView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC18MessagesBinding.inflate(inflater, content)
    var onSelect: (Int) -> Unit = {}

    private data class Row(val index: Int, val name: String, val verified: Boolean, val order: String, val lastMessage: String, val time: String)

    private val adapter = RowAdapter<Row, MaterialCardView>(
        create = { parent -> ItemConversationBinding.inflate(inflater, parent, false).root },
        bind = { card, row, _ ->
            val item = ItemConversationBinding.bind(card)
            item.header.name = row.name
            item.header.rating = ""
            item.header.services = null
            item.header.verifiedLabel = if (row.verified) string(R.string.provider_verified) else null
            item.order.text = row.order
            item.lastMessage.text = row.lastMessage.ifBlank { string(R.string.messages_no_messages) }
            item.time.text = row.time
            card.setOnClickListener { onSelect(row.index) }
        },
        key = { it.index },
    )

    init {
        binding.conversations.rows(adapter, resources.getDimensionPixelSize(R.dimen.bremo_space_m))
    }

    override fun title(state: CustomerUiState) = string(R.string.messages_title)

    override fun renderContent(state: CustomerUiState) {
        val empty = state.phase == CustomerPhase.Empty
        binding.empty.isVisible = empty
        binding.conversations.isVisible = !empty && state.items.isNotEmpty()
        adapter.submitList(
            if (empty) emptyList() else state.items.indices.map { index ->
                Row(
                    index, state.items[index], !state.fieldErrors.getOrNull(index).isNullOrBlank(),
                    state.itemDetails.getOrElse(index) { "" }, state.options.getOrElse(index) { "" }, state.fieldValues.getOrElse(index) { "" },
                )
            },
        )
    }
}
