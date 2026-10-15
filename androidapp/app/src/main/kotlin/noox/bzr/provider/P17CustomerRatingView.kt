package noox.bzr.provider

import android.content.Context
import androidx.core.view.children
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.design.views.IconSquareButtonView
import noox.bzr.gallery.databinding.ScreenP17CustomerRatingBinding

class P17CustomerRatingView(context: Context) : ProviderScreenView(context) {
    private val binding = ScreenP17CustomerRatingBinding.inflate(inflater, content)
    var onRate: (Int) -> Unit = {}
    var onSubmit: () -> Unit = {}
    var onTag: (Int) -> Unit = {}
    var onComment: (String) -> Unit = {}
    private val stars get() = binding.rating.stars.children.filterIsInstance<IconSquareButtonView>().toList()

    init {
        (binding.submit.parent as android.view.ViewGroup).removeView(binding.submit)
        scaffold.bottomBar.addView(binding.submit)
        scaffold.bottomBar.isVisible = true
        binding.tagChoices.maxColumns = 2
        binding.comment.onValueChange = { onComment(it) }
        binding.rating.label.text = string(R.string.provider_rating_label)
        stars.forEachIndexed { index, star ->
            star.label = "${string(R.string.provider_rating_label)} ${index + 1}"
            star.onClick = { onRate(index) }
        }
        binding.submit.onClick = { onSubmit() }
    }

    override fun title() = string(R.string.provider_rating_title)

    override fun renderContent(state: ProviderUiState) {
        binding.tagLabel.isVisible = state.options.isNotEmpty()
        binding.tagChoices.removeAllViews()
        state.options.forEachIndexed { index, label ->
            binding.tagChoices.addView(noox.bzr.design.views.SelectableChipView(context).apply {
                text = label
                this.state = if (state.isBusy || "rate_customer" !in state.visibleActions) noox.bzr.design.SelectionState.Disabled
                    else if (index in state.selectedPrimaryIndices) noox.bzr.design.SelectionState.Selected else noox.bzr.design.SelectionState.Unselected
                setOnClickListener { onTag(index) }
            })
        }
        binding.comment.value = state.fieldValues.firstOrNull().orEmpty()
        binding.comment.isEnabled = !state.isBusy && "rate_customer" in state.visibleActions
        binding.comment.error = string(R.string.provider_rating_comment_limit).takeIf { "provider.rating.comment_limit" in state.fieldErrors }
        binding.order.editText = null
        binding.order.rows = listOf(
            R.drawable.ic_orders to state.items.getOrElse(0) { "" },
            R.drawable.ic_account to state.items.getOrElse(1) { "" },
            R.drawable.ic_info to state.items.getOrElse(2) { "" },
        )
        stars.forEachIndexed { index, star ->
            star.icon = if (index <= state.selectedOptionIndex) R.drawable.ic_star_filled else R.drawable.ic_star
        }
        binding.success.isVisible = state.messageKey == "provider.rating.success"
        binding.warning.isVisible = state.fieldErrors.isNotEmpty() || state.messageKey == "provider.rating.unavailable" || state.messageKey == "error.body"
        binding.warning.text = string(
            if (state.messageKey == "error.body") R.string.error_body
            else if (state.messageKey == "provider.rating.unavailable") R.string.provider_rating_unavailable
            else if ("provider.rating.comment_limit" in state.fieldErrors) R.string.provider_rating_comment_limit else R.string.provider_rating_required,
        )
        binding.submit.isVisible = "rate_customer" in state.visibleActions
        binding.submit.state = buttonState(state)
    }
}
