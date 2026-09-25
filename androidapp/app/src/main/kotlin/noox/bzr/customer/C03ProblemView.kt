package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.FieldVisualState
import noox.bzr.design.MediaKind
import noox.bzr.design.MediaState
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenC03ProblemBinding

/** SCR-C03 (DEC-047): problem type, description, media. */
class C03ProblemView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC03ProblemBinding.inflate(inflater, content)
    var onProblemSelect: () -> Unit = {}
    var onOpen: (String) -> Unit = {}

    init {
        binding.leak.setOnClickListener { onProblemSelect() }
        binding.addMedia.onClick = { onOpen("SCR-C17") }
        binding.next.onClick = { onOpen("SCR-C04") }
        binding.thumb.title = string(R.string.media_photo)
        binding.thumb.stateLabel = string(R.string.media_uploaded)
        binding.thumb.kind = MediaKind.Photo
        binding.thumb.state = MediaState.Uploaded
    }

    override fun title(state: CustomerUiState) = string(R.string.request_problem_title)

    override fun renderContent(state: CustomerUiState) {
        binding.description.value = if (state.phase == CustomerPhase.Empty) "" else string(R.string.request_review_description)
        binding.description.state = if (state.descriptionError == null) FieldVisualState.Filled else FieldVisualState.Error
        binding.description.error = state.descriptionError?.let {
            string(
                when (it) {
                    "request.description.other_min" -> R.string.request_description_other_min
                    "request.description.max" -> R.string.request_description_max
                    else -> R.string.request_problem_required
                },
            )
        }
        binding.thumb.isVisible = state.canContinue
        binding.next.state = continueState(state)
    }
}
