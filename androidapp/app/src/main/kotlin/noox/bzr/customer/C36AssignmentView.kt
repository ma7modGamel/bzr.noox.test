package noox.bzr.customer

import android.content.Context
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenC36AssignmentBinding

/** SCR-C36 (employee mode): the published order waiting for assignment; edit and cancel from `available_actions`. */
class C36AssignmentView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC36AssignmentBinding.inflate(inflater, content)
    var onAction: (String) -> Unit = {}

    init {
        binding.actions.onAction = { onAction(it) }
    }

    override fun title(state: CustomerUiState) = string(R.string.assignment_waiting_title)

    override fun renderContent(state: CustomerUiState) {
        binding.summary.title = state.fieldValues.firstOrNull().orEmpty().ifBlank { string(R.string.assignment_summary_title) }
        binding.summary.rows = state.items.mapIndexed { index, row ->
            (if (index == 0) R.drawable.ic_orders else if (index == 1) R.drawable.ic_location else R.drawable.ic_calendar) to row
        }
        binding.actions.actions = state.visibleActions
    }
}
