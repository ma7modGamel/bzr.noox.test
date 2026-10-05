package noox.bzr.customer

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import noox.bzr.design.BadgeKind
import noox.bzr.design.R
import noox.bzr.design.StepState
import noox.bzr.gallery.databinding.ScreenC09TrackingBinding

/** SCR-C09 (DEC-047): order status, ETA, map, stepper, actions. */
class C09TrackingView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC09TrackingBinding.inflate(inflater, content)
    var onAction: (String) -> Unit = {}

    fun attachNativeMap(view: View, onMyLocation: () -> Unit) {
        binding.map.setNativeContent(view)
        binding.map.onMyLocation = onMyLocation
    }

    /** The last ETA from `GET /tracking`; renders keep it instead of a sample value (CFG-081). */
    private var etaMinutes: Int? = null

    fun updateEta(minutes: Int?, approximate: Boolean) {
        if (minutes != null) etaMinutes = minutes
        binding.eta.value = etaMinutes?.toString() ?: "—"
        binding.eta.subtitle = string(if (approximate) R.string.eta_approximate else R.string.eta_subtitle)
    }

    init {
        binding.status.kind = BadgeKind.Status
        binding.actions.onAction = { onAction(it) }
    }

    override fun title(state: CustomerUiState) = string(R.string.tracking_title)

    override fun renderContent(state: CustomerUiState) {
        binding.offline.isVisible = state.phase == CustomerPhase.Offline
        binding.status.isVisible = state.displayStatus.isNotEmpty()
        binding.status.text = displayStatus(state.displayStatus)
        binding.cancelled.isVisible = state.showStatusCard
        binding.cancelled.text = string(
            when (state.messageKey) {
                "tracking.status.open" -> R.string.tracking_status_open
                "tracking.status.expired" -> R.string.tracking_status_expired
                else -> R.string.tracking_status_cancelled
            },
        )
        binding.eta.isVisible = state.showEta
        binding.eta.value = etaMinutes?.toString() ?: "—"
        binding.eta.unit = string(R.string.unit_minute)
        binding.eta.title = string(R.string.eta_title)
        binding.eta.subtitle = string(if (state.etaApproximate) R.string.eta_approximate else R.string.eta_subtitle)
        binding.map.isVisible = state.showMap
        binding.waiting.isVisible = state.showWaiting
        binding.stepper.isVisible = state.showStepper
        if (state.showStepper) {
            val labels = listOf(
                R.string.status_confirmed, R.string.status_on_the_way, R.string.status_arrived,
                R.string.status_in_progress, R.string.status_payment, R.string.status_closed,
            ).map(::string)
            val fallback = listOf("done", "active", "pending", "pending", "pending", "pending")
            binding.stepper.steps = labels.zip(state.stepStates.ifEmpty { fallback }.map(::stepState))
        }
        binding.reviewing.isVisible = state.messageKey == "status.reviewing"
        val summary = state.items.takeIf { it.size >= 2 }
        binding.summary.rows = listOf(
            R.drawable.ic_orders to (summary?.get(0) ?: string(R.string.tracking_order_number)),
            R.drawable.ic_location to (summary?.get(1) ?: string(R.string.tracking_area)),
        )
        binding.actions.actions = state.visibleActions
    }

    private fun stepState(value: String): StepState = when (value) {
        "done" -> StepState.Done
        "active" -> StepState.Active
        "on_hold" -> StepState.OnHold
        else -> StepState.Pending
    }
}
