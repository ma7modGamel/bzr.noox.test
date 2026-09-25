package noox.bzr.customer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import noox.bzr.design.Badge
import noox.bzr.design.BadgeKind
import noox.bzr.design.EtaCard
import noox.bzr.design.InfoBanner
import noox.bzr.design.MapCard
import noox.bzr.design.OfflineBanner
import noox.bzr.design.R
import noox.bzr.design.StepState
import noox.bzr.design.StatusStepper
import noox.bzr.design.SummaryCard
import noox.bzr.design.WarningBox

@Composable
fun C09TrackingScreen(state: CustomerUiState, onAction: (String) -> Unit = {}) {
    CustomerScreen(stringResource(R.string.tracking_title), state) {
        if (state.phase == CustomerPhase.Offline) OfflineBanner(stringResource(R.string.offline_message))
        Badge(state.displayStatus.ifEmpty { stringResource(R.string.order_status) }, BadgeKind.Status)
        if (state.showStatusCard) InfoBanner(stringResource(R.string.tracking_status_cancelled))
        if (state.showEta) EtaCard("25", stringResource(R.string.unit_minute), stringResource(R.string.eta_title), if (state.etaApproximate) stringResource(R.string.eta_approximate) else stringResource(R.string.eta_subtitle))
        if (state.showMap) MapCard(stringResource(R.string.map_title))
        if (state.showWaiting) InfoBanner(stringResource(R.string.tracking_waiting))
        if (state.showStepper) {
            val labels = listOf(
                R.string.status_confirmed, R.string.status_on_the_way, R.string.status_arrived,
                R.string.status_in_progress, R.string.status_payment, R.string.status_closed,
            ).map { stringResource(it) }
            val fallback = listOf("done", "active", "pending", "pending", "pending", "pending")
            StatusStepper(labels.zip((state.stepStates.ifEmpty { fallback }).map(::stepState)))
        }
        if (state.messageKey == "status.reviewing") WarningBox(stringResource(R.string.status_reviewing))
        SummaryCard(stringResource(R.string.summary_title), listOf(R.drawable.ic_orders to stringResource(R.string.tracking_order_number), R.drawable.ic_location to stringResource(R.string.tracking_area)), stringResource(R.string.common_edit))
        ActionButtons(state.visibleActions, onAction)
    }
}

private fun stepState(value: String): StepState = when (value) {
    "done" -> StepState.Done
    "active" -> StepState.Active
    "on_hold" -> StepState.OnHold
    else -> StepState.Pending
}
