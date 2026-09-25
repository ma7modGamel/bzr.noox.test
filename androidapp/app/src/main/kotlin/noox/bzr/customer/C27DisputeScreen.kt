package noox.bzr.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import noox.bzr.design.Badge
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.FieldVisualState
import noox.bzr.design.InfoBanner
import noox.bzr.design.MediaKind
import noox.bzr.design.MediaState
import noox.bzr.design.MediaThumb
import noox.bzr.design.PrimaryButton
import noox.bzr.design.R
import noox.bzr.design.RadioCard
import noox.bzr.design.SecondaryButton
import noox.bzr.design.SummaryCard
import noox.bzr.design.TextAreaField

@Composable
fun C27DisputeScreen(
    state: CustomerUiState,
    onReasonSelect: (Int) -> Unit = {},
    onDescriptionChange: (String) -> Unit = {},
    onAddPhoto: () -> Unit = {},
    onDeletePhoto: (Int) -> Unit = {},
    onSubmit: () -> Unit = {},
) {
    CustomerScreen(stringResource(R.string.dispute_title), state) {
        if (state.messageKey == "dispute.success") {
            InfoBanner(stringResource(R.string.dispute_success))
        } else if (state.items.isNotEmpty()) {
            Badge(state.itemStates.firstOrNull().orEmpty())
            SummaryCard(
                stringResource(R.string.dispute_existing_title),
                state.items.map { R.drawable.ic_info to it },
                null,
            )
            state.itemStates.getOrNull(1)?.takeIf(String::isNotBlank)?.let { InfoBanner(it) }
        } else {
            ScreenHeading(stringResource(R.string.dispute_reason_title), stringResource(R.string.dispute_body))
            state.options.forEachIndexed { index, label ->
                Box(Modifier.clickable(enabled = !state.isBusy) { onReasonSelect(index) }) {
                    RadioCard(label, stringResource(R.string.payment_option_body), state.selectedOptionIndex == index)
                }
            }
            TextAreaField(
                stringResource(R.string.dispute_description_label),
                stringResource(R.string.dispute_description_placeholder),
                state.fieldValues.firstOrNull().orEmpty(),
                if (state.isBusy) FieldVisualState.Disabled else FieldVisualState.Empty,
                onValueChange = onDescriptionChange,
            )
            SecondaryButton(stringResource(R.string.dispute_photo_add), onClick = onAddPhoto)
            if (state.hasPhoto) {
                MediaThumb(
                    stringResource(R.string.dispute_photo_title),
                    stringResource(R.string.media_uploaded),
                    MediaKind.Photo,
                    MediaState.Uploaded,
                    onDelete = { onDeletePhoto(0) },
                )
            }
            state.descriptionError?.let { InfoBanner(stringResource(R.string.request_description_max)) }
            if ("open_dispute" in state.visibleActions) {
                PrimaryButton(
                    stringResource(R.string.dispute_submit),
                    when {
                        state.isBusy -> ButtonVisualState.Loading
                        state.canContinue -> ButtonVisualState.Normal
                        else -> ButtonVisualState.Disabled
                    },
                    onClick = onSubmit,
                )
            }
        }
    }
}
