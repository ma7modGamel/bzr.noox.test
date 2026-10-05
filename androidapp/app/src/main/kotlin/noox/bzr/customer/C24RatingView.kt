package noox.bzr.customer

import android.content.Context
import androidx.core.view.children
import androidx.core.view.isVisible
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.FieldVisualState
import noox.bzr.design.R
import noox.bzr.design.views.IconSquareButtonView
import noox.bzr.gallery.databinding.ItemRatingRowBinding
import noox.bzr.gallery.databinding.ScreenC24RatingBinding

/** SCR-C24 (DEC-047): three rating dimensions, comment, submit. */
class C24RatingView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC24RatingBinding.inflate(inflater, content)
    var onRate: (Int, Int) -> Unit = { _, _ -> }
    var onCommentChange: (String) -> Unit = {}
    var onSubmit: () -> Unit = {}
    private val rows: List<ItemRatingRowBinding> get() = listOf(binding.quality, binding.punctuality, binding.conduct)

    init {
        val labels = listOf(R.string.rating_quality, R.string.rating_punctuality, R.string.rating_conduct).map(::string)
        rows.forEachIndexed { dimension, row ->
            row.label.text = labels[dimension]
            stars(row).forEachIndexed { index, star ->
                star.label = "${labels[dimension]} ${index + 1}"
                star.onClick = { onRate(dimension, index + 1) }
            }
        }
        binding.comment.onValueChange = { onCommentChange(it) }
        binding.submit.onClick = { onSubmit() }
    }

    override fun title(state: CustomerUiState) = string(R.string.rating_title)

    override fun renderContent(state: CustomerUiState) {
        rows.forEachIndexed { dimension, row ->
            val selected = state.ratings.getOrElse(dimension) { 0 }
            stars(row).forEachIndexed { index, star -> star.icon = if (index + 1 <= selected) R.drawable.ic_star_filled else R.drawable.ic_star }
        }
        val error = state.fieldErrors.firstOrNull()?.let { key ->
            string(if (key == "rating.validation.comment_max") R.string.rating_validation_comment_max else R.string.rating_validation_required)
        }
        binding.comment.value = state.fieldValues.firstOrNull().orEmpty()
        binding.comment.state = if (error == null) FieldVisualState.Filled else FieldVisualState.Error
        binding.comment.error = error
        val key = state.messageKey
        binding.success.isVisible = key == "rating.success"
        binding.warning.isVisible = key != null && key != "rating.success"
        if (key != null && key != "rating.success") {
            binding.warning.text = string(if (key == "rating.already_exists") R.string.rating_already_exists else R.string.rating_window_closed)
        }
        binding.submit.isVisible = "rate" in state.visibleActions
        binding.submit.state = if (state.isBusy) ButtonVisualState.Loading else continueState(state)
    }

    private fun stars(row: ItemRatingRowBinding) = row.stars.children.filterIsInstance<IconSquareButtonView>().toList()
}
