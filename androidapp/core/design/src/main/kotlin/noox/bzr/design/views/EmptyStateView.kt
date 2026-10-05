package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewEmptyStateBinding

internal fun LinearLayout.feedbackFrame() {
    orientation = LinearLayout.VERTICAL
    gravity = Gravity.CENTER_HORIZONTAL
    gap(R.drawable.bremo_gap_s)
    val padding = px(R.dimen.bremo_space_l)
    setPadding(padding, padding, padding, padding)
}

/** 43 §3 EmptyState (38 §9): icon circle, title, body, optional primary action. */
class EmptyStateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : BremoLinearLayout(context, attrs) {
    private val binding: ViewEmptyStateBinding

    var title: String = ""
        set(value) {
            field = value
            binding.title.text = value
        }
    var body: String = ""
        set(value) {
            field = value
            binding.body.text = value
        }
    var action: String? = null
        set(value) {
            field = value
            binding.action.text = value.orEmpty()
            binding.action.showIf(value != null)
        }
    var onAction: () -> Unit = {}

    init {
        feedbackFrame()
        binding = ViewEmptyStateBinding.inflate(inflater, this)
        binding.emptyIcon.setIcon(R.drawable.ic_empty)
        binding.action.onClick = { onAction() }
        binding.action.showIf(false)
        context.withStyledAttributes(attrs, R.styleable.FeedbackStateView) {
            title = getString(R.styleable.FeedbackStateView_bremoTitle).orEmpty()
            body = getString(R.styleable.FeedbackStateView_bremoBody).orEmpty()
            action = getString(R.styleable.FeedbackStateView_action)
        }
    }
}
