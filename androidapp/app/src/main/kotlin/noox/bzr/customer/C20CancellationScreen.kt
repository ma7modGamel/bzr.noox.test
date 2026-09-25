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
fun C20CancellationScreen(
    state: CustomerUiState,
    onReasonSelect: (Int) -> Unit = {},
    onNoteChange: (String) -> Unit = {},
    onConfirm: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    Box(Modifier.fillMaxSize()) {
        CustomerScreen(stringResource(R.string.cancel_title), state) {
            InfoBanner(stringResource(R.string.cancel_free))
            state.options.forEachIndexed { index, label ->
                Box(Modifier.clickable { onReasonSelect(index) }) {
                    RadioCard(label, stringResource(R.string.cancel_body), state.selectedOptionIndex == index)
                }
            }
            TextAreaField(
                stringResource(R.string.cancel_note_label), stringResource(R.string.cancel_note_placeholder),
                state.fieldValues.firstOrNull().orEmpty(),
                if (state.isBusy) FieldVisualState.Disabled else FieldVisualState.Empty,
                optionalLabel = stringResource(R.string.common_optional), onValueChange = onNoteChange,
            )
        }
        if (state.phase == CustomerPhase.Content) {
            Box(Modifier.align(Alignment.BottomCenter)) {
                AppBottomSheet(
                    title = stringResource(R.string.cancel_title), body = stringResource(R.string.cancel_free),
                    action = stringResource(R.string.cancel_confirm),
                    actionState = when { state.isBusy -> ButtonVisualState.Loading; state.canContinue -> ButtonVisualState.Normal; else -> ButtonVisualState.Disabled },
                    secondaryAction = stringResource(R.string.common_cancel),
                    onAction = onConfirm, onSecondary = onBack,
                )
            }
        }
    }
}
