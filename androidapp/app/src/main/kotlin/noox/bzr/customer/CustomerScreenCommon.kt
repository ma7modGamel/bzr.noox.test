package noox.bzr.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import noox.bzr.design.AppTopBar
import noox.bzr.design.BzrText
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.DangerTextButton
import noox.bzr.design.ErrorState
import noox.bzr.design.LoadingSkeleton
import noox.bzr.design.PrimaryButton
import noox.bzr.design.R
import noox.bzr.design.SecondaryButton
import noox.bzr.design.generated.DesignColors
import noox.bzr.design.generated.DesignSpace
import noox.bzr.design.generated.DesignType

@Composable
internal fun CustomerScreen(
    title: String,
    state: CustomerUiState,
    onBack: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(DesignColors.surface)) {
        AppTopBar(title, onBack = onBack)
        Column(
            verticalArrangement = Arrangement.spacedBy(DesignSpace.m),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = DesignSpace.screenHorizontal, vertical = DesignSpace.m),
        ) {
            when (state.phase) {
                CustomerPhase.Loading -> repeat(3) { LoadingSkeleton() }
                CustomerPhase.Error -> ErrorState(stringResource(R.string.error_title), stringResource(R.string.error_body), stringResource(R.string.common_retry))
                else -> content()
            }
        }
    }
}

@Composable
internal fun ScreenHeading(title: String, body: String? = null) {
    BzrText(title, DesignType.sectionTitle)
    body?.let { BzrText(it, DesignType.secondary) }
}

@Composable
internal fun ActionButtons(actions: List<String>, onAction: (String) -> Unit = {}) {
    actions.forEach { action ->
        when (action) {
            "accept_offer", "republish", "approve_proposal", "pay_electronic", "confirm_completion", "rate" -> PrimaryButton(actionText(action), onClick = { onAction(action) })
            "cancel", "open_dispute", "report_provider" -> DangerTextButton(actionText(action), onClick = { onAction(action) })
            else -> SecondaryButton(actionText(action), onClick = { onAction(action) })
        }
    }
}

@Composable
private fun actionText(action: String): String = stringResource(
    when (action) {
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
    },
)

@Composable
internal fun TwoColumns(first: @Composable () -> Unit, second: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DesignSpace.m)) {
        Column(Modifier.weight(1f)) { first() }
        Column(Modifier.weight(1f)) { second() }
    }
}

internal fun buttonState(state: CustomerUiState): ButtonVisualState = when {
    state.isBusy -> ButtonVisualState.Loading
    state.canContinue -> ButtonVisualState.Normal
    else -> ButtonVisualState.Disabled
}
