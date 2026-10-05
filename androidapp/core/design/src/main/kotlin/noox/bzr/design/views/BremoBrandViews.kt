package noox.bzr.design.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import noox.bzr.design.R

/** Original artwork is never mirrored with navigation direction. */
class BremoBrandMarkView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : androidx.appcompat.widget.AppCompatImageView(context, attrs) {
    init {
        setImageResource(R.drawable.bremo_brand_icon)
        scaleType = ScaleType.FIT_CENTER
        contentDescription = resources.getString(R.string.app_name)
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, minOf(view.width, view.height) * fraction(R.dimen.bremo_ratio_brand_corner))
            }
        }
    }
}

/** Animation belongs to the visible loading state and stops when hidden or detached. */
class BremoBrandLoaderView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : BremoLinearLayout(context, attrs) {
    private var mark: BremoBrandMarkView? = null
    private var pulse: ValueAnimator? = null

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        gap(R.drawable.bremo_gap_m)
        val padding = px(R.dimen.bremo_space_l)
        setPaddingRelative(0, padding, 0, padding)
        contentDescription = resources.getString(R.string.a11y_loading)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        val size = px(R.dimen.bremo_size_brand_loader_logo)
        mark = BremoBrandMarkView(context).apply { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
        addView(mark, LayoutParams(size, size))
        addView(TextView(context).apply {
            appearance(R.style.TextAppearance_Bremo_Secondary)
            text = resources.getString(R.string.brand_loading)
            gravity = Gravity.CENTER
            importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateMotion()
    }

    override fun onDetachedFromWindow() {
        stopMotion()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateMotion()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        updateMotion()
    }

    private fun updateMotion() {
        val image = mark ?: return
        if (!isAttachedToWindow || !isShown || windowVisibility != VISIBLE || SnapshotMode.enabled || !ValueAnimator.areAnimatorsEnabled()) {
            stopMotion()
            return
        }
        if (pulse != null) return
        val minimumAlpha = fraction(R.dimen.bremo_ratio_loading_dim)
        val minimumScale = fraction(R.dimen.bremo_ratio_brand_loading_scale)
        pulse = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = resources.getInteger(R.integer.bremo_motion_brand_pulse_ms).toLong()
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                image.alpha = minimumAlpha + (1f - minimumAlpha) * progress
                val scale = minimumScale + (1f - minimumScale) * progress
                image.scaleX = scale
                image.scaleY = scale
            }
            start()
        }
    }

    private fun stopMotion() {
        pulse?.cancel()
        pulse = null
        mark?.apply {
            alpha = 1f
            scaleX = 1f
            scaleY = 1f
        }
    }
}
