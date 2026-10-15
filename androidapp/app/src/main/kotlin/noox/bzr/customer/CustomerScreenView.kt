package noox.bzr.customer

import noox.bzr.design.views.BremoLinearLayout

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.isVisible
import noox.bzr.gallery.databinding.ViewCustomerScreenBinding
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.R
import noox.bzr.design.views.DangerTextButtonView
import noox.bzr.design.views.PrimaryButtonView
import noox.bzr.design.views.SecondaryButtonView

/**
 * XML counterpart of the Compose CustomerScreen scaffold (DEC-047): top bar, scrolling column
 * (spacing m, screen padding), loading skeletons / error state / content by phase, and a bottom
 * overlay for the inline sheets. Screens only render [CustomerUiState]; all decisions stay in
 * [CustomerLogic] and [CustomerViewModel].
 */
abstract class CustomerScreenView(context: Context) : FrameLayout(context) {
    protected val inflater: LayoutInflater = LayoutInflater.from(context)
    protected val scaffold = ViewCustomerScreenBinding.inflate(inflater, this)
    protected val content: LinearLayout get() = scaffold.content
    protected val bottomBar: LinearLayout get() = scaffold.bottomBar
    protected val overlay: FrameLayout get() = scaffold.overlay

    private var requestProgress: noox.bzr.design.views.StepIndicatorView? = null

