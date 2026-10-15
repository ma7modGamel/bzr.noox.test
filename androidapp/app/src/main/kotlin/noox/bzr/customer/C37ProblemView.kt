package noox.bzr.customer

import android.content.Context
import android.content.res.ColorStateList
import android.widget.LinearLayout
import androidx.core.widget.ImageViewCompat
import noox.bzr.design.BzrFormat
import noox.bzr.design.FieldVisualState
import noox.bzr.design.R
import noox.bzr.design.SelectionState
import noox.bzr.design.generated.CategoryIcons
import noox.bzr.design.views.SelectableChipView
import noox.bzr.design.views.cardShadow
import noox.bzr.design.views.sizeCompoundIcons
import noox.bzr.gallery.databinding.ItemAttachmentTileBinding
import noox.bzr.gallery.databinding.ScreenC37ProblemBinding

/** SCR-C37, step 2 of 5 (DEC-071): problem type, description, media — the former C03 body. */
class C37ProblemView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC37ProblemBinding.inflate(inflater, content)
    var onProblemSelect: (Int) -> Unit = {}
    var onDescriptionChange: (String) -> Unit = {}
    var onOpen: (String) -> Unit = {}

    init {
        pinAction(binding.next)
        addRequestProgress(2)
        binding.servicePill.cardShadow()
        binding.descriptionHint.sizeCompoundIcons(R.dimen.bremo_size_icon_small)
        binding.privacy.sizeCompoundIcons(R.dimen.bremo_size_icon_small)
        binding.editService.onClick = { onOpen("SCR-C03") }
        binding.description.onValueChange = { onDescriptionChange(it) }
        listOf(binding.addPhoto, binding.addVideo, binding.addAudio).forEach { pill -> pill.onClick = { onOpen("SCR-C17") } }
        binding.next.onClick = { onOpen("SCR-C04") }
    }

    override fun title(state: CustomerUiState) = string(R.string.request_problem_title)

    override fun renderContent(state: CustomerUiState) {
        binding.serviceName.text = state.items.firstOrNull().orEmpty()
        val iconKey = state.optionIcons.firstOrNull().orEmpty()
        binding.serviceIcon.setImageResource(if (iconKey.isEmpty()) R.drawable.ic_wrench else CategoryIcons.drawable(iconKey))
        ImageViewCompat.setImageTintList(binding.serviceIcon, ColorStateList.valueOf(context.getColor(R.color.bremo_primary600)))
        binding.problemList.removeAllViews()
        // Read before the chips while none is chosen: the step needs a problem type (BR-012).
        binding.problemList.contentDescription = if (state.selectedOptionIndex < 0) string(R.string.request_problem_required) else null
        state.itemDetails.forEachIndexed { index, label ->
            binding.problemList.addView(
                SelectableChipView(context).apply {
                    text = label
                    this.state = if (index == state.selectedOptionIndex) SelectionState.Selected else SelectionState.Unselected
                    setOnClickListener { onProblemSelect(index) }
                },
                LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
            )
        }
        val description = state.fieldValues.firstOrNull().orEmpty()
        binding.description.value = description
        binding.description.optionalLabel = BzrFormat.fill(
            string(R.string.request_description_count),
            "count" to BzrFormat.number(description.length), "max" to BzrFormat.number(MAX_DESCRIPTION),
        )
        // The type is chosen above, so its prompt is not repeated as a field error.
        val error = state.descriptionError?.takeIf { state.selectedOptionIndex >= 0 }
        binding.description.state = if (error == null) FieldVisualState.Filled else FieldVisualState.Error
        binding.description.error = error?.let {
            string(if (it == "request.description.other_min") R.string.request_description_other_min else R.string.request_description_max)
        }
        renderAttachments(state.itemStates)
        binding.next.state = continueState(state)
    }

    private fun renderAttachments(kinds: List<String>) {
        binding.attachmentsTitle.text = BzrFormat.fill(string(R.string.request_media_current), "count" to BzrFormat.number(kinds.size))
        binding.attachments.removeAllViews()
        (kinds + ADD).forEach { kind ->
            val tile = ItemAttachmentTileBinding.inflate(inflater, binding.attachments, false)
            val (icon, label) = when (kind) {
                "photo" -> R.drawable.ic_image to R.string.media_photo
                "video" -> R.drawable.ic_video to R.string.media_video
                "audio" -> R.drawable.ic_mic to R.string.media_audio
                else -> R.drawable.ic_plus to R.string.request_media_add
            }
            tile.icon.setImageResource(icon)
            tile.label.setText(label)
            if (kind == ADD) tile.root.setBackgroundResource(R.drawable.bremo_bg_add_slot)
            if (kind != ADD) tile.root.cardShadow()
            tile.root.setOnClickListener { onOpen("SCR-C17") }
            binding.attachments.addView(tile.root)
        }
    }

    private companion object {
        const val MAX_DESCRIPTION = 1000
        const val ADD = "add"
    }
}
