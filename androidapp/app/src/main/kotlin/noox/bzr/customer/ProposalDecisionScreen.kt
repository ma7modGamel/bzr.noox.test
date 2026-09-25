package noox.bzr.customer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import noox.bzr.design.Countdown
import noox.bzr.design.InfoBanner
import noox.bzr.design.MediaKind
import noox.bzr.design.MediaState
import noox.bzr.design.MediaThumb
import noox.bzr.design.R
import noox.bzr.design.SummaryCard
import noox.bzr.design.WarningBox

@Composable
fun C30ExecutionQuoteScreen(state: CustomerUiState, onAction: (String) -> Unit = {}) =
    ProposalDecisionContent("SCR-C30", state, onAction)

@Composable
fun C31AdditionalCostScreen(state: CustomerUiState, onAction: (String) -> Unit = {}) =
    ProposalDecisionContent("SCR-C31", state, onAction)

@Composable
private fun ProposalDecisionContent(screen: String, state: CustomerUiState, onAction: (String) -> Unit) {
    CustomerScreen(
        stringResource(if (screen == "SCR-C30") R.string.proposal_execution_title else R.string.proposal_additional_title),
        state,
    ) {
        SummaryCard(
            state.items.getOrElse(0) { stringResource(R.string.proposal_amount) },
            listOf(
                R.drawable.ic_orders to state.items.getOrElse(1) { "" },
                R.drawable.ic_info to state.items.getOrElse(2) { "" },
                R.drawable.ic_check to state.items.getOrElse(3) { "" },
            ), null,
        )
        if (state.hasPhoto) MediaThumb(stringResource(R.string.proposal_photo), "", MediaKind.Photo, MediaState.Uploaded)
        Countdown(state.countdownSeconds, stringResource(R.string.countdown_label))
        if (screen == "SCR-C30") InfoBanner(stringResource(R.string.proposal_free_inspection))
        WarningBox(stringResource(state.messageKey?.let { resourceForProposalMessage(it) } ?: R.string.proposal_pending_warning))
        ActionButtons(state.visibleActions, onAction)
    }
}

private fun resourceForProposalMessage(key: String): Int = when (key) {
    "proposal.expired" -> R.string.proposal_expired
    "proposal.deciding" -> R.string.proposal_deciding
    else -> R.string.proposal_pending_warning
}
