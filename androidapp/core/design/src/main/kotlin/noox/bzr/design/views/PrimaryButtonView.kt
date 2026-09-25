package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewPrimaryButtonBinding

/** 43 §3 PrimaryButton: normal, pressed, disabled (40%), loading (static arc, so snapshots are stable). */
class PrimaryButtonView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val binding = ViewPrimaryButtonBinding.inflate(inflater, this)

    var text: String = ""
        set(value) {
            field = value
            render()
        }
    var state: ButtonVisualState = ButtonVisualState.Normal
        set(value) {
            field = value
            render()
        }

    @get:JvmName("buttonHeight")
    @set:JvmName("setButtonHeight")
    var height: Int = px(R.dimen.bremo_size_primary_button_height)
        set(value) {
            field = value
            requestLayout()
        }
    var onClick: () -> Unit = {}

    init {
        binding.button.setOnClickListener { if (interactive()) onClick() }
        context.withStyledAttributes(attrs, R.styleable.PrimaryButtonView) {
            text = getString(R.styleable.PrimaryButtonView_bremoText).orEmpty()
            state = enumValue(R.styleable.PrimaryButtonView_buttonState, ButtonVisualState.entries.toTypedArray(), ButtonVisualState.Normal)
            height = getDimensionPixelSize(R.styleable.PrimaryButtonView_bremoHeight, height)
        }
        render()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) = super.onMeasure(widthMeasureSpec, fixedHeight(height))

    private fun interactive() = state == ButtonVisualState.Normal || state == ButtonVisualState.Pressed

    private fun render() {
        val button = binding.button
        val loading = state == ButtonVisualState.Loading
        button.text = if (loading) "" else text
        button.icon = if (loading) drawable(R.drawable.bremo_progress_arc) else null
        button.contentDescription = if (loading) resources.getString(R.string.a11y_loading) else null
        button.backgroundTintList = ContextCompat.getColorStateList(
            context,
            if (state == ButtonVisualState.Pressed) R.color.bremo_primary700 else R.color.bremo_button_primary_bg,
        )
        button.isEnabled = interactive()
        applyEnabledAlpha(state != ButtonVisualState.Disabled)
    }
}
