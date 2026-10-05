package noox.bzr.design.views

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.ColorUtils
import androidx.core.widget.ImageViewCompat
import noox.bzr.design.R
import noox.bzr.design.SelectionState
import noox.bzr.design.databinding.ViewSelectableTileBinding
import noox.bzr.design.generated.CategoryIcons

/** 43 §3 SelectableTile: category tile; selected, unselected, disabled; a two-tone catalog icon when `categoryIcon` is set (DEC-059). */
class SelectableTileView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : BremoLinearLayout(context, attrs) {
    private val binding: ViewSelectableTileBinding

    var text: String = ""
        set(value) {
            field = value
            binding.text.text = value
            contentDescription = value
        }

    @DrawableRes
    var icon: Int = R.drawable.ic_home
        set(value) {
            field = value
            render()
        }
    var state: SelectionState = SelectionState.Unselected
        set(value) {
            field = value
            render()
        }

    /** The `icon_path` file name from `/catalog`; drawn untinted in its own two tones. */
    var categoryIcon: String? = null
        set(value) {
            field = value
            render()
        }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        minimumHeight = px(R.dimen.bremo_size_category_tile_h)
        isFocusable = true
        foreground = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground)).let { attributes ->
            try { attributes.getDrawable(0) } finally { attributes.recycle() }
        }
        gap(R.drawable.bremo_gap_s)
        setBackgroundResource(R.drawable.bremo_bg_selectable_card)
        cardShadow()
        val padding = px(R.dimen.bremo_space_m)
        setPadding(padding, padding, padding, padding)
        binding = ViewSelectableTileBinding.inflate(inflater, this)
        context.withStyledAttributes(attrs, R.styleable.SelectableTileView) {
            text = getString(R.styleable.SelectableTileView_bremoText).orEmpty()
            resource(R.styleable.SelectableTileView_bremoIcon)?.let { icon = it }
            state = enumValue(R.styleable.SelectableTileView_selectionState, SelectionState.entries.toTypedArray(), SelectionState.Unselected)
        }
        render()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) =
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

    private fun render() {
        isSelected = state == SelectionState.Selected
        isEnabled = state != SelectionState.Disabled
        applyEnabledAlpha(state != SelectionState.Disabled)
        val category = categoryIcon
        if (category == null) {
            binding.icon.gradientIcon(icon)
            binding.iconWell.setBackgroundResource(R.drawable.bremo_bg_icon_well)
        } else {
            binding.icon.setImageResource(CategoryIcons.drawable(category))
            ImageViewCompat.setImageTintList(binding.icon, null)
            // DEC-063: the well takes the category's own tint, fading toward white.
            val palette = CategoryIcons.palette(category)
            binding.iconWell.background = GradientDrawable(
                if (layoutDirection == LAYOUT_DIRECTION_RTL) GradientDrawable.Orientation.TR_BL else GradientDrawable.Orientation.TL_BR,
                intArrayOf(palette.tint, ColorUtils.blendARGB(palette.tint, color(R.color.bremo_surface), 1f - fraction(R.dimen.bremo_ratio_well_fade))),
            ).apply { cornerRadius = resources.getDimension(R.dimen.bremo_radius_menu_icon_box) }
        }
    }
}
