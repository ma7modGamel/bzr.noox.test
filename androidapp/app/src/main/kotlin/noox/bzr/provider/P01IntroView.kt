package noox.bzr.provider

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenP01IntroBinding

class P01IntroView(context: Context) : ProviderScreenView(context) {
    private val binding = ScreenP01IntroBinding.inflate(inflater, content)
    var onAction: (String) -> Unit = {}

    init {
        binding.start.onClick = { onAction("start_provider_application") }
    }

    override fun title() = string(R.string.provider_onboarding_intro_title)

    override fun renderContent(state: ProviderUiState) {
        binding.start.isVisible = "start_provider_application" in state.visibleActions
        binding.start.state = buttonState(state)
    }
}
