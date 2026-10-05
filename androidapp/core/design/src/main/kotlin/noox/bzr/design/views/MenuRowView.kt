package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.content.withStyledAttributes
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewMenuRowBinding

/** 43 §3 MenuRow: icon box, text, chevron (C02 menu). */
class MenuRowView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : BremoLinearLayout(context, attrs) {
    private val binding: ViewMenuRowBinding

    @DrawableRes
    var icon: Int = R.drawable.ic_home
        set(value) {
            field = value
            binding.icon.setIcon(value)
        }

    var text: String = ""
        set(value) {
            field = value
            binding.text.text = value
        }
    var onClick: () -> Unit = {}

    /** A destructive row (sign out): red icon in a red-tinted well, red label. */
    var danger: Boolean = false
        set(value) {
            field = value
            renderTone()
        }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        gap(R.drawable.bremo_gap_m)
        val vertical = px(R.dimen.bremo_space_s)
        setPadding(0, vertical, 0, vertical)
        binding = ViewMenuRowBinding.inflate(inflater, this)
        setOnClickListener { onClick() }
        context.withStyledAttributes(attrs, R.styleable.MenuRowView) {
            resource(R.styleable.MenuRowView_bremoIcon)?.let { icon = it }
            text = getString(R.styleable.MenuRowView_bremoText).orEmpty()
        }
        renderTone()
    }

    private fun renderTone() {
        if (danger) {
            binding.icon.setGradient(color(R.color.bremo_danger), color(R.color.bremo_danger))
            binding.iconBox.setBackgroundResource(R.drawable.bremo_bg_icon_well_danger)
            binding.text.setTextColor(color(R.color.bremo_danger))
        } else {
            binding.icon.setGradient(color(R.color.bremo_gradient_icon_brand_from), color(R.color.bremo_gradient_icon_brand_to))
            binding.iconBox.setBackgroundResource(R.drawable.bremo_bg_icon_well)
            binding.text.setTextColor(color(R.color.bremo_navy900))
        }
    }
}
