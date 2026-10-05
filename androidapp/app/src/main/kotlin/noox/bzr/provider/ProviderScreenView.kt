package noox.bzr.provider

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isVisible
import noox.bzr.design.ButtonVisualState
import noox.bzr.gallery.databinding.ViewCustomerScreenBinding

abstract class ProviderScreenView(context: Context) : FrameLayout(context) {
    protected val inflater: LayoutInflater = LayoutInflater.from(context)
    protected val scaffold = ViewCustomerScreenBinding.inflate(inflater, this)
    protected val content: LinearLayout get() = scaffold.content
    protected val overlay: FrameLayout get() = scaffold.overlay
    var onBack: () -> Unit = {}

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        layoutDirection = resources.configuration.layoutDirection
        scaffold.topBar.onBack = { onBack() }
        scaffold.error.retry = string(noox.bzr.design.R.string.common_cancel)
        scaffold.error.onRetry = { onBack() }
    }

    fun render(state: ProviderUiState) {
        scaffold.topBar.title = title()
        scaffold.loading.isVisible = state.phase == ProviderPhase.Loading
        scaffold.error.isVisible = state.phase == ProviderPhase.Error
        content.isVisible = state.phase != ProviderPhase.Loading && state.phase != ProviderPhase.Error
        if (content.isVisible) renderContent(state)
    }

    protected abstract fun title(): String
    protected abstract fun renderContent(state: ProviderUiState)
    protected fun string(id: Int): String = context.getString(id)
    protected fun buttonState(state: ProviderUiState): ButtonVisualState = when {
        state.isBusy -> ButtonVisualState.Loading
        state.canContinue -> ButtonVisualState.Normal
        else -> ButtonVisualState.Disabled
    }

    protected fun providerDisplayStatus(key: String): String =
        when (key) {
            "order.status.provider.OPEN" -> string(noox.bzr.design.R.string.order_status_provider_open)
            "order.status.provider.CONFIRMED" -> string(noox.bzr.design.R.string.order_status_provider_confirmed)
            "order.status.provider.ON_THE_WAY" -> string(noox.bzr.design.R.string.order_status_provider_on_the_way)
            "order.status.provider.ARRIVED" -> string(noox.bzr.design.R.string.order_status_provider_arrived)
            "order.status.provider.AWAITING_QUOTE_APPROVAL" -> string(noox.bzr.design.R.string.order_status_provider_awaiting_quote_approval)
            "order.status.provider.IN_PROGRESS" -> string(noox.bzr.design.R.string.order_status_provider_in_progress)
            "order.status.provider.AWAITING_PAYMENT" -> string(noox.bzr.design.R.string.order_status_provider_awaiting_payment)
            "order.status.provider.AWAITING_TRANSFER_VERIFICATION" -> string(noox.bzr.design.R.string.order_status_provider_awaiting_transfer_verification)
            "order.status.provider.AWAITING_CONFIRMATION" -> string(noox.bzr.design.R.string.order_status_provider_awaiting_confirmation)
            "order.status.provider.DISPUTED" -> string(noox.bzr.design.R.string.order_status_provider_disputed)
            "order.status.provider.CLOSED" -> string(noox.bzr.design.R.string.order_status_provider_closed)
            "order.status.provider.CANCELLED" -> string(noox.bzr.design.R.string.order_status_provider_cancelled)
            "order.status.provider.EXPIRED" -> string(noox.bzr.design.R.string.order_status_provider_expired)
            // A status the app has no key for is shown as the server sent it, never as sample text.
            else -> key
        }
}
