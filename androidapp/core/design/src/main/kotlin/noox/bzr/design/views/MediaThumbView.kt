package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.card.MaterialCardView
import noox.bzr.design.MediaKind
import noox.bzr.design.MediaState
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewMediaThumbBinding

/** 43 §3 MediaThumb: photo/video/audio with uploading, uploaded, failed, and delete; read-only for media owned by the other party. */
class MediaThumbView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    MaterialCardView(context, attrs, com.google.android.material.R.attr.materialCardViewStyle) {
    private val binding = ViewMediaThumbBinding.inflate(inflater, this)

    var title: String = ""
        set(value) {
            field = value
            binding.title.text = value
        }
    var stateLabel: String = ""
        set(value) {
            field = value
            render()
        }
    var kind: MediaKind = MediaKind.Photo
        set(value) {
            field = value
            render()
        }
    var state: MediaState = MediaState.Uploading
        set(value) {
            field = value
            render()
        }
    var readOnly: Boolean = false
        set(value) {
            field = value
            render()
        }
    var onDelete: () -> Unit = {}

    init {
        val padding = px(R.dimen.bremo_space_card_padding)
        setContentPadding(padding, padding, padding, padding)
        binding.delete.setOnClickListener { onDelete() }
        render()
    }

    private fun render() {
        binding.kind.icon(
            when (kind) {
                MediaKind.Photo -> R.drawable.ic_image
                MediaKind.Video -> R.drawable.ic_video
                MediaKind.Audio -> R.drawable.ic_mic
            },
            R.color.bremo_primary600,
        )
        binding.stateLabel.text = stateLabel
        binding.stateLabel.setTextColor(color(if (state == MediaState.Failed) R.color.bremo_danger else R.color.bremo_slate500))
        when (state) {
            MediaState.Uploading -> binding.status.icon(R.drawable.bremo_progress_arc, R.color.bremo_primary600)
            MediaState.Uploaded -> binding.status.icon(R.drawable.ic_check, R.color.bremo_primary600)
            MediaState.Failed -> binding.status.icon(R.drawable.ic_error, R.color.bremo_danger)
        }
        binding.status.contentDescription = stateLabel
        binding.stateLabel.visibility = if (stateLabel.isBlank()) GONE else VISIBLE
        binding.status.visibility = if (readOnly) GONE else VISIBLE
        binding.delete.visibility = if (readOnly) GONE else VISIBLE
    }
}
