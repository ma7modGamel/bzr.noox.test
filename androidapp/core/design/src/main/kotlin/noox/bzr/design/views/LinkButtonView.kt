package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewLinkButtonBinding

/** 43 §3 LinkButton: teal text action. */
class LinkButtonView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val binding = ViewLinkButtonBinding.inflate(inflater, this)

    var text: String = ""
        set(value) {
            field = value
            binding.button.text = value
        }
    var onClick: () -> Unit = {}

    init {
        binding.button.setOnClickListener { onClick() }
        context.withStyledAttributes(attrs, R.styleable.LinkButtonView) {
            text = getString(R.styleable.LinkButtonView_bremoText).orEmpty()
        }
    }
}
