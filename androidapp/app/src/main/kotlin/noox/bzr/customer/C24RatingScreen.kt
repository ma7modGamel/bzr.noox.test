package noox.bzr.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import noox.bzr.design.BzrText
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.IconSquareButton
import noox.bzr.design.InfoBanner
import noox.bzr.design.PrimaryButton
import noox.bzr.design.R
import noox.bzr.design.TextAreaField
import noox.bzr.design.FieldVisualState
import noox.bzr.design.WarningBox
import noox.bzr.design.generated.DesignSpace
import noox.bzr.design.generated.DesignType

@Composable
fun C24RatingScreen(
    state: CustomerUiState,
    onRate: (Int, Int) -> Unit = { _, _ -> },
    onCommentChange: (String) -> Unit = {},
    onSubmit: () -> Unit = {},
) {
    CustomerScreen(stringResource(R.string.rating_title), state) {
        ScreenHeading(stringResource(R.string.rating_heading), stringResource(R.string.rating_help))
        val labels = listOf(R.string.rating_quality, R.string.rating_punctuality, R.string.rating_conduct)
        labels.indices.forEach { dimension ->
            RatingRow(stringResource(labels[dimension]), state.ratings.getOrElse(dimension) { 0 }) { onRate(dimension, it) }
        }
        val error = state.fieldErrors.firstOrNull()?.let { key ->
            stringResource(if (key == "rating.validation.comment_max") R.string.rating_validation_comment_max else R.string.rating_validation_required)
        }
        TextAreaField(
            stringResource(R.string.rating_comment), stringResource(R.string.rating_comment_placeholder),
            state.fieldValues.firstOrNull().orEmpty(), if (error == null) FieldVisualState.Filled else FieldVisualState.Error,
            error = error, onValueChange = onCommentChange,
        )
        state.messageKey?.let { key ->
            if (key == "rating.success") InfoBanner(stringResource(R.string.rating_success)) else WarningBox(ratingMessageText(key))
        }
        if ("rate" in state.visibleActions) {
            PrimaryButton(
                stringResource(R.string.rating_submit),
                if (state.isBusy) ButtonVisualState.Loading else if (state.canContinue) ButtonVisualState.Normal else ButtonVisualState.Disabled,
                onClick = onSubmit,
            )
        }
    }
}

@Composable
private fun RatingRow(label: String, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignSpace.s)) {
        BzrText(label, DesignType.cardTitle)
        Row(horizontalArrangement = Arrangement.spacedBy(DesignSpace.s)) {
            (1..5).forEach { value ->
                IconSquareButton(label = "$label $value", icon = if (value <= selected) R.drawable.ic_star_filled else R.drawable.ic_star) { onSelect(value) }
            }
        }
    }
}

@Composable
private fun ratingMessageText(key: String): String = stringResource(
    if (key == "rating.already_exists") R.string.rating_already_exists else R.string.rating_window_closed,
)
