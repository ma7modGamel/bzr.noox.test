package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.FieldVisualState
import noox.bzr.design.MediaKind
import noox.bzr.design.MediaState
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenC27DisputeBinding

/** SCR-C27 (DEC-047): open a dispute, or show the existing one. */
class C27DisputeView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC27DisputeBinding.inflate(inflater, content)
    var onReasonSelect: (Int) -> Unit = {}
    var onDescriptionChange: (String) -> Unit = {}
    var onAddPhoto: () -> Unit = {}
    var onDeletePhoto: (Int) -> Unit = {}
    var onSubmit: () -> Unit = {}
    private val reasons = RadioOptions(binding.reasons, resources.getDimensionPixelSize(R.dimen.bremo_space_m), string(R.string.payment_option_body)) { onReasonSelect(it) }

    init {
        binding.existing.editText = null
        binding.description.onValueChange = { onDescriptionChange(it) }
        binding.addPhoto.onClick = { onAddPhoto() }
        binding.photo.title = string(R.string.dispute_photo_title)
        binding.photo.stateLabel = string(R.string.media_uploaded)
        binding.photo.kind = MediaKind.Photo
        binding.photo.state = MediaState.Uploaded
        binding.photo.onDelete = { onDeletePhoto(0) }
        binding.submit.onClick = { onSubmit() }
    }

    override fun title(state: CustomerUiState) = string(R.string.dispute_title)

    override fun renderContent(state: CustomerUiState) {
        val success = state.messageKey == "dispute.success"
        val existing = !success && state.items.isNotEmpty()
        val form = !success && !existing
        binding.success.isVisible = success
        binding.existingStatus.isVisible = existing
        binding.existing.isVisible = existing
        val note = state.itemStates.getOrNull(1)?.takeIf(String::isNotBlank)
        binding.existingNote.isVisible = existing && note != null
        if (existing) {
            binding.existingStatus.text = state.itemStates.firstOrNull().orEmpty()
            binding.existing.rows = state.items.map { R.drawable.ic_info to it }
            binding.existingNote.text = note.orEmpty()
        }
        listOf(binding.reasonTitle, binding.reasonBody, binding.description, binding.addPhoto).forEach { it.isVisible = form }
        binding.reasons.isVisible = form && state.options.isNotEmpty()
        reasons.submit(state.options, state.selectedOptionIndex, !state.isBusy)
        binding.description.value = state.fieldValues.firstOrNull().orEmpty()
        binding.description.state = if (state.isBusy) FieldVisualState.Disabled else FieldVisualState.Empty
        binding.photo.isVisible = form && state.hasPhoto
        binding.descriptionMax.isVisible = form && state.descriptionError != null
        binding.submit.isVisible = form && "open_dispute" in state.visibleActions
        binding.submit.state = when {
            state.isBusy -> ButtonVisualState.Loading
            state.canContinue -> ButtonVisualState.Normal
            else -> ButtonVisualState.Disabled
        }
    }
}
