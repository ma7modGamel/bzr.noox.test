package noox.bzr.design.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import noox.bzr.design.R

/**
 * DEC-063: a line icon filled with a gradient (brand by default), top-start → bottom-end. The icon is drawn as a
 * mask and the gradient painted through it, so any `ic_*` vector takes the gradient without a new asset.
 */
class GradientIconView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var drawable: Drawable? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN) }

    @ColorInt
    private var from = ContextCompat.getColor(context, R.color.bremo_gradient_icon_brand_from)

    @ColorInt
    private var to = ContextCompat.getColor(context, R.color.bremo_gradient_icon_brand_to)

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun setIcon(@DrawableRes icon: Int) {
        drawable = ContextCompat.getDrawable(context, icon)?.mutate()
        invalidate()
    }

    /** Paints the icon with [from] → [to]; a flat colour is the same value twice. */
    fun setGradient(@ColorInt from: Int, @ColorInt to: Int) {
        if (this.from == from && this.to == to) return
        this.from = from
        this.to = to
        updateShader()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateShader()
    }

    private fun updateShader() {
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val startX = if (rtl) width.toFloat() else 0f
        val endX = if (rtl) 0f else width.toFloat()
        paint.shader = LinearGradient(startX, 0f, endX, height.toFloat(), from, to, Shader.TileMode.CLAMP)
    }

    override fun onDraw(canvas: Canvas) {
        val icon = drawable ?: return
        val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        icon.setBounds(0, 0, width, height)
        icon.draw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        canvas.restoreToCount(layer)
    }
}
