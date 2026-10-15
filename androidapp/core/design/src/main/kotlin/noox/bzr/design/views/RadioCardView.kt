package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.core.content.withStyledAttributes
import com.google.android.material.card.MaterialCardView
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewRadioCardBinding

/**
 * 43 §3 RadioCard on MaterialCardView. DEC-070 (Figma «Primo App»): white card with a soft shadow and no outline,
 * an optional icon well at the start and badge beside the title; the chosen card takes the selection tint and a
 * filled radio with a check (the unselected radio keeps its outline so it stays visible on white).
 */
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
            binding.body.showIf(value.isNotEmpty())
        }

    @get:JvmName("isRadioSelected")
    @set:JvmName("setRadioSelected")
    var selected: Boolean = false
        set(value) {
            field = value
            render()
        }

    @DrawableRes
    var icon: Int = 0
        set(value) {
            field = value
            binding.iconWell.showIf(value != 0)
            if (value != 0) binding.icon.setImageResource(value)
        }

    /** Short highlight beside the title (C04: «أسرع خيار»); hidden when empty. */
    var badge: String = ""
        set(value) {
            field = value
            binding.badge.text = value
            binding.badge.showIf(value.isNotEmpty())
        }

    init {
        val padding = px(R.dimen.bremo_space_l)
        setContentPadding(padding, padding, padding, padding)
        radius = resources.getDimension(R.dimen.bremo_radius_card)
        strokeWidth = 0
        cardElevation = resources.getDimension(R.dimen.bremo_elevation_card)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            outlineSpotShadowColor = color(R.color.bremo_shadow_card)
            outlineAmbientShadowColor = color(R.color.bremo_shadow_card)
        }
        context.withStyledAttributes(attrs, R.styleable.RadioCardView) {
            title = getString(R.styleable.RadioCardView_bremoTitle).orEmpty()
            body = getString(R.styleable.RadioCardView_bremoBody).orEmpty()
            selected = getBoolean(R.styleable.RadioCardView_bremoSelected, false)
            icon = getResourceId(R.styleable.RadioCardView_bremoIcon, 0)
        }
        badge = ""
        render()
    }

    private fun render() {
        setCardBackgroundColor(color(if (selected) R.color.bremo_primary50 else R.color.bremo_surface))
        binding.radio.setBackgroundResource(if (selected) R.drawable.bremo_bg_radio_check else R.drawable.bremo_bg_radio)
        binding.dot.showIf(selected)
        isSelected = selected
    }
}
