package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import com.google.android.material.card.MaterialCardView
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewRadioCardBinding

/** 43 §3 RadioCard on MaterialCardView: selected (teal frame + dot) or unselected. */
class RadioCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    MaterialCardView(context, attrs, com.google.android.material.R.attr.materialCardViewStyle) {
    private val binding = ViewRadioCardBinding.inflate(inflater, this)

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

    @get:JvmName("isRadioSelected")
    @set:JvmName("setRadioSelected")
    var selected: Boolean = false
        set(value) {
            field = value
            render()
        }

    init {
        val padding = px(R.dimen.bremo_space_l)
        setContentPadding(padding, padding, padding, padding)
        context.withStyledAttributes(attrs, R.styleable.RadioCardView) {
            title = getString(R.styleable.RadioCardView_bremoTitle).orEmpty()
            body = getString(R.styleable.RadioCardView_bremoBody).orEmpty()
            selected = getBoolean(R.styleable.RadioCardView_bremoSelected, false)
        }
        render()
    }

    private fun render() {
        setCardBackgroundColor(color(if (selected) R.color.bremo_primary50 else R.color.bremo_surface))
        strokeColor = color(if (selected) R.color.bremo_primary600 else R.color.bremo_border)
        strokeWidth = px(if (selected) R.dimen.bremo_border_selected_width else R.dimen.bremo_border_width)
        binding.radio.setBackgroundResource(if (selected) R.drawable.bremo_bg_radio_checked else R.drawable.bremo_bg_radio)
        binding.dot.showIf(selected)
    }
}
