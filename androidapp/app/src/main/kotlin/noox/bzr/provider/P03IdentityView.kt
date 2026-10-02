package noox.bzr.provider

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenP03IdentityBinding

class P03IdentityView(context: Context) : ProviderScreenView(context) {
    private val binding = ScreenP03IdentityBinding.inflate(inflater, content)
    var onPickFront: () -> Unit = {}
    var onPickBack: () -> Unit = {}
    var onNext: () -> Unit = {}

    init {
        binding.front.onClick = { onPickFront() }
        binding.back.onClick = { onPickBack() }
        binding.next.onClick = { onNext() }
    }

    override fun title() = string(R.string.provider_onboarding_identity_title)

    override fun renderContent(state: ProviderUiState) {
        binding.frontStatus.rows = if (state.hasIdFront) listOf(R.drawable.ic_check to string(R.string.provider_onboarding_identity_uploaded)) else emptyList()
        binding.backStatus.rows = if (state.hasIdBack) listOf(R.drawable.ic_check to string(R.string.provider_onboarding_identity_uploaded)) else emptyList()
        binding.validation.isVisible = state.messageKey != null || state.fieldErrors.isNotEmpty()
        binding.validation.text = string(
            if (state.messageKey == "media.error.invalid") R.string.media_validation_rejected
            else R.string.provider_onboarding_identity_required,
        )
        binding.next.state = buttonState(state)
    }
}
