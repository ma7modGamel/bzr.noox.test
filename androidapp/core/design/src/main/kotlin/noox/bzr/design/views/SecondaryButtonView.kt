package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewSecondaryButtonBinding

/** 43 §3 SecondaryButton: teal outline; normal and disabled (40%). */
class SecondaryButtonView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val binding = ViewSecondaryButtonBinding.inflate(inflater, this)

    var text: String = ""
        set(value) {
            field = value
            binding.button.text = value
        }

    @get:JvmName("isButtonEnabled")
    @set:JvmName("setButtonEnabled")
    var enabled: Boolean = true
        set(value) {
            field = value
            binding.button.isEnabled = value
            applyEnabledAlpha(value)
        }
    var onClick: () -> Unit = {}

    init {
        binding.button.setOnClickListener { if (enabled) onClick() }
        context.withStyledAttributes(attrs, R.styleable.SecondaryButtonView) {
            text = getString(R.styleable.SecondaryButtonView_bremoText).orEmpty()
            enabled = getBoolean(R.styleable.SecondaryButtonView_bremoEnabled, true)
        }
    }
}
