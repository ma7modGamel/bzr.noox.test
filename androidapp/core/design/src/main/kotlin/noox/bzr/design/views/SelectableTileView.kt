package noox.bzr.design.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.content.withStyledAttributes
import androidx.core.widget.ImageViewCompat
import noox.bzr.design.R
import noox.bzr.design.SelectionState
import noox.bzr.design.databinding.ViewSelectableTileBinding
import noox.bzr.design.generated.CategoryIcons

/**
 * 43 §3 SelectableTile: category tile; selected, unselected, disabled; the catalog icon when `categoryIcon` is set
 * (DEC-059). DEC-070 (Figma «Primo App»): white card without an outline, an optional one-line [detail], and the
 * chosen tile takes the selection tint, a deeper icon well and a check badge at its top-start corner.
 */
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

    /** One line under the name (C03: the category's first problem types); hidden when empty. */
    var detail: String = ""
        set(value) {
            field = value
            binding.detail.text = value
            binding.detail.showIf(value.isNotEmpty())
            contentDescription = listOf(text, value).filter(String::isNotEmpty).joinToString(context.getString(R.string.format_list_separator))
        }

    private val checkBadge = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_check)!!.mutate().apply {
        setTint(color(R.color.bremo_on_primary))
    }
    private val checkCircle = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color(R.color.bremo_primary600))
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

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        if (state != SelectionState.Selected) return
        val size = px(R.dimen.bremo_size_check_badge)
        val inset = px(R.dimen.bremo_space_s)
        val left = if (layoutDirection == LAYOUT_DIRECTION_RTL) width - inset - size else inset
        checkCircle.setBounds(left, inset, left + size, inset + size)
        checkCircle.draw(canvas)
        val pad = size / 5
        checkBadge.setBounds(left + pad, inset + pad, left + size - pad, inset + size - pad)
        checkBadge.draw(canvas)
    }

    private fun render() {
        isSelected = state == SelectionState.Selected
        invalidate()
        isEnabled = state != SelectionState.Disabled
        applyEnabledAlpha(state != SelectionState.Disabled)
        val category = categoryIcon
        if (category == null) {
            binding.icon.setImageResource(icon)
            ImageViewCompat.setImageTintList(binding.icon, android.content.res.ColorStateList.valueOf(color(if (isSelected) R.color.bremo_primary600 else R.color.bremo_decorative_icon)))
            binding.iconWell.background = GradientDrawable().apply {
                cornerRadius = resources.getDimension(R.dimen.bremo_radius_menu_icon_box)
                setColor(color(if (isSelected) R.color.bremo_primary_well_selected else R.color.bremo_accent_tint))
            }
        } else {
            binding.icon.setImageResource(CategoryIcons.drawable(category))
            ImageViewCompat.setImageTintList(binding.icon, android.content.res.ColorStateList.valueOf(color(if (isSelected) R.color.bremo_primary600 else R.color.bremo_decorative_icon)))
            // DEC-070: one flat well for every category (Figma); deeper when the tile is chosen.
            val palette = CategoryIcons.palette(category)
            binding.iconWell.background = GradientDrawable().apply {
                cornerRadius = resources.getDimension(R.dimen.bremo_radius_menu_icon_box)
                setColor(if (isSelected) color(R.color.bremo_primary_well_selected) else palette.tint)
            }
        }
    }
}
