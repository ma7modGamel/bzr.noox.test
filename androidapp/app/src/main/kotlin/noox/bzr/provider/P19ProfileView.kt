package noox.bzr.provider

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.FieldImeAction
import noox.bzr.design.FieldVisualState
import noox.bzr.design.MediaKind
import noox.bzr.design.MediaState
import noox.bzr.design.R
import noox.bzr.design.StatItem
import noox.bzr.design.views.AppBottomSheetView
import noox.bzr.design.views.CheckRowView
import noox.bzr.design.views.MediaThumbView
import noox.bzr.gallery.databinding.ScreenP19ProfileBinding

class P19ProfileView(context: Context) : ProviderScreenView(context) {
    private val binding = ScreenP19ProfileBinding.inflate(inflater, content)
    private val deleteSheet = AppBottomSheetView(context)
    private var pendingDeleteIndex: Int? = null
    var onExperienceChange: (String) -> Unit = {}
    var onBioChange: (String) -> Unit = {}
    var onSpecialty: (Int) -> Unit = {}
    var onArea: (Int) -> Unit = {}
    var onCaptionChange: (String) -> Unit = {}
    var onAvatar: () -> Unit = {}
    var onPortfolio: () -> Unit = {}
    var onDeletePortfolio: (Int) -> Unit = {}
    var onSupport: () -> Unit = {}
    var onSave: () -> Unit = {}

    init {
        binding.experience.imeAction = FieldImeAction.Next
        binding.experience.onValueChange = { onExperienceChange(it) }
        binding.bio.onValueChange = { onBioChange(it) }
        binding.caption.onValueChange = { onCaptionChange(it) }
        binding.changeAvatar.onClick = { onAvatar() }
        binding.addPortfolio.onClick = { onPortfolio() }
        binding.contactSupport.onClick = { onSupport() }
        binding.save.onClick = { onSave() }
        deleteSheet.title = string(R.string.provider_profile_portfolio_delete_title)
        deleteSheet.body = string(R.string.provider_profile_portfolio_delete_body)
        deleteSheet.action = string(R.string.common_delete)
        deleteSheet.secondaryAction = string(R.string.common_cancel)
        deleteSheet.onAction = {
            pendingDeleteIndex?.let(onDeletePortfolio)
            pendingDeleteIndex = null
            deleteSheet.isVisible = false
        }
        deleteSheet.onSecondary = {
            pendingDeleteIndex = null
            deleteSheet.isVisible = false
        }
        deleteSheet.isVisible = false
        overlay.addView(deleteSheet)
    }

    override fun title() = string(R.string.provider_profile_title)

    override fun renderContent(state: ProviderUiState) {
        binding.header.name = state.customerName
        binding.header.rating = state.customerRating
        binding.header.services = null
        binding.header.verifiedLabel = null
        binding.stats.items = listOf(
            StatItem(R.drawable.ic_star, state.itemStates.getOrElse(0) { "" }, string(R.string.stat_rating)),
            StatItem(R.drawable.ic_orders, state.itemStates.getOrElse(1) { "" }, string(R.string.stat_services)),
            StatItem(
                R.drawable.ic_clock,
                string(R.string.format_minutes).replace("{n}", state.itemStates.getOrElse(2) { "" }),
                string(R.string.stat_response_speed),
            ),
        )
        binding.experience.value = state.fieldValues.getOrElse(0) { "" }
        binding.experience.state = fieldState(state, "provider.onboarding.experience.invalid")
        binding.experience.error = error(state, "provider.onboarding.experience.invalid", R.string.provider_onboarding_experience_invalid)
        binding.bio.value = state.fieldValues.getOrElse(1) { "" }
        binding.bio.state = fieldState(state, "provider.onboarding.bio.max")
        binding.bio.error = error(state, "provider.onboarding.bio.max", R.string.provider_onboarding_bio_max)
        binding.categories.editText = null
        binding.categories.rows = state.options.map { R.drawable.ic_info to it }
        binding.specialties.replace(state.secondaryOptions.mapIndexed { index, label ->
            CheckRowView(context).apply {
                text = label
                checked = index in state.selectedPrimaryIndices
                setOnClickListener { onSpecialty(index) }
            }
        })
        binding.areas.replace(state.areaOptions.mapIndexed { index, label ->
            CheckRowView(context).apply {
                text = label
                checked = index in state.selectedSecondaryIndices
                setOnClickListener { onArea(index) }
            }
        })
        binding.validation.isVisible = state.fieldErrors.isNotEmpty()
        binding.validation.text = state.fieldErrors.firstOrNull()?.let(::errorText).orEmpty()
        binding.portfolio.replace(state.items.mapIndexed { index, label ->
            MediaThumbView(context).apply {
                title = label
                stateLabel = string(R.string.media_uploaded)
                kind = MediaKind.Photo
                this.state = MediaState.Uploaded
                onDelete = {
                    pendingDeleteIndex = index
                    deleteSheet.isVisible = true
                }
            }
        })
        binding.caption.value = state.fieldValues.getOrElse(2) { "" }
        binding.caption.state = if (state.isBusy) FieldVisualState.Disabled else FieldVisualState.Filled
        binding.addPortfolio.isVisible = "add_portfolio_item" in state.visibleActions
        binding.contactSupport.isVisible = "contact_support" in state.visibleActions
        binding.saved.isVisible = state.messageKey == "provider.profile.saved"
        binding.full.isVisible = state.messageKey == "provider.profile.portfolio.full"
        binding.save.isVisible = "update_provider_profile" in state.visibleActions
        binding.save.state = buttonState(state)
    }

    private fun fieldState(state: ProviderUiState, key: String): FieldVisualState = when {
        state.isBusy -> FieldVisualState.Disabled
        key in state.fieldErrors -> FieldVisualState.Error
        else -> FieldVisualState.Filled
    }

    private fun error(state: ProviderUiState, key: String, resource: Int): String? =
        string(resource).takeIf { key in state.fieldErrors }

    private fun errorText(key: String): String = string(
        when (key) {
            "provider.onboarding.experience.invalid" -> R.string.provider_onboarding_experience_invalid
            "provider.onboarding.bio.max" -> R.string.provider_onboarding_bio_max
            "provider.onboarding.specialties.required" -> R.string.provider_onboarding_specialties_required
            else -> R.string.provider_onboarding_areas_required
        },
    )
}
