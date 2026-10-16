package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewSecondaryButtonBinding

/** Secondary actions use a compact teal surface inside the full touch target. */
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
            alpha = 1f
        }
    /** DEC-076: an optional leading icon (Figma «رسالة» / «اتصال»). */
    @androidx.annotation.DrawableRes
    var icon: Int = 0
        set(value) {
            field = value
            binding.button.icon = if (value == 0) null else androidx.core.content.ContextCompat.getDrawable(context, value)
            binding.button.iconTint = androidx.core.content.ContextCompat.getColorStateList(context, R.color.bremo_button_secondary_fg)
            binding.button.iconSize = resources.getDimensionPixelSize(R.dimen.bremo_size_icon_small)
            binding.button.iconPadding = resources.getDimensionPixelSize(R.dimen.bremo_space_s)
            binding.button.iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_START
        }
    var onClick: () -> Unit = {}

    init {
        val inset = (px(R.dimen.bremo_size_touch_target_min) - px(R.dimen.bremo_size_secondary_button_visual_height)) / 2
        binding.button.insetTop = inset
        binding.button.insetBottom = inset
        binding.button.setOnClickListener { if (enabled) onClick() }
        context.withStyledAttributes(attrs, R.styleable.SecondaryButtonView) {
            text = getString(R.styleable.SecondaryButtonView_bremoText).orEmpty()
            enabled = getBoolean(R.styleable.SecondaryButtonView_bremoEnabled, true)
        }
    }
}
