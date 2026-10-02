package noox.bzr.provider

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.customer.RadioOptions
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.FieldVisualState
import noox.bzr.design.R as DesignR
import noox.bzr.design.views.AppBottomSheetView
import noox.bzr.gallery.databinding.ScreenP21UnableBinding

class P21UnableView(context: Context) : ProviderScreenView(context) {
    private val binding = ScreenP21UnableBinding.inflate(inflater, content)
    private val sheet = AppBottomSheetView(context)
    private val reasons = RadioOptions(
        binding.reasons,
        resources.getDimensionPixelSize(DesignR.dimen.bremo_space_m),
        "",
    ) { onReasonSelect(it) }

    var onReasonSelect: (Int) -> Unit = {}
    var onNoteChange: (String) -> Unit = {}
    var onConfirm: () -> Unit = {}

    init {
        binding.note.onValueChange = { onNoteChange(it) }
        sheet.body = ""
        sheet.action = string(DesignR.string.provider_unable_confirm)
        sheet.secondaryAction = string(DesignR.string.common_cancel)
        sheet.onAction = { onConfirm() }
        sheet.onSecondary = { onBack() }
        sheet.isVisible = false
        overlay.addView(sheet)
    }

    override fun title(): String = string(DesignR.string.provider_unable_title)

    override fun renderContent(state: ProviderUiState) {
        val noShow = state.currentAction == "report_no_show"
        binding.reasons.isVisible = !noShow
        reasons.submit(state.options, state.selectedOptionIndex, !state.isBusy)
        binding.note.isVisible = state.currentAction == "report_unable"
        binding.note.value = state.fieldValues.firstOrNull().orEmpty()
        val noteError = "provider.unable.note.invalid" in state.fieldErrors
        binding.note.state = if (noteError) FieldVisualState.Error else FieldVisualState.Filled
        binding.note.error = if (noteError) string(DesignR.string.provider_unable_note_invalid) else null
        binding.warning.isVisible = state.messageKey != null || state.fieldErrors.isNotEmpty()
        binding.warning.text = string(
            when {
                "provider.unable.reason.required" in state.fieldErrors -> DesignR.string.provider_unable_reason_required
                state.messageKey == "provider.no_show.not_ready" -> DesignR.string.provider_no_show_not_ready
                state.messageKey == "provider.no_show.confirm" -> DesignR.string.provider_no_show_confirm
                else -> DesignR.string.error_body
            },
        )
        sheet.isVisible = true
        sheet.title = string(
            when (state.currentAction) {
                "back_out" -> DesignR.string.provider_back_out_title
                "report_no_show" -> DesignR.string.provider_no_show_title
                else -> DesignR.string.provider_unable_title
            },
        )
        sheet.body = string(
            if (noShow) DesignR.string.provider_no_show_confirm else DesignR.string.provider_unable_reason,
        )
        sheet.actionState = when {
            state.isBusy -> ButtonVisualState.Loading
            state.canContinue -> ButtonVisualState.Normal
            else -> ButtonVisualState.Disabled
        }
    }
}
