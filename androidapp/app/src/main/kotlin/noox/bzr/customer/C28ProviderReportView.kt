package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.FieldVisualState
import noox.bzr.design.R
import noox.bzr.design.views.AppBottomSheetView
import noox.bzr.gallery.databinding.ScreenC28ProviderReportBinding

/** SCR-C28 (DEC-047): report a provider; the submit sheet stays at the bottom until success. */
class C28ProviderReportView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC28ProviderReportBinding.inflate(inflater, content)
    private val sheet = AppBottomSheetView(context)
    var onReasonSelect: (Int) -> Unit = {}
    var onDescriptionChange: (String) -> Unit = {}
    var onSubmit: () -> Unit = {}
    var onBackAction: () -> Unit = {}
    private val reasons = RadioOptions(binding.reasons, resources.getDimensionPixelSize(R.dimen.bremo_space_m), string(R.string.payment_option_body)) { onReasonSelect(it) }

    init {
        binding.description.onValueChange = { onDescriptionChange(it) }
        sheet.title = string(R.string.provider_report_title)
        sheet.body = string(R.string.provider_report_body)
        sheet.action = string(R.string.provider_report_submit)
        sheet.secondaryAction = string(R.string.common_cancel)
        sheet.onAction = { onSubmit() }
        sheet.onSecondary = { onBackAction() }
        overlay.addView(sheet)
    }

    override fun title(state: CustomerUiState) = string(R.string.provider_report_title)

    override fun renderContent(state: CustomerUiState) {
        val success = state.messageKey == "provider.report.success"
        binding.intro.text = string(if (success) R.string.provider_report_success else R.string.provider_report_body)
        binding.reasonTitle.isVisible = !success
        binding.reasons.isVisible = !success && state.options.isNotEmpty()
        reasons.submit(state.options, state.selectedOptionIndex, !state.isBusy)
        binding.description.isVisible = !success
        binding.description.value = state.fieldValues.firstOrNull().orEmpty()
        binding.description.state = if (state.isBusy) FieldVisualState.Disabled else FieldVisualState.Empty
    }

    override fun renderOverlay(state: CustomerUiState) {
        sheet.isVisible = state.messageKey != "provider.report.success"
        sheet.actionState = when {
            state.isBusy -> ButtonVisualState.Loading
            state.canContinue -> ButtonVisualState.Normal
            else -> ButtonVisualState.Disabled
        }
    }
}
