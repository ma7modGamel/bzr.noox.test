package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.MediaKind
import noox.bzr.design.MediaState
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenC30ProposalBinding

/** SCR-C30 execution quote and SCR-C31 additional cost (DEC-047): one content, two titles. */
open class ProposalDecisionView(context: Context, private val screen: String) : CustomerScreenView(context) {
    private val binding = ScreenC30ProposalBinding.inflate(inflater, content)
    var onAction: (String) -> Unit = {}

    init {
        binding.photo.title = string(R.string.proposal_photo)
        binding.photo.stateLabel = ""
        binding.photo.kind = MediaKind.Photo
        binding.photo.state = MediaState.Uploaded
        binding.freeInspection.isVisible = screen == "SCR-C30"
        binding.actions.onAction = { onAction(it) }
    }

    override fun title(state: CustomerUiState) =
        string(if (screen == "SCR-C30") R.string.proposal_execution_title else R.string.proposal_additional_title)

    override fun renderContent(state: CustomerUiState) {
        binding.summary.title = state.items.getOrElse(0) { string(R.string.proposal_amount) }
        binding.summary.rows = listOf(
            R.drawable.ic_orders to state.items.getOrElse(1) { "" },
            R.drawable.ic_info to state.items.getOrElse(2) { "" },
            R.drawable.ic_check to state.items.getOrElse(3) { "" },
        )
        binding.summary.editText = null
        binding.photo.isVisible = state.hasPhoto
        binding.countdown.seconds = state.countdownSeconds
        binding.warning.text = string(
            when (state.messageKey) {
                "proposal.expired" -> R.string.proposal_expired
                "proposal.deciding" -> R.string.proposal_deciding
                else -> R.string.proposal_pending_warning
            },
        )
        binding.actions.actions = state.visibleActions
    }
}

/** SCR-C30 (C30ExecutionQuoteView in SwiftUI). */
class C30ExecutionQuoteView(context: Context) : ProposalDecisionView(context, "SCR-C30")

/** SCR-C31 (C31AdditionalCostView in SwiftUI). */
class C31AdditionalCostView(context: Context) : ProposalDecisionView(context, "SCR-C31")
