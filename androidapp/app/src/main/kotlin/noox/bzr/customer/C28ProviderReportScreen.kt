package noox.bzr.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import noox.bzr.design.AppBottomSheet
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.FieldVisualState
import noox.bzr.design.InfoBanner
import noox.bzr.design.R
import noox.bzr.design.RadioCard
import noox.bzr.design.TextAreaField

@Composable
fun C28ProviderReportScreen(
    state: CustomerUiState,
    onReasonSelect: (Int) -> Unit = {},
    onDescriptionChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    Box(Modifier.fillMaxSize()) {
        CustomerScreen(stringResource(R.string.provider_report_title), state) {
            InfoBanner(
                if (state.messageKey == "provider.report.success") {
                    stringResource(R.string.provider_report_success)
                } else {
                    stringResource(R.string.provider_report_body)
                },
            )
            if (state.messageKey != "provider.report.success") {
                ScreenHeading(stringResource(R.string.provider_report_reason_title))
                state.options.forEachIndexed { index, label ->
                    Box(Modifier.clickable(enabled = !state.isBusy) { onReasonSelect(index) }) {
                        RadioCard(label, stringResource(R.string.payment_option_body), state.selectedOptionIndex == index)
                    }
                }
                TextAreaField(
                    stringResource(R.string.provider_report_description_label),
                    stringResource(R.string.provider_report_description_placeholder),
                    state.fieldValues.firstOrNull().orEmpty(),
                    if (state.isBusy) FieldVisualState.Disabled else FieldVisualState.Empty,
                    optionalLabel = stringResource(R.string.common_optional),
                    onValueChange = onDescriptionChange,
                )
            }
        }
        if (state.messageKey != "provider.report.success") {
            Box(Modifier.align(Alignment.BottomCenter)) {
                AppBottomSheet(
                    title = stringResource(R.string.provider_report_title),
                    body = stringResource(R.string.provider_report_body),
                    action = stringResource(R.string.provider_report_submit),
                    actionState = when {
                        state.isBusy -> ButtonVisualState.Loading
                        state.canContinue -> ButtonVisualState.Normal
                        else -> ButtonVisualState.Disabled
                    },
                    secondaryAction = stringResource(R.string.common_cancel),
                    onAction = onSubmit,
                    onSecondary = onBack,
                )
            }
        }
    }
}
