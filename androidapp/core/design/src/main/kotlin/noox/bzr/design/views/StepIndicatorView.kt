package noox.bzr.design.views

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.withStyledAttributes
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewStepIndicatorBinding

/**
 * 43 §3 StepIndicator, restyled after Figma «Primo App» (DEC-070): «الخطوة n من total» and a hint on one line,
 * then one rounded segment per step — reached segments in the primary colour, the rest in the container tint.
 * Segments follow the layout direction, so in RTL the bar fills from the right.
 */
class StepIndicatorView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : BremoLinearLayout(context, attrs) {
    private val binding: ViewStepIndicatorBinding

    var current: Int = 1
        set(value) {
            field = value
            render()
        }
    var total: Int = 1
        set(value) {
            field = value
            render()
        }

    /** «الخطوة n من total», already formatted by the caller. */
    var label: String = ""
        set(value) {
            field = value
            binding.step.text = value
        }

    /** The current step's name or what comes next; hidden when empty. */
    var hint: String = ""
        set(value) {
            field = value
            binding.label.text = value
            binding.label.showIf(value.isNotEmpty())
        }

    init {
        orientation = VERTICAL
        gap(R.drawable.bremo_gap_s)
        binding = ViewStepIndicatorBinding.inflate(inflater, this)
        context.withStyledAttributes(attrs, R.styleable.StepIndicatorView) {
            current = getInt(R.styleable.StepIndicatorView_current, 1)
            total = getInt(R.styleable.StepIndicatorView_total, 1)
            label = getString(R.styleable.StepIndicatorView_bremoLabel).orEmpty()
        }
        hint = ""
    }

    private fun render() {
        val steps = binding.steps
        steps.removeAllViews()
        val gapPx = px(R.dimen.bremo_size_step_segment_gap)
        for (step in 1..total) {
            val segment = View(context).apply {
                background = GradientDrawable().apply {
                    cornerRadius = resources.getDimension(R.dimen.bremo_radius_full)
                    setColor(color(if (step <= this@StepIndicatorView.current) R.color.bremo_primary600 else R.color.bremo_surface_container_highest))
                }
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            steps.addView(segment, android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                if (step > 1) marginStart = gapPx
            })
        }
    }
}
