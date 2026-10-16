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
    var onPickWorkPhoto: (Boolean) -> Unit = {}
    var onArrivalCodeChange: (String) -> Unit = {}
    var onArrivalExceptionNoteChange: (String) -> Unit = {}
    private val arrivalCodeField = noox.bzr.design.views.AppTextFieldView(context)
    private val arrivalNoteField = noox.bzr.design.views.AppTextFieldView(context)
    private val arrivalReport = SecondaryButtonView(context)
    private val arrivalPending = noox.bzr.design.views.InfoBannerView(context)
    private val arrivalRefresh = SecondaryButtonView(context)
    private val arrivalPanel = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private var requiresArrivalCode = false
    var onCompletionEstimate: (String) -> Unit = {}
    private val estimateField = noox.bzr.design.views.AppTextFieldView(context)
    private val estimateTime = android.widget.TextView(context)
    private val estimateSave = SecondaryButtonView(context)
    private val warrantyInfo = noox.bzr.design.views.InfoBannerView(context)
    private val photoInfo = noox.bzr.design.views.InfoBannerView(context)
    private val photoCount = android.widget.TextView(context)

    init {
        arrivalCodeField.label = string(DesignR.string.arrival_code_input)
        arrivalCodeField.findViewById<android.widget.EditText>(DesignR.id.edit).inputType = android.text.InputType.TYPE_CLASS_NUMBER
        arrivalCodeField.onValueChange = { value -> arrivalCodeField.value = value; arrivalCodeField.error = null; onArrivalCodeChange(value) }
        arrivalNoteField.label = string(DesignR.string.arrival_exception_note)
        arrivalNoteField.onValueChange = { value -> arrivalNoteField.value = value; arrivalNoteField.error = null; onArrivalExceptionNoteChange(value) }
        arrivalReport.text = string(DesignR.string.arrival_exception_report)
        arrivalReport.onClick = {
            if (arrivalNoteField.value.trim().length !in 5..500) { arrivalNoteField.error = string(DesignR.string.arrival_exception_note_invalid) }
            else onAction("report_arrival_exception")
        }
        arrivalPending.text = string(DesignR.string.arrival_exception_pending)
        arrivalRefresh.text = string(DesignR.string.tracking_follow)
        arrivalRefresh.onClick = { onAction("refresh_order") }
        listOf(arrivalCodeField, arrivalNoteField, arrivalReport, arrivalPending, arrivalRefresh).forEach { arrivalPanel.addView(it) }
        content.addView(arrivalPanel, 0)

        estimateField.label = string(DesignR.string.provider_completion_estimate_minutes)
        estimateField.placeholder = string(DesignR.string.provider_completion_estimate_hint)
        estimateField.findViewById<android.widget.EditText>(DesignR.id.edit).inputType = android.text.InputType.TYPE_CLASS_NUMBER
        estimateField.onValueChange = { value -> estimateField.value = value; estimateField.error = null; estimateField.state = noox.bzr.design.FieldVisualState.Filled }
        estimateSave.text = string(DesignR.string.provider_completion_estimate_save)
        estimateSave.onClick = {
            val minutes = estimateField.value.trim().toIntOrNull()
            if (minutes == null || minutes !in 1..1440) { estimateField.error = string(DesignR.string.provider_completion_estimate_invalid); estimateField.state = noox.bzr.design.FieldVisualState.Error }
            else onCompletionEstimate(estimateField.value)
        }
        content.addView(estimateTime)
        content.addView(estimateField)
        content.addView(estimateSave)
        binding.status.kind = BadgeKind.Status
        content.addView(warrantyInfo, 0)
        photoInfo.text = string(DesignR.string.repair_photos_required)
        content.addView(photoInfo, content.indexOfChild(binding.actions))
        content.addView(photoCount, content.indexOfChild(binding.actions))
    }

    override fun title(): String = string(DesignR.string.provider_order_title)

    fun attachNativeMap(view: View, onMyLocation: () -> Unit) {
        binding.map.setNativeContent(view)
        binding.map.onMyLocation = onMyLocation
    }

    override fun renderContent(state: ProviderUiState) {
        requiresArrivalCode = state.arrivalCodeRequired
        arrivalPanel.isVisible = requiresArrivalCode
        arrivalCodeField.isVisible = requiresArrivalCode
        arrivalNoteField.isVisible = requiresArrivalCode && !state.arrivalExceptionPending
        arrivalReport.isVisible = arrivalNoteField.isVisible
        arrivalReport.enabled = !state.isBusy
        arrivalPending.isVisible = requiresArrivalCode && state.arrivalExceptionPending
        arrivalRefresh.isVisible = requiresArrivalCode
        val mayEstimate = "update_completion_estimate" in state.visibleActions
        estimateField.isVisible = mayEstimate
        estimateSave.isVisible = mayEstimate
        estimateSave.enabled = !state.isBusy
        val estimate = state.fieldValues.firstOrNull().orEmpty()
        estimateTime.isVisible = mayEstimate && estimate.isNotEmpty()
        val displayTime = runCatching { java.time.OffsetDateTime.parse(estimate).atZoneSameInstant(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.SHORT)) }.getOrDefault(estimate)
        estimateTime.text = noox.bzr.design.BzrFormat.fill(string(DesignR.string.tracking_finish_estimated_at), "time" to displayTime)
        warrantyInfo.isVisible = state.warrantyCoverage.isNotBlank()
        warrantyInfo.text = state.warrantyCoverage
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
            // The view model sends the "now" key, as for P08; a scheduled slot arrives as text.
            DesignR.drawable.ic_clock to state.items.getOrElse(3) { "" }.let {
                if (it == "provider.home.timing.now") string(DesignR.string.provider_home_timing_now) else it
            },
        )
        binding.priceGuide.isVisible = state.showPriceGuide
        if (state.showPriceGuide) {
            binding.priceGuide.text = String.format(
                string(DesignR.string.provider_price_guide_range),
                state.priceGuideMinimum,
                state.priceGuideMaximum,
            )
        }
        binding.warning.isVisible = state.messageKey != null || state.arrivalError.isNotEmpty()
        binding.warning.isError = state.arrivalError.isNotEmpty() || state.messageKey in setOf("media.upload_error", "arrival.code.failed", "error.body")
        binding.warning.text = if (state.arrivalError.isNotEmpty()) state.arrivalError else if (state.messageKey == "provider.no_show.pending") string(DesignR.string.provider_no_show_pending) else warning(state.messageKey)
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
        photoInfo.isVisible = screen == "SCR-P14" || screen == "SCR-P15"
        photoCount.isVisible = photoInfo.isVisible
        photoCount.text = String.format(string(DesignR.string.repair_photos_count), state.beforePhotoCount.toString(), state.afterPhotoCount.toString())
        renderActions(state)
    }

    private fun renderActions(state: ProviderUiState) {
        binding.actions.removeAllViews()
        state.visibleActions.filter { it !in listOf("update_completion_estimate", "report_arrival_exception") }.forEach { action ->
            binding.actions.addView(button(action, state), LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun button(action: String, state: ProviderUiState): View {
        val label = string(providerActionLabel(action, screen))
        if (action in listOf("upload_repair_before", "upload_repair_after")) return SecondaryButtonView(context).apply {
            text = label; enabled = !state.isBusy; onClick = { onPickWorkPhoto(action == "upload_repair_before") }
        }
        val click = {
            if (!state.isBusy) {
                if (action == "mark_arrived" && requiresArrivalCode && arrivalCodeField.value.mapNotNull { it.digitToIntOrNull() }.size != 6) {
                    arrivalCodeField.error = string(DesignR.string.arrival_code_invalid)
                } else onAction(action)
            }
        }
        return when (action) {
            "start_trip", "mark_arrived", "start_work", "submit_execution_quote", "complete_work", "confirm_cash" ->
                PrimaryButtonView(context).apply {
                    text = label
                    this.state = when {
                        state.isBusy && state.currentAction == action -> ButtonVisualState.Loading
                        state.isBusy || (action == "complete_work" && (state.beforePhotoCount == 0 || state.afterPhotoCount == 0)) -> ButtonVisualState.Disabled
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
            "media.upload_error" -> DesignR.string.media_upload_error
            "provider.proposal.pending" -> DesignR.string.provider_proposal_pending
            "provider.payment.waiting" -> DesignR.string.provider_payment_waiting
            "arrival.code.failed" -> DesignR.string.arrival_verification_failed
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
    "upload_repair_before" -> DesignR.string.repair_photos_add_before
    "upload_repair_after" -> DesignR.string.repair_photos_add_after
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