    var onBack: () -> Unit = {}
    var onReload: (() -> Unit)? = null

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        // Follow the selected resource language, including Arabic RTL and English LTR.
        layoutDirection = resources.configuration.layoutDirection
        scaffold.topBar.onBack = { onBack() }
        scaffold.error.onRetry = { (onReload ?: onBack)() }
    }

    /** C01 draws its own header (location, notifications, greeting) instead of the top bar (DEC-063). */
    protected open val showsTopBar: Boolean = true

    fun render(state: CustomerUiState) {
        requestProgress?.apply {
            total = state.requestSteps
            if (state.screen == "SCR-C05") current = state.requestSteps
            label = noox.bzr.design.BzrFormat.fill(string(R.string.format_step),
                "current" to noox.bzr.design.BzrFormat.number(current), "total" to noox.bzr.design.BzrFormat.number(total))
            contentDescription = listOf(label, hint).filter(String::isNotEmpty).joinToString(string(R.string.format_list_separator))
        }
        scaffold.topBar.isVisible = showsTopBar
        scaffold.topBar.title = title(state)
        scaffold.topBar.showsBack = state.screen !in setOf("SCR-C01", "SCR-C25", "SCR-C18", "SCR-C02")
        scaffold.error.retry = string(if (onReload == null) R.string.common_cancel else R.string.common_retry)
        scaffold.loading.isVisible = state.phase == CustomerPhase.Loading
        scaffold.error.isVisible = state.phase == CustomerPhase.Error
        content.isVisible = state.phase != CustomerPhase.Loading && state.phase != CustomerPhase.Error
        bottomBar.isVisible = content.isVisible && bottomBar.childCount > 0
        if (content.isVisible) renderContent(state)
        renderOverlay(state)
    }

    /** DEC-073: a screen-owned header that stays above the scrolling body (Figma home header). */
    protected fun pinHeader(view: View) {
        (view.parent as ViewGroup).removeView(view)
        val column = scaffold.topBar.parent as ViewGroup
        column.addView(view, column.indexOfChild(scaffold.topBar) + 1, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    protected fun pinAction(view: View) {
        (view.parent as ViewGroup).removeView(view)
        bottomBar.addView(view, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    /** DEC-071: the request flow has five steps; [hint] names the current step (Figma «Primo App»). */
    protected fun addRequestProgress(current: Int, hint: String = "") {
        content.addView(noox.bzr.design.views.StepIndicatorView(context).apply {
            this.current = current
            total = REQUEST_STEPS
            label = noox.bzr.design.BzrFormat.fill(
                string(R.string.format_step),
                "current" to noox.bzr.design.BzrFormat.number(current), "total" to noox.bzr.design.BzrFormat.number(REQUEST_STEPS),
            )
            this.hint = hint
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            contentDescription = listOf(label, hint).filter(String::isNotEmpty).joinToString(string(R.string.format_list_separator))
        }.also { requestProgress = it }, 0)
    }

    private companion object {
        const val REQUEST_STEPS = 5
    }

    protected abstract fun title(state: CustomerUiState): String

    protected abstract fun renderContent(state: CustomerUiState)

    /** Sheets are drawn over the scaffold in every phase, as in Compose. */
    protected open fun renderOverlay(state: CustomerUiState) = Unit

    protected fun string(id: Int): String = context.getString(id)

    /** `display_status` is a shared localization key from the API, not display-ready copy. */
    protected fun displayStatus(key: String): String =
        when (key) {
            "order.status.customer.OPEN" -> string(R.string.order_status_customer_open)
            "order.status.customer.CONFIRMED" -> string(R.string.order_status_customer_confirmed)
            "order.status.customer.ON_THE_WAY" -> string(R.string.order_status_customer_on_the_way)
            "order.status.customer.ARRIVED" -> string(R.string.order_status_customer_arrived)
            "order.status.customer.AWAITING_QUOTE_APPROVAL" -> string(R.string.order_status_customer_awaiting_quote_approval)
            "order.status.customer.IN_PROGRESS" -> string(R.string.order_status_customer_in_progress)
            "order.status.customer.AWAITING_PAYMENT" -> string(R.string.order_status_customer_awaiting_payment)
            "order.status.customer.AWAITING_TRANSFER_VERIFICATION" -> string(R.string.order_status_customer_awaiting_transfer_verification)
            "order.status.customer.AWAITING_CONFIRMATION" -> string(R.string.order_status_customer_awaiting_confirmation)
            "order.status.customer.DISPUTED" -> string(R.string.order_status_customer_disputed)
            "order.status.customer.CLOSED" -> string(R.string.order_status_customer_closed)
            "order.status.customer.CANCELLED" -> string(R.string.order_status_customer_cancelled)
            "order.status.customer.EXPIRED" -> string(R.string.order_status_customer_expired)
            // A status the app has no key for is shown as the server sent it, never as sample text.
            else -> key
        }
}

/** ActionButtons: the visible order actions, primary / danger / secondary by kind. */
class ActionButtonsView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : BremoLinearLayout(context, attrs) {
    var onAction: (String) -> Unit = {}
    private var shown: List<String> = emptyList()

    init {
        orientation = VERTICAL
        dividerDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bremo_gap_m)
        showDividers = SHOW_DIVIDER_MIDDLE
        isVisible = false
    }

    var actions: List<String>
        get() = shown
        set(value) {
            isVisible = value.isNotEmpty()
            if (value == shown) return
            shown = value
            removeAllViews()
            value.forEach { action -> addView(button(action), LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)) }
        }

    private fun button(action: String): View {
        val label = context.getString(actionLabel(action))
        return when (action) {
            "accept_offer", "republish", "approve_proposal", "pay_electronic", "confirm_completion", "rate" ->
                PrimaryButtonView(context).apply { text = label; state = ButtonVisualState.Normal; onClick = { onAction(action) } }
            "cancel", "open_dispute", "report_provider" ->
                DangerTextButtonView(context).apply { text = label; onClick = { onAction(action) } }
            else -> SecondaryButtonView(context).apply { text = label; onClick = { onAction(action) } }
        }
    }
}

internal fun actionLabel(action: String): Int = when (action) {
    "request_warranty" -> R.string.action_request_warranty
    "accept_offer" -> R.string.action_accept_offer
    "edit_request" -> R.string.action_edit_request
    "republish" -> R.string.action_republish
    "cancel" -> R.string.action_cancel
    "call" -> R.string.action_call
    "chat" -> R.string.action_chat
    "share_visit" -> R.string.action_share_visit
    "open_dispute" -> R.string.action_open_dispute
    "report_provider" -> R.string.action_report_provider
    "approve_proposal" -> R.string.action_approve_proposal
    "reject_proposal" -> R.string.action_reject_proposal
    "change_payment_method" -> R.string.action_change_payment_method
    "pay_electronic" -> R.string.action_pay_electronic
    "confirm_completion" -> R.string.action_confirm_completion
    "rate" -> R.string.action_rate
    else -> R.string.common_close
}

internal fun customerButtonState(state: CustomerUiState): ButtonVisualState = when {
    state.isBusy -> ButtonVisualState.Loading
    state.canContinue -> ButtonVisualState.Normal
    else -> ButtonVisualState.Disabled
}

internal fun continueState(state: CustomerUiState): ButtonVisualState =
    if (state.canContinue) ButtonVisualState.Normal else ButtonVisualState.Disabled
