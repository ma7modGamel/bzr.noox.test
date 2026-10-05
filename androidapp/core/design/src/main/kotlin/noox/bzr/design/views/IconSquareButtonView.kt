package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.content.withStyledAttributes
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewIconSquareButtonBinding

/** 43 §3 IconSquareButton: the square chat button next to a primary action. */
class IconSquareButtonView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val binding = ViewIconSquareButtonBinding.inflate(inflater, this)

    var label: String = ""
        set(value) {
            field = value
            binding.button.contentDescription = value
        }

    @DrawableRes
    var icon: Int = R.drawable.ic_chat
        set(value) {
            field = value
            binding.button.icon = drawable(value)
        }
    var onClick: () -> Unit = {}

    init {
        binding.button.icon = drawable(icon)
        binding.button.setOnClickListener { onClick() }
        context.withStyledAttributes(attrs, R.styleable.IconSquareButtonView) {
            label = getString(R.styleable.IconSquareButtonView_bremoLabel).orEmpty()
            resource(R.styleable.IconSquareButtonView_bremoIcon)?.let { icon = it }
        }
    }
}
