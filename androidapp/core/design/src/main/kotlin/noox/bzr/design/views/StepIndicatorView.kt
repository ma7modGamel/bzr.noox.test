package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import noox.bzr.design.BzrFormat
import noox.bzr.design.R
import noox.bzr.design.StepState
import noox.bzr.design.databinding.ViewStepCircleBinding
import noox.bzr.design.databinding.ViewStepIndicatorBinding
import androidx.core.content.withStyledAttributes

/** 43 §3 StepIndicator: numbered circles joined by lines, then the step label. */
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
    var label: String = ""
        set(value) {
            field = value
            binding.label.text = value
        }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        gap(R.drawable.bremo_gap_s)
        binding = ViewStepIndicatorBinding.inflate(inflater, this)
        context.withStyledAttributes(attrs, R.styleable.StepIndicatorView) {
            current = getInt(R.styleable.StepIndicatorView_current, 1)
            total = getInt(R.styleable.StepIndicatorView_total, 1)
            label = getString(R.styleable.StepIndicatorView_bremoLabel).orEmpty()
        }
    }

    private fun render() {
        val steps = binding.steps
        steps.removeAllViews()
        for (step in 1..total) {
            val state = when {
                step < current -> StepState.Done
                step == current -> StepState.Active
                else -> StepState.Pending
            }
            steps.addView(stepCircle(step, state), LayoutParams(stepSize(), stepSize()))
            if (step < total) {
                val line = View(context)
                line.setBackgroundResource(if (step < current) R.color.bremo_primary600 else R.color.bremo_border)
                steps.addView(line, LayoutParams(0, px(R.dimen.bremo_size_stepper_line), 1f))
            }
        }
    }

    private fun stepSize() = (px(R.dimen.bremo_size_step_circle) * resources.configuration.fontScale.coerceAtLeast(1f)).toInt()

    private fun stepCircle(step: Int, state: StepState): View {
        val circle = FrameLayout(context)
        val circleBinding = ViewStepCircleBinding.inflate(inflater, circle)
        circle.setBackgroundResource(
            when (state) {
                StepState.Pending -> R.drawable.bremo_bg_step_pending
                StepState.OnHold -> R.drawable.bremo_bg_step_on_hold
                else -> R.drawable.bremo_bg_step_filled
            },
        )
        circleBinding.check.showIf(state == StepState.Done)
        circleBinding.number.showIf(state != StepState.Done)
        circleBinding.number.text = BzrFormat.number(step)
        circleBinding.number.setTextColor(color(if (state == StepState.Pending) R.color.bremo_slate500 else R.color.bremo_on_primary))
        return circle
    }
}
