package noox.bzr.design.views

import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import noox.bzr.design.R

// Small helpers shared by the component views. Every value is a generated resource (43 §13).

internal val View.inflater: LayoutInflater get() = LayoutInflater.from(context)

internal fun View.color(@ColorRes id: Int): Int = ContextCompat.getColor(context, id)

internal fun View.px(@DimenRes id: Int): Int = resources.getDimensionPixelSize(id)

internal fun View.fraction(@DimenRes id: Int): Float = ResourcesCompat.getFloat(resources, id)

internal fun View.drawable(@DrawableRes id: Int): Drawable? = ContextCompat.getDrawable(context, id)

/** 40% opacity for the disabled state (DesignOpacity.disabled). */
internal fun View.applyEnabledAlpha(enabled: Boolean) {
    alpha = if (enabled) 1f else fraction(R.dimen.bremo_opacity_disabled)
}

internal fun TextView.appearance(@StyleRes id: Int, @ColorRes colorOverride: Int? = null) {
    TextViewCompat.setTextAppearance(this, id)
    includeFontPadding = true
    colorOverride?.let { setTextColor(color(it)) }
}

internal fun ImageView.icon(@DrawableRes id: Int, @ColorRes tint: Int) {
    setImageResource(id)
    ImageViewCompat.setImageTintList(this, ContextCompat.getColorStateList(context, tint))
}

internal fun View.showIf(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

internal fun TypedArray.text(index: Int): String? = getString(index)

internal fun TypedArray.resource(index: Int): Int? = getResourceId(index, 0).takeIf { it != 0 }

internal fun <E : Enum<E>> TypedArray.enumValue(index: Int, values: Array<E>, default: E): E =
    getInt(index, -1).let { values.getOrNull(it) ?: default }

internal fun View.setHeight(height: Int) {
    val params = layoutParams ?: ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
    params.height = height
    layoutParams = params
}

internal fun View.setSquare(@DimenRes size: Int) {
    val value = px(size)
    val params = layoutParams ?: ViewGroup.LayoutParams(value, value)
    params.width = value
    params.height = value
    layoutParams = params
}


/** Hint in the placeholder token (Regular), while typed text keeps the body token (Medium). */
internal fun View.placeholder(text: String): CharSequence {
    val attributes = context.obtainStyledAttributes(R.style.TextAppearance_Bremo_Placeholder, intArrayOf(android.R.attr.fontFamily))
    val font = try {
        attributes.getResourceId(0, 0)
    } finally {
        attributes.recycle()
    }
    val typeface = font.takeIf { it != 0 }?.let { ResourcesCompat.getFont(context, it) } ?: return text
    return android.text.SpannableString(text).apply {
        setSpan(PlaceholderTypefaceSpan(typeface), 0, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

private class PlaceholderTypefaceSpan(private val typeface: android.graphics.Typeface) : android.text.style.MetricAffectingSpan() {
    override fun updateDrawState(paint: android.text.TextPaint) {
        paint.typeface = typeface
    }

    override fun updateMeasureState(paint: android.text.TextPaint) {
        paint.typeface = typeface
    }
}

/** DEC-063: the soft brand-coloured shadow under gradient actions (spot/ambient colour from API 28). */
internal fun View.brandShadow(target: View, enabled: Boolean) {
    target.elevation = if (enabled) resources.getDimension(noox.bzr.design.R.dimen.bremo_elevation_brand) else 0f
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        target.outlineSpotShadowColor = color(noox.bzr.design.R.color.bremo_shadow_brand)
        target.outlineAmbientShadowColor = color(noox.bzr.design.R.color.bremo_shadow_brand)
    }
}

/** DEC-063: the quiet card shadow that lifts a white card off the tinted screen. */
internal fun View.cardShadow() {
    elevation = resources.getDimension(noox.bzr.design.R.dimen.bremo_elevation_card)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        outlineSpotShadowColor = color(noox.bzr.design.R.color.bremo_shadow_card)
        outlineAmbientShadowColor = color(noox.bzr.design.R.color.bremo_shadow_card)
    }
}

/** DEC-063: shows an `ic_*` icon filled with the brand gradient (top-start → bottom-end). */
internal fun ImageView.gradientIcon(@DrawableRes id: Int) {
    val source = requireNotNull(ContextCompat.getDrawable(context, id)).mutate()
    ImageViewCompat.setImageTintList(this, null)
    setImageDrawable(
        GradientMaskDrawable(
            source,
            ContextCompat.getColor(context, noox.bzr.design.R.color.bremo_gradient_icon_brand_from),
            ContextCompat.getColor(context, noox.bzr.design.R.color.bremo_gradient_icon_brand_to),
            rtl = layoutDirection == View.LAYOUT_DIRECTION_RTL,
        ),
    )
}

/** Draws [source] as a mask and paints [from] → [to] through it. */
internal class GradientMaskDrawable(
    private val source: android.graphics.drawable.Drawable,
    private val from: Int,
    private val to: Int,
    private val rtl: Boolean,
) : android.graphics.drawable.Drawable() {
    private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
    }

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        source.bounds = bounds
        val startX = if (rtl) bounds.right.toFloat() else bounds.left.toFloat()
        val endX = if (rtl) bounds.left.toFloat() else bounds.right.toFloat()
        paint.shader = android.graphics.LinearGradient(startX, bounds.top.toFloat(), endX, bounds.bottom.toFloat(), from, to, android.graphics.Shader.TileMode.CLAMP)
    }

    override fun draw(canvas: android.graphics.Canvas) {
        val b = bounds
        val layer = canvas.saveLayer(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat(), null)
        source.draw(canvas)
        canvas.drawRect(b, paint)
        canvas.restoreToCount(layer)
    }

    override fun getIntrinsicWidth() = source.intrinsicWidth
    override fun getIntrinsicHeight() = source.intrinsicHeight
    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) = Unit
    @Deprecated("Deprecated in Java")
    override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
}
