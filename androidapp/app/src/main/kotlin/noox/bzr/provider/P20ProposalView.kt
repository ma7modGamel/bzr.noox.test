package noox.bzr.provider

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.customer.RadioOptions
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.FieldVisualState
import noox.bzr.design.MediaKind
import noox.bzr.design.MediaState
import noox.bzr.design.R as DesignR
import noox.bzr.design.views.AppBottomSheetView
import noox.bzr.gallery.databinding.ScreenP20ProposalBinding

class P20ProposalView(context: Context) : ProviderScreenView(context) {
    private val binding = ScreenP20ProposalBinding.inflate(inflater, content)
    private val sheet = AppBottomSheetView(context)
    private val types = RadioOptions(
        binding.types,
        resources.getDimensionPixelSize(DesignR.dimen.bremo_space_m),
        "",
    ) { onTypeSelect(it) }

    var onTypeSelect: (Int) -> Unit = {}
    var onAmountChange: (String) -> Unit = {}
    var onReasonChange: (String) -> Unit = {}
    var onOutsideReasonChange: (String) -> Unit = {}
    var onPickPhoto: () -> Unit = {}
    var onDeletePhoto: () -> Unit = {}
    var onSubmit: () -> Unit = {}

    init {
        binding.amount.onValueChange = { onAmountChange(it) }
        binding.reason.onValueChange = { onReasonChange(it) }
        binding.outsideReason.onValueChange = { onOutsideReasonChange(it) }
        binding.addPhoto.onClick = { onPickPhoto() }
        binding.photo.onDelete = { onDeletePhoto() }
        binding.photo.kind = MediaKind.Photo
        binding.photo.title = string(DesignR.string.provider_proposal_photo)
        binding.photo.state = MediaState.Uploaded
        binding.photo.stateLabel = string(DesignR.string.media_uploaded)
        sheet.title = string(DesignR.string.provider_proposal_title)
        sheet.body = ""
        sheet.action = string(DesignR.string.provider_unable_confirm)
        sheet.secondaryAction = string(DesignR.string.common_cancel)
        sheet.onAction = { onSubmit() }
        sheet.onSecondary = { onBack() }
        sheet.isVisible = false
        overlay.addView(sheet)
    }

    override fun title(): String = string(DesignR.string.provider_proposal_title)

    override fun renderContent(state: ProviderUiState) {
        types.submit(state.options, state.selectedOptionIndex, !state.isBusy)
        binding.amount.value = state.secondaryOptions.getOrElse(0) { "" }
        binding.reason.value = state.secondaryOptions.getOrElse(1) { "" }
        binding.outsideReason.value = state.secondaryOptions.getOrElse(2) { "" }
        bindError(binding.amount, state, "provider.proposal.amount.invalid", DesignR.string.provider_proposal_amount_invalid)
        bindError(binding.reason, state, "provider.proposal.reason.invalid", DesignR.string.provider_proposal_reason_invalid)
        bindError(
            binding.outsideReason,
            state,
            "provider.proposal.outside_reason.invalid",
            DesignR.string.provider_proposal_outside_reason_invalid,
        )
        binding.outsideReason.isVisible = state.showOutsideReason
        binding.priceGuide.isVisible = state.showPriceGuide
        binding.priceGuide.text = String.format(
            string(DesignR.string.provider_price_guide_range),
            state.priceGuideMinimum,
            state.priceGuideMaximum,
        )
        binding.warning.isVisible = state.messageKey != null
        binding.warning.text = string(
            when (state.messageKey) {
                "provider.price_guide.other_review" -> DesignR.string.provider_price_guide_other_review
                "provider.proposal.success" -> DesignR.string.provider_proposal_success
                else -> DesignR.string.error_body
            },
        )
        binding.photo.isVisible = state.hasPhoto
        binding.addPhoto.isVisible = !state.hasPhoto
        binding.addPhoto.enabled = !state.isBusy
        sheet.isVisible = true
        sheet.action = string(
            if (state.currentAction == "submit_execution_quote") {
                DesignR.string.action_submit_execution_quote
            } else {
                DesignR.string.action_submit_proposal
            },
        )
        sheet.actionState = when {
            state.isBusy -> ButtonVisualState.Loading
            state.canContinue -> ButtonVisualState.Normal
            else -> ButtonVisualState.Disabled
        }
    }

    private fun bindError(
        field: noox.bzr.design.views.AmountFieldView,
        state: ProviderUiState,
        key: String,
        text: Int,
    ) {
        val hasError = key in state.fieldErrors
        field.state = if (hasError) FieldVisualState.Error else FieldVisualState.Filled
        field.error = if (hasError) string(text) else null
    }

    private fun bindError(
        field: noox.bzr.design.views.TextAreaFieldView,
        state: ProviderUiState,
        key: String,
        text: Int,
    ) {
        val hasError = key in state.fieldErrors
        field.state = if (hasError) FieldVisualState.Error else FieldVisualState.Filled
        field.error = if (hasError) string(text) else null
    }
}
