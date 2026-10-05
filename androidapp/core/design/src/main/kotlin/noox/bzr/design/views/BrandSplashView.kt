package noox.bzr.design.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import noox.bzr.design.R

/**
 * DEC-064: the animated launch that follows the system splash. It starts on the system splash colour with the same
 * symbol at the same size, so the hand-off is seamless, then the brand gradient turns slowly, a soft glow breathes
 * behind the symbol, two rings ripple out, the symbol settles with a spring and the name rises in. [dismiss] fades
 * it into the first screen. When the system turns animations off, it is a still gradient and leaves at once.
 */
class BrandSplashView(context: Context) : FrameLayout(context) {
    private val from = ContextCompat.getColor(context, R.color.bremo_gradient_splash_from)
    private val to = ContextCompat.getColor(context, R.color.bremo_gradient_splash_to)
    private val backdrop = ContextCompat.getColor(context, R.color.bremo_brand_backdrop)
    private val onBrand = ContextCompat.getColor(context, R.color.bremo_on_primary)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.getDimension(R.dimen.bremo_size_control_stroke)
    }
    private var iconSize = px(R.dimen.bremo_size_brand_launch_logo)
    private val ringScale = fraction(R.dimen.bremo_ratio_splash_ring_scale)
    private val glowAlpha = fraction(R.dimen.bremo_ratio_splash_glow)
    private val animated = ValueAnimator.areAnimatorsEnabled() && !SnapshotMode.enabled

    /** 0 → 1 while entering (the gradient blends in from the backdrop); then the loop phase drives the motion. */
    private var entrance = if (animated) 0f else 1f
    private var loop = 0f
    private var loopAnimator: ValueAnimator? = null

    private val logo = ImageView(context).apply {
        setImageResource(R.drawable.bremo_launch_symbol)
        scaleType = ImageView.ScaleType.FIT_CENTER
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private val name = TextView(context).apply {
        appearance(R.style.TextAppearance_Bremo_HeroTitle)
        setTextColor(onBrand)
        text = resources.getString(R.string.app_launcher_name)
        gravity = Gravity.CENTER
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    init {
        setWillNotDraw(false)
        isClickable = true
        contentDescription = resources.getString(R.string.app_name)
        addView(logo, LayoutParams(iconSize, iconSize, Gravity.CENTER))
        addView(name, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL or Gravity.TOP))
        if (animated) {
            name.alpha = 0f
            name.translationY = resources.getDimension(R.dimen.bremo_space_l)
        }
    }

    /**
     * Continues from the system splash (Android 12+): [icon] is the system's own icon view, drawn into this splash
     * at its exact size, so the symbol neither jumps nor changes scale on any device.
     */
    fun continueFrom(icon: View?) {
        if (icon == null || icon.width <= 0 || icon.height <= 0) return
        val bitmap = Bitmap.createBitmap(icon.width, icon.height, Bitmap.Config.ARGB_8888)
        icon.draw(Canvas(bitmap))
        logo.setImageBitmap(bitmap)
        logo.layoutParams = (logo.layoutParams as LayoutParams).apply { width = icon.width; height = icon.height }
        iconSize = icon.width
        placeName()
        invalidate()
    }

    /** Runs the entrance and the loop; call once the view is attached over the window. */
    fun play() {
        if (!animated) return
        val enter = resources.getInteger(R.integer.bremo_motion_splash_enter_ms).toLong()
        name.animate().alpha(1f).translationY(0f).setStartDelay(enter / 3).setDuration(enter * 2 / 3).start()
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = enter
            addUpdateListener { entrance = it.animatedValue as Float; invalidate() }
            start()
        }
        loopAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = resources.getInteger(R.integer.bremo_motion_splash_loop_ms).toLong()
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                loop = it.animatedValue as Float
                // The symbol breathes with the glow, from the exact size the system left it at.
                val breath = 1f + BREATH * (1f - cos(loop * 4 * Math.PI).toFloat()) / 2f
                logo.scaleX = breath
                logo.scaleY = breath
                invalidate()
            }
            start()
        }
    }

    /** Fades into the screen underneath, then removes itself. */
    fun dismiss() {
        val parent = parent as? ViewGroup ?: return
        if (!animated) {
            stop()
            parent.removeView(this)
            return
        }
        animate().alpha(0f).scaleX(SPLASH_EXIT_SCALE).scaleY(SPLASH_EXIT_SCALE)
            .setDuration(resources.getInteger(R.integer.bremo_motion_splash_exit_ms).toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    stop()
                    parent.removeView(this@BrandSplashView)
                }
            }).start()
    }

    private fun stop() {
        loopAnimator?.cancel()
        loopAnimator = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        placeName()
    }

    /** The name sits a gap below the centred symbol, whatever the symbol's size. */
    private fun placeName() {
        if (height == 0) return
        val top = height / 2 + iconSize / 2 + px(R.dimen.bremo_space_xl)
        val params = name.layoutParams as LayoutParams
        if (params.topMargin != top) name.layoutParams = params.apply { topMargin = top }
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        // The gradient axis turns a quarter circle per loop and back, so the light moves without a visible seam.
        val angle = Math.toRadians(DIAGONAL_DEGREES + QUARTER_TURN * sin(loop * 2 * Math.PI))
        val reach = hypot(w, h) / 2f
        val dx = (cos(angle) * reach).toFloat()
        val dy = (sin(angle) * reach).toFloat()
        val start = ColorUtils.blendARGB(backdrop, from, entrance)
        backgroundPaint.shader = LinearGradient(cx - dx, cy - dy, cx + dx, cy + dy, start, to, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, backgroundPaint)

        // A breathing glow behind the symbol.
        val breathe = (1f + sin(loop * 4 * Math.PI).toFloat()) / 2f
        val glowRadius = iconSize * ringScale * (0.75f + 0.25f * breathe)
        glowPaint.shader = RadialGradient(
            cx, cy, glowRadius,
            ColorUtils.setAlphaComponent(onBrand, (255 * glowAlpha * entrance).toInt()),
            ColorUtils.setAlphaComponent(onBrand, 0),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, glowRadius, glowPaint)

        // Two rings ripple out from the symbol, half a cycle apart, fading as they grow.
        if (animated) {
            for (offset in floatArrayOf(0f, 0.5f)) {
                val t = (loop * 2 + offset) % 1f
                val radius = iconSize / 2f * (1f + (ringScale - 1f) * t)
                ringPaint.color = ColorUtils.setAlphaComponent(onBrand, (255 * glowAlpha * 2 * (1f - t) * entrance).toInt())
                canvas.drawCircle(cx, cy, radius, ringPaint)
            }
        }
        super.onDraw(canvas)
    }

    private companion object {
        /** Top-start → bottom-end, the direction of every brand gradient (DEC-063). */
        const val DIAGONAL_DEGREES = 45.0

        /** How far the gradient axis swings each way. */
        const val QUARTER_TURN = 45.0

        /** How much the symbol grows at the top of each breath. */
        const val BREATH = 0.04f

        /** The splash grows slightly as it fades, as if the app comes forward through it. */
        const val SPLASH_EXIT_SCALE = 1.06f
    }
}
