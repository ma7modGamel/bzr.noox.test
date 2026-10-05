package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.FieldVisualState
import noox.bzr.design.R
import noox.bzr.design.views.SummaryCardView
import noox.bzr.gallery.databinding.ScreenC29HelpBinding

/** SCR-C29 (DEC-047): FAQ (expandable) and contact form. */
class C29HelpView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC29HelpBinding.inflate(inflater, content)
    var onExpand: (Int) -> Unit = {}
    var onSubjectChange: (String) -> Unit = {}
    var onMessageChange: (String) -> Unit = {}
    var onSubmit: () -> Unit = {}

    private data class Row(val index: Int, val question: String, val answer: String?)

    private val adapter = RowAdapter<Row, SummaryCardView>(
        create = { parent -> SummaryCardView(parent.context).apply { editText = null } },
        bind = { card, row, _ ->
            card.title = row.question
            card.rows = row.answer?.let { listOf(R.drawable.ic_info to it) } ?: emptyList()
            card.setOnClickListener { onExpand(row.index) }
        },
        key = { it.index },
    )

    init {
        binding.faq.rows(adapter, resources.getDimensionPixelSize(R.dimen.bremo_space_m))
        binding.subject.onValueChange = { onSubjectChange(it) }
        binding.message.onValueChange = { onMessageChange(it) }
        binding.send.onClick = { onSubmit() }
    }

    override fun title(state: CustomerUiState) = string(R.string.help_title)

    override fun renderContent(state: CustomerUiState) {
        binding.faq.isVisible = state.items.isNotEmpty()
        adapter.submitList(
            state.items.mapIndexed { index, question ->
                Row(index, question, if (state.selectedIndex == index) state.itemDetails.getOrElse(index) { "" } else null)
            },
        )
        binding.success.isVisible = state.messageKey == "help.success"
        binding.subject.value = state.fieldValues.getOrElse(0) { "" }
        binding.subject.state = fieldState(state, "help.validation.subject")
        binding.subject.error = if ("help.validation.subject" in state.fieldErrors) string(R.string.help_validation_subject) else null
        binding.message.value = state.fieldValues.getOrElse(1) { "" }
        binding.message.state = fieldState(state, "help.validation.message")
        binding.message.error = if ("help.validation.message" in state.fieldErrors) string(R.string.help_validation_message) else null
        binding.send.state = customerButtonState(state)
    }

    private fun fieldState(state: CustomerUiState, key: String): FieldVisualState = when {
        state.isBusy -> FieldVisualState.Disabled
        key in state.fieldErrors -> FieldVisualState.Error
        else -> FieldVisualState.Empty
    }
}
