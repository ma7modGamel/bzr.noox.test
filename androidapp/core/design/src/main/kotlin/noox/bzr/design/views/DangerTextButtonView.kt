package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewDangerTextButtonBinding

/** 43 §3 DangerTextButton: red text action. */
class DangerTextButtonView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val binding = ViewDangerTextButtonBinding.inflate(inflater, this)

    var text: String = ""
        set(value) {
            field = value
            binding.button.text = value
        }
    var onClick: () -> Unit = {}

    init {
        binding.button.setOnClickListener { onClick() }
        context.withStyledAttributes(attrs, R.styleable.DangerTextButtonView) {
            text = getString(R.styleable.DangerTextButtonView_bremoText).orEmpty()
        }
    }
}
