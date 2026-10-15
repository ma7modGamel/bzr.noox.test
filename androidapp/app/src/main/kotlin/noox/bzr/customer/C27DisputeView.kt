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
    var onOpenVisit: () -> Unit = {}
    private val visitButton = noox.bzr.design.views.SecondaryButtonView(context).apply { text = string(R.string.warranty_follow_visit); onClick = { onOpenVisit() } }
    private val reasons = RadioOptions(binding.reasons, resources.getDimensionPixelSize(R.dimen.bremo_space_m), string(R.string.payment_option_body)) { onReasonSelect(it) }

    init {
        content.addView(visitButton)
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

    override fun title(state: CustomerUiState) = string(if (state.isWarranty) R.string.warranty_title else R.string.dispute_title)

    override fun renderContent(state: CustomerUiState) {
        visitButton.isVisible = state.isWarranty && state.warrantyVisitId != null
        val success = state.messageKey in listOf("dispute.success", "warranty.success")
        binding.success.text = string(if (state.isWarranty) R.string.warranty_success else R.string.dispute_success)
        binding.submit.text = string(if (state.isWarranty) R.string.warranty_submit else R.string.dispute_submit)
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
        binding.reasonTitle.isVisible = form && !state.isWarranty
        binding.reasonBody.isVisible = form
        binding.reasonBody.text = string(if (state.isWarranty) R.string.warranty_body else R.string.dispute_body)
        binding.addPhoto.isVisible = form && !state.isWarranty
        binding.reasons.isVisible = form && !state.isWarranty && state.options.isNotEmpty()
        reasons.submit(state.options, state.selectedOptionIndex, !state.isBusy)
        binding.description.label = string(if (state.isWarranty) R.string.warranty_description else R.string.dispute_description_label)
        binding.description.value = state.fieldValues.firstOrNull().orEmpty()
        binding.description.state = if (state.isBusy) FieldVisualState.Disabled else FieldVisualState.Empty
        binding.photo.isVisible = form && state.hasPhoto
        binding.descriptionMax.isVisible = form && state.descriptionError != null
        binding.submit.isVisible = form && ("open_dispute" in state.visibleActions || "request_warranty" in state.visibleActions)
        binding.submit.state = when {
            state.isBusy -> ButtonVisualState.Loading
            state.canContinue -> ButtonVisualState.Normal
            else -> ButtonVisualState.Disabled
        }
    }
}
