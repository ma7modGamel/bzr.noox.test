package noox.bzr.provider

import android.content.Context
import noox.bzr.design.FieldImeAction
import noox.bzr.design.FieldVisualState
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenP02ProfileBinding

class P02ProfileView(context: Context) : ProviderScreenView(context) {
    private val binding = ScreenP02ProfileBinding.inflate(inflater, content)
    var onExperienceChange: (String) -> Unit = {}
    var onBioChange: (String) -> Unit = {}
    var onPickPhoto: () -> Unit = {}
    var onNext: () -> Unit = {}

    init {
        binding.experience.imeAction = FieldImeAction.Next
        binding.experience.onValueChange = { onExperienceChange(it) }
        binding.bio.onValueChange = { onBioChange(it) }
        binding.photo.onClick = { onPickPhoto() }
        binding.next.onClick = { onNext() }
    }

    override fun title() = string(R.string.provider_onboarding_data_title)

    override fun renderContent(state: ProviderUiState) {
        binding.photoStatus.text = string(
            if (state.hasProfilePhoto) R.string.provider_onboarding_profile_photo_uploaded
            else R.string.provider_onboarding_profile_photo_required,
        )
        binding.experience.value = state.fieldValues.getOrElse(0) { "" }
        binding.experience.state = fieldState(state, "provider.onboarding.experience.invalid")
        binding.experience.error = errorText(state, "provider.onboarding.experience.invalid", R.string.provider_onboarding_experience_invalid)
        binding.bio.value = state.fieldValues.getOrElse(1) { "" }
        binding.bio.state = fieldState(state, "provider.onboarding.bio.max")
        binding.bio.error = errorText(state, "provider.onboarding.bio.max", R.string.provider_onboarding_bio_max)
        binding.next.state = buttonState(state)
    }

    private fun fieldState(state: ProviderUiState, key: String): FieldVisualState = when {
        state.isBusy -> FieldVisualState.Disabled
        key in state.fieldErrors -> FieldVisualState.Error
        else -> FieldVisualState.Filled
    }

    private fun errorText(state: ProviderUiState, key: String, text: Int): String? =
        string(text).takeIf { key in state.fieldErrors }
}
