package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.content.withStyledAttributes
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewPrimaryButtonBinding

/**
 * 43 §3 PrimaryButton: normal, pressed, disabled (neutral palette), loading (native progress; static arc in snapshots).
 * DEC-070 (Figma «Primo App»): an optional [icon] after the label — the forward arrow on flow buttons, mirrored in
 * RTL — and a [compact] size (smaller label) for buttons inside cards.
 */
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
    @androidx.annotation.DrawableRes
    var icon: Int = 0
        set(value) {
            field = value
            render()
        }

    var compact: Boolean = false
        set(value) {
            field = value
            androidx.core.widget.TextViewCompat.setTextAppearance(
                binding.button, if (value) R.style.TextAppearance_Bremo_ButtonSmall else R.style.TextAppearance_Bremo_Button,
            )
            binding.button.iconSize = px(if (value) R.dimen.bremo_size_icon_small else R.dimen.bremo_size_icon)
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
            icon = getResourceId(R.styleable.PrimaryButtonView_bremoIcon, 0)
            compact = getBoolean(R.styleable.PrimaryButtonView_bremoCompact, false)
        }
        render()
    }

    /** A wrap-content button (hero action) hugs its label instead of stretching the inner MaterialButton. */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val hug = MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY
        val params = binding.button.layoutParams
        val width = if (hug) LayoutParams.WRAP_CONTENT else LayoutParams.MATCH_PARENT
        if (params.width != width) {
            params.width = width
            binding.button.layoutParams = params
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    private fun interactive() = state == ButtonVisualState.Normal || state == ButtonVisualState.Pressed

    private fun render() {
        val button = binding.button
        val loading = state == ButtonVisualState.Loading
        button.text = if (loading) "" else text
        button.icon = when {
            loading && SnapshotMode.enabled -> drawable(R.drawable.bremo_progress_arc)
            !loading && icon != 0 -> drawable(icon)
            else -> null
        }
        button.iconTint = android.content.res.ColorStateList.valueOf(color(if (state == ButtonVisualState.Disabled) R.color.bremo_disabled_text else R.color.bremo_on_primary))
        button.iconGravity = if (!loading && icon != 0) com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_END
        else com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
        binding.progress.isVisible = loading && !SnapshotMode.enabled
        // DEC-063: brand gradient (pressed: deeper gradient) with a soft brand-coloured shadow.
        button.backgroundTintList = null
        button.setBackgroundResource(if (state == ButtonVisualState.Disabled) R.drawable.bremo_bg_button_disabled else R.drawable.bremo_bg_button_primary)
        button.setTextColor(color(if (state == ButtonVisualState.Disabled) R.color.bremo_disabled_text else R.color.bremo_on_primary))
        button.isPressed = state == ButtonVisualState.Pressed
        brandShadow(button, enabled = state == ButtonVisualState.Normal || state == ButtonVisualState.Pressed)
        button.isEnabled = interactive()
        button.contentDescription = if (state == ButtonVisualState.Loading) "$text, ${context.getString(R.string.a11y_loading)}" else text
        alpha = 1f
    }
}
