package noox.bzr.provider

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import noox.bzr.design.BadgeKind
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.StepState
import noox.bzr.design.R as DesignR
import noox.bzr.design.views.DangerTextButtonView
import noox.bzr.design.views.PrimaryButtonView
import noox.bzr.design.views.SecondaryButtonView
import noox.bzr.gallery.databinding.ScreenProviderActiveOrderBinding

open class ProviderActiveOrderView(context: Context, private val screen: String) : ProviderScreenView(context) {
    private val binding = ScreenProviderActiveOrderBinding.inflate(inflater, content)
    var onAction: (String) -> Unit = {}

    init {
        binding.status.kind = BadgeKind.Status
    }

    override fun title(): String = string(DesignR.string.provider_order_title)

    fun attachNativeMap(view: View, onMyLocation: () -> Unit) {
        binding.map.setNativeContent(view)
        binding.map.onMyLocation = onMyLocation
    }

    override fun renderContent(state: ProviderUiState) {
        binding.status.text = providerDisplayStatus(state.displayStatus)
        binding.customer.name = state.customerName
        binding.customer.rating = state.customerRating
        binding.customer.verifiedLabel = null
        binding.map.isVisible = screen == "SCR-P13"
        binding.summary.editText = null
        binding.summary.rows = listOf(
            DesignR.drawable.ic_orders to state.items.getOrElse(0) { "" },
            DesignR.drawable.ic_info to state.items.getOrElse(1) { "" },
            DesignR.drawable.ic_location to state.items.getOrElse(2) { "" },
            DesignR.drawable.ic_clock to state.items.getOrElse(3) { "" },
        )
        binding.priceGuide.isVisible = state.showPriceGuide
        if (state.showPriceGuide) {
            binding.priceGuide.text = String.format(
                string(DesignR.string.provider_price_guide_range),
                state.priceGuideMinimum,
                state.priceGuideMaximum,
            )
        }
        binding.warning.isVisible = state.messageKey != null
        binding.warning.text = warning(state.messageKey)
        binding.payment.isVisible = screen == "SCR-P16" && state.secondaryOptions.isNotEmpty()
        binding.payment.editText = null
        binding.payment.rows = listOf(
            DesignR.drawable.ic_orders to state.secondaryOptions.getOrElse(0) { "" },
            DesignR.drawable.ic_info to state.secondaryOptions.getOrElse(1) { "" },
            DesignR.drawable.ic_check to state.secondaryOptions.getOrElse(2) { "" },
            DesignR.drawable.ic_account to state.secondaryOptions.getOrElse(3) { "" },
        )
        val labels = listOf(
            DesignR.string.status_confirmed, DesignR.string.status_on_the_way,
            DesignR.string.status_arrived, DesignR.string.status_in_progress,
            DesignR.string.status_payment, DesignR.string.status_closed,
        ).map(::string)
        binding.stepper.steps = labels.zip(state.stepStates.ifEmpty {
            listOf("done", "active", "pending", "pending", "pending", "pending")
        }.map(::stepState))
        renderActions(state)
    }

    private fun renderActions(state: ProviderUiState) {
        binding.actions.removeAllViews()
        state.visibleActions.forEach { action ->
            binding.actions.addView(button(action, state), LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun button(action: String, state: ProviderUiState): View {
        val label = string(providerActionLabel(action, screen))
        val click = { if (!state.isBusy) onAction(action) }
        return when (action) {
            "start_trip", "mark_arrived", "start_work", "submit_execution_quote", "complete_work", "confirm_cash" ->
                PrimaryButtonView(context).apply {
                    text = label
                    this.state = when {
                        state.isBusy && state.currentAction == action -> ButtonVisualState.Loading
                        state.isBusy -> ButtonVisualState.Disabled
                        else -> ButtonVisualState.Normal
                    }
                    onClick = click
                }
            "back_out", "report_unable", "report_no_show" -> DangerTextButtonView(context).apply {
                text = label
                onClick = click
                isEnabled = !state.isBusy
            }
            else -> SecondaryButtonView(context).apply {
                text = label
                enabled = !state.isBusy
                onClick = click
            }
        }
    }

    private fun warning(key: String?): String = string(
        when (key) {
            "provider.order.trip.not_ready" -> DesignR.string.provider_order_trip_not_ready
            "provider.order.location.required" -> DesignR.string.provider_order_location_required
            "provider.price_guide.other_review" -> DesignR.string.provider_price_guide_other_review
            "provider.proposal.pending" -> DesignR.string.provider_proposal_pending
            "provider.payment.waiting" -> DesignR.string.provider_payment_waiting
            else -> DesignR.string.error_body
        },
    )

    private fun stepState(value: String): StepState = when (value) {
        "done" -> StepState.Done
        "active" -> StepState.Active
        "on_hold" -> StepState.OnHold
        else -> StepState.Pending
    }
}

class P12ConfirmedOrderView(context: Context) : ProviderActiveOrderView(context, "SCR-P12")
class P13OnTheWayView(context: Context) : ProviderActiveOrderView(context, "SCR-P13")
class P14ArrivedView(context: Context) : ProviderActiveOrderView(context, "SCR-P14")
class P15InProgressView(context: Context) : ProviderActiveOrderView(context, "SCR-P15")
class P16AwaitingPaymentView(context: Context) : ProviderActiveOrderView(context, "SCR-P16")

private fun providerActionLabel(action: String, screen: String): Int = when (action) {
    "start_trip" -> DesignR.string.action_start_trip
    "back_out" -> DesignR.string.action_back_out
    "mark_arrived" -> DesignR.string.action_mark_arrived
    "start_work" -> DesignR.string.action_start_work
    "submit_execution_quote" -> DesignR.string.action_submit_execution_quote
    "complete_inspection_only" -> DesignR.string.action_complete_inspection_only_free
    "submit_proposal" -> DesignR.string.action_submit_proposal
    "complete_work" -> DesignR.string.action_complete_work
    "report_unable" -> DesignR.string.action_report_unable
    "report_no_show" -> DesignR.string.action_report_no_show
    "confirm_cash" -> DesignR.string.action_confirm_cash
    "navigate" -> DesignR.string.action_navigate
    "call" -> DesignR.string.action_call
    "chat" -> DesignR.string.action_chat
    else -> DesignR.string.common_close
}
