package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.content.withStyledAttributes
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewPrimaryButtonBinding

/** 43 §3 PrimaryButton: normal, pressed, disabled (40%), loading (native progress; static arc in snapshots). */
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
            binding.button.minimumHeight = value
            minimumHeight = value
            requestLayout()
        }
    var onClick: () -> Unit = {}

    init {
        minimumHeight = height
        binding.button.minimumHeight = height
        binding.button.setOnClickListener { if (interactive()) onClick() }
        context.withStyledAttributes(attrs, R.styleable.PrimaryButtonView) {
            text = getString(R.styleable.PrimaryButtonView_bremoText).orEmpty()
            state = enumValue(R.styleable.PrimaryButtonView_buttonState, ButtonVisualState.entries.toTypedArray(), ButtonVisualState.Normal)
            height = getDimensionPixelSize(R.styleable.PrimaryButtonView_bremoHeight, height)
        }
        render()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) = super.onMeasure(widthMeasureSpec, heightMeasureSpec)

    private fun interactive() = state == ButtonVisualState.Normal || state == ButtonVisualState.Pressed

    private fun render() {
        val button = binding.button
        val loading = state == ButtonVisualState.Loading
        button.text = if (loading) "" else text
        button.icon = if (loading && SnapshotMode.enabled) drawable(R.drawable.bremo_progress_arc) else null
        binding.progress.isVisible = loading && !SnapshotMode.enabled
        // DEC-063: brand gradient (pressed: deeper gradient) with a soft brand-coloured shadow.
        button.backgroundTintList = null
        button.setBackgroundResource(R.drawable.bremo_bg_button_primary)
        button.isPressed = state == ButtonVisualState.Pressed
        brandShadow(button, enabled = state == ButtonVisualState.Normal || state == ButtonVisualState.Pressed)
        button.isEnabled = interactive()
        button.contentDescription = if (state == ButtonVisualState.Loading) "$text, ${context.getString(R.string.a11y_loading)}" else text
        applyEnabledAlpha(state != ButtonVisualState.Disabled)
    }
}
