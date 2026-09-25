package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.MediaKind
import noox.bzr.design.MediaState
import noox.bzr.design.R
import noox.bzr.design.views.MediaThumbView
import noox.bzr.gallery.databinding.ScreenC17MediaBinding

/** SCR-C17 (DEC-047): pick / record media, upload states, limits. */
class C17MediaView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC17MediaBinding.inflate(inflater, content)
    var onPickPhoto: () -> Unit = {}
    var onPickVideo: () -> Unit = {}
    var onRecordAudio: () -> Unit = {}
    var onDelete: (Int) -> Unit = {}
    var onDone: () -> Unit = {}

    private data class Row(val index: Int, val kind: String, val state: String)

    private val adapter = RowAdapter<Row, MediaThumbView>(
        create = { parent -> MediaThumbView(parent.context) },
        bind = { thumb, row, _ ->
            val mediaState = when (row.state) {
                "uploading" -> MediaState.Uploading
                "failed" -> MediaState.Failed
                else -> MediaState.Uploaded
            }
            thumb.title = string(
                when (row.kind) {
                    "video" -> R.string.media_video
                    "audio" -> R.string.media_audio
                    else -> R.string.media_photo
                },
            )
            thumb.stateLabel = string(
                when (mediaState) {
                    MediaState.Uploading -> R.string.media_uploading
                    MediaState.Failed -> R.string.media_failed
                    MediaState.Uploaded -> R.string.media_uploaded
                },
            )
            thumb.kind = when (row.kind) {
                "video" -> MediaKind.Video
                "audio" -> MediaKind.Audio
                else -> MediaKind.Photo
            }
            thumb.state = mediaState
            thumb.onDelete = { onDelete(row.index) }
        },
        key = { it.index },
    )

    init {
        binding.photo.onClick = { onPickPhoto() }
        binding.video.onClick = { onPickVideo() }
        binding.record.onClick = { onRecordAudio() }
        binding.items.rows(adapter, resources.getDimensionPixelSize(R.dimen.bremo_space_m))
        binding.done.onClick = { onDone() }
    }

    override fun title(state: CustomerUiState) = string(R.string.media_sheet_title)

    override fun renderContent(state: CustomerUiState) {
        val recording = state.messageKey == "media.recording"
        binding.record.text = string(if (recording) R.string.media_record_stop else R.string.media_record_audio)
        binding.recording.isVisible = recording
        binding.items.isVisible = state.items.isNotEmpty()
        adapter.submitList(state.items.mapIndexed { index, kind -> Row(index, kind, state.itemStates.getOrElse(index) { "uploaded" }) })
        val error = state.fieldErrors.firstOrNull()
        binding.validation.isVisible = error != null
        error?.let {
            binding.validation.text = string(
                when (it) {
                    "media.validation.photo_limit" -> R.string.media_validation_photo_limit
                    "media.validation.video_limit" -> R.string.media_validation_video_limit
                    "media.validation.audio_limit" -> R.string.media_validation_audio_limit
                    else -> R.string.media_validation_rejected
                },
            )
        }
        binding.done.state = continueState(state)
    }
}
