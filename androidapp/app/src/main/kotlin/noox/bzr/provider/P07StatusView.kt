package noox.bzr.provider

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.design.views.PrimaryButtonView
import noox.bzr.gallery.databinding.ScreenP07StatusBinding

class P07StatusView(context: Context) : ProviderScreenView(context) {
    private val binding = ScreenP07StatusBinding.inflate(inflater, content)
    var onAction: (String) -> Unit = {}

    override fun title() = string(R.string.provider_application_status_title)

    override fun renderContent(state: ProviderUiState) {
        val pair = statusCopy(state.messageKey)
        binding.status.title = string(pair.first)
        binding.status.body = string(pair.second)
        binding.reason.isVisible = state.items.isNotEmpty()
        binding.reason.text = state.items.firstOrNull().orEmpty()
        binding.actions.replace(state.visibleActions.filter { it != "submit_provider_application" }.map { action ->
            PrimaryButtonView(context).apply {
                text = string(
                    if (action == "open_provider_home") R.string.action_open_provider_home
                    else R.string.action_resubmit_provider_application,
                )
                this.state = buttonState(state)
                onClick = { onAction(action) }
            }
        })
    }

    private fun statusCopy(key: String?): Pair<Int, Int> = when (key) {
        "provider.application.rejected" -> R.string.provider_application_rejected to R.string.provider_application_rejected_body
        "provider.application.active" -> R.string.provider_application_active to R.string.provider_application_active_body
        "provider.application.suspended" -> R.string.provider_application_suspended to R.string.provider_application_suspended_body
        else -> R.string.provider_application_pending to R.string.provider_application_pending_body
    }
}
