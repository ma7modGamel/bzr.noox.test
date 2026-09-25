package noox.bzr.customer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.InfoBanner
import noox.bzr.design.MediaKind
import noox.bzr.design.MediaState
import noox.bzr.design.MediaThumb
import noox.bzr.design.PrimaryButton
import noox.bzr.design.R
import noox.bzr.design.SecondaryButton

@Composable
fun C17MediaScreen(
    state: CustomerUiState,
    onPickPhoto: () -> Unit = {},
    onPickVideo: () -> Unit = {},
    onRecordAudio: () -> Unit = {},
    onDelete: (Int) -> Unit = {},
    onDone: () -> Unit = {},
) {
    CustomerScreen(stringResource(R.string.media_sheet_title), state) {
        InfoBanner(stringResource(R.string.media_limits))
        TwoColumns(
            { SecondaryButton(stringResource(R.string.media_pick_photo), onClick = onPickPhoto) },
            { SecondaryButton(stringResource(R.string.media_pick_video), onClick = onPickVideo) },
        )
        SecondaryButton(
            stringResource(if (state.messageKey == "media.recording") R.string.media_record_stop else R.string.media_record_audio),
            onClick = onRecordAudio,
        )
        if (state.messageKey == "media.recording") InfoBanner(stringResource(R.string.media_recording))
        state.items.forEachIndexed { index, kind ->
            val mediaState = when (state.itemStates.getOrElse(index) { "uploaded" }) {
                "uploading" -> MediaState.Uploading
                "failed" -> MediaState.Failed
                else -> MediaState.Uploaded
            }
            MediaThumb(
                title = stringResource(
                    when (kind) {
                        "video" -> R.string.media_video
                        "audio" -> R.string.media_audio
                        else -> R.string.media_photo
                    },
                ),
                stateLabel = stringResource(
                    when (mediaState) {
                        MediaState.Uploading -> R.string.media_uploading
                        MediaState.Failed -> R.string.media_failed
                        MediaState.Uploaded -> R.string.media_uploaded
                    },
                ),
                kind = when (kind) {
                    "video" -> MediaKind.Video
                    "audio" -> MediaKind.Audio
                    else -> MediaKind.Photo
                },
                state = mediaState,
                onDelete = { onDelete(index) },
            )
        }
        state.fieldErrors.firstOrNull()?.let {
            InfoBanner(
                stringResource(
                    when (it) {
                        "media.validation.photo_limit" -> R.string.media_validation_photo_limit
                        "media.validation.video_limit" -> R.string.media_validation_video_limit
                        "media.validation.audio_limit" -> R.string.media_validation_audio_limit
                        else -> R.string.media_validation_rejected
                    },
                ),
            )
        }
        PrimaryButton(
            stringResource(R.string.media_done),
            if (state.canContinue) ButtonVisualState.Normal else ButtonVisualState.Disabled,
            onClick = onDone,
        )
    }
}
