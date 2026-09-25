package noox.bzr.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.FieldVisualState
import noox.bzr.design.MediaKind
import noox.bzr.design.MediaState
import noox.bzr.design.MediaThumb
import noox.bzr.design.PrimaryButton
import noox.bzr.design.R
import noox.bzr.design.SecondaryButton
import noox.bzr.design.SelectionState
import noox.bzr.design.SelectableTile
import noox.bzr.design.TextAreaField

@Composable
fun C03ProblemScreen(
    state: CustomerUiState,
    onProblemSelect: () -> Unit = {},
    onOpen: (String) -> Unit = {},
) {
    CustomerScreen(stringResource(R.string.request_problem_title), state) {
        TwoColumns(
            { Box(Modifier.clickable(onClick = onProblemSelect)) { SelectableTile(stringResource(R.string.request_problem_leak), R.drawable.ic_home, SelectionState.Selected) } },
            { SelectableTile(stringResource(R.string.request_problem_blockage), R.drawable.ic_warning, SelectionState.Unselected) },
        )
        TwoColumns(
            { SelectableTile(stringResource(R.string.request_problem_installation), R.drawable.ic_edit, SelectionState.Unselected) },
            { SelectableTile(stringResource(R.string.request_problem_other), R.drawable.ic_more, SelectionState.Unselected) },
        )
        TextAreaField(
            stringResource(R.string.request_description_label), stringResource(R.string.request_description_placeholder),
            if (state.phase == CustomerPhase.Empty) "" else stringResource(R.string.request_review_description),
            if (state.descriptionError == null) FieldVisualState.Filled else FieldVisualState.Error,
            state.descriptionError?.let {
                stringResource(
                    when (it) {
                        "request.description.other_min" -> R.string.request_description_other_min
                        "request.description.max" -> R.string.request_description_max
                        else -> R.string.request_problem_required
                    },
                )
            },
        )
        ScreenHeading(stringResource(R.string.request_media_title))
        SecondaryButton(stringResource(R.string.request_media_add), onClick = { onOpen("SCR-C17") })
        if (state.canContinue) {
            MediaThumb(stringResource(R.string.media_photo), stringResource(R.string.media_uploaded), MediaKind.Photo, MediaState.Uploaded)
        }
        PrimaryButton(stringResource(R.string.common_next), if (state.canContinue) ButtonVisualState.Normal else ButtonVisualState.Disabled, onClick = { onOpen("SCR-C04") })
    }
}
