package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewErrorStateBinding

/** 43 §3 ErrorState (38 §9): icon circle, title, body, optional retry. */
class ErrorStateView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val binding: ViewErrorStateBinding

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
    var retry: String? = null
        set(value) {
            field = value
            binding.action.text = value.orEmpty()
            binding.action.showIf(value != null)
        }
    var onRetry: () -> Unit = {}

    init {
        feedbackFrame()
        binding = ViewErrorStateBinding.inflate(inflater, this)
        binding.action.onClick = { onRetry() }
        binding.action.showIf(false)
        context.withStyledAttributes(attrs, R.styleable.FeedbackStateView) {
            title = getString(R.styleable.FeedbackStateView_bremoTitle).orEmpty()
            body = getString(R.styleable.FeedbackStateView_bremoBody).orEmpty()
            retry = getString(R.styleable.FeedbackStateView_action)
        }
    }
}
