package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.FieldVisualState
import noox.bzr.design.R
import noox.bzr.design.views.AppBottomSheetView
import noox.bzr.gallery.databinding.ScreenC20CancellationBinding

/** SCR-C20 (DEC-047): cancellation reasons, optional note, confirmation sheet. */
class C20CancellationView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC20CancellationBinding.inflate(inflater, content)
    private val sheet = AppBottomSheetView(context)
    var onReasonSelect: (Int) -> Unit = {}
    var onNoteChange: (String) -> Unit = {}
    var onConfirm: () -> Unit = {}
    var onBackAction: () -> Unit = {}
    private val reasons = RadioOptions(binding.reasons, resources.getDimensionPixelSize(R.dimen.bremo_space_m), string(R.string.cancel_body)) { onReasonSelect(it) }

    init {
        binding.note.onValueChange = { onNoteChange(it) }
        sheet.title = string(R.string.cancel_title)
        sheet.body = string(R.string.cancel_free)
        sheet.action = string(R.string.cancel_confirm)
        sheet.secondaryAction = string(R.string.common_cancel)
        sheet.onAction = { onConfirm() }
        sheet.onSecondary = { onBackAction() }
        overlay.addView(sheet)
    }

    override fun title(state: CustomerUiState) = string(R.string.cancel_title)

    override fun renderContent(state: CustomerUiState) {
        binding.reasons.isVisible = state.options.isNotEmpty()
        reasons.submit(state.options, state.selectedOptionIndex)
        binding.note.value = state.fieldValues.firstOrNull().orEmpty()
        binding.note.state = if (state.isBusy) FieldVisualState.Disabled else FieldVisualState.Empty
    }

    override fun renderOverlay(state: CustomerUiState) {
        sheet.isVisible = state.phase == CustomerPhase.Content
        sheet.actionState = when {
            state.isBusy -> ButtonVisualState.Loading
            state.canContinue -> ButtonVisualState.Normal
            else -> ButtonVisualState.Disabled
        }
    }
}
