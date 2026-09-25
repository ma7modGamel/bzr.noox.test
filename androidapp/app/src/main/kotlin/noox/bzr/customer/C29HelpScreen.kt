package noox.bzr.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import noox.bzr.design.AppTextField
import noox.bzr.design.FieldVisualState
import noox.bzr.design.InfoBanner
import noox.bzr.design.PrimaryButton
import noox.bzr.design.R
import noox.bzr.design.SummaryCard
import noox.bzr.design.TextAreaField

@Composable
fun C29HelpScreen(
    state: CustomerUiState,
    onExpand: (Int) -> Unit = {},
    onSubjectChange: (String) -> Unit = {},
    onMessageChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
) {
    CustomerScreen(stringResource(R.string.help_title), state) {
        ScreenHeading(stringResource(R.string.help_faq_title))
        state.items.forEachIndexed { index, question ->
            Box(Modifier.clickable { onExpand(index) }) {
                SummaryCard(
                    question,
                    if (state.selectedIndex == index) listOf(R.drawable.ic_info to state.itemDetails.getOrElse(index) { "" }) else emptyList(),
                    null,
                )
            }
        }
        ScreenHeading(stringResource(R.string.help_contact_title))
        if (state.messageKey == "help.success") InfoBanner(stringResource(R.string.help_success))
        AppTextField(
            stringResource(R.string.help_subject), stringResource(R.string.help_subject_placeholder),
            state.fieldValues.getOrElse(0) { "" }, fieldState(state, "help.validation.subject"),
            state.fieldErrors.firstOrNull { it == "help.validation.subject" }?.let { stringResource(R.string.help_validation_subject) },
            onValueChange = onSubjectChange,
        )
        TextAreaField(
            stringResource(R.string.help_message), stringResource(R.string.help_message_placeholder),
            state.fieldValues.getOrElse(1) { "" }, fieldState(state, "help.validation.message"),
            state.fieldErrors.firstOrNull { it == "help.validation.message" }?.let { stringResource(R.string.help_validation_message) },
            onValueChange = onMessageChange,
        )
        PrimaryButton(
            stringResource(R.string.help_send),
            state = buttonState(state),
            onClick = onSubmit,
        )
    }
}

private fun fieldState(state: CustomerUiState, key: String): FieldVisualState = when {
    state.isBusy -> FieldVisualState.Disabled
    key in state.fieldErrors -> FieldVisualState.Error
    else -> FieldVisualState.Empty
}
