package noox.bzr.design.views

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import noox.bzr.design.R
import noox.bzr.design.StepState
import noox.bzr.design.databinding.ViewStatusStepBinding

/** 43 §3 StatusStepper (C09): the line toward the previous step is teal once this step is reached. */
class StatusStepperView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    var steps: List<Pair<String, StepState>> = emptyList()
        set(value) {
            field = value
            render()
        }

    init {
        orientation = HORIZONTAL
    }

    private fun render() {
        removeAllViews()
        steps.forEachIndexed { index, (label, state) ->
            val column = LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                gap(R.drawable.bremo_gap_s)
            }
            val step = ViewStatusStepBinding.inflate(inflater, column)
            val reached = R.color.bremo_primary600
            step.lineBefore.setBackgroundResource(
                when {
                    index == 0 -> android.R.color.transparent
                    state != StepState.Pending -> reached
                    else -> R.color.bremo_border
                },
            )
            step.lineAfter.setBackgroundResource(
                when {
                    index == steps.lastIndex -> android.R.color.transparent
                    steps[index + 1].second != StepState.Pending -> reached
                    else -> R.color.bremo_border
                },
            )
            val active = state == StepState.Active || state == StepState.OnHold
            step.dot.setSquare(if (active) R.dimen.bremo_size_stepper_dot_active else R.dimen.bremo_size_stepper_dot)
            step.dot.setBackgroundResource(
                when (state) {
                    StepState.Done -> R.drawable.bremo_bg_stepper_done
                    StepState.Active -> R.drawable.bremo_bg_stepper_active
                    StepState.OnHold -> R.drawable.bremo_bg_stepper_on_hold
                    StepState.Pending -> R.drawable.bremo_bg_stepper_pending
                },
            )
            step.check.showIf(state == StepState.Done)
            step.inner.showIf(active)
            step.inner.setBackgroundResource(if (state == StepState.OnHold) R.drawable.bremo_dot_star else R.drawable.bremo_dot_primary)
            step.label.text = label
            val bold = ResourcesCompat.getFont(context, R.font.cairo_bold)
            when (state) {
                StepState.Active -> {
                    step.label.typeface = bold ?: Typeface.DEFAULT_BOLD
                    step.label.setTextColor(color(R.color.bremo_navy900))
                }
                StepState.OnHold -> {
                    step.label.typeface = bold ?: Typeface.DEFAULT_BOLD
                    step.label.setTextColor(color(R.color.bremo_star))
                }
                StepState.Done -> step.label.setTextColor(color(R.color.bremo_navy800))
                StepState.Pending -> Unit
            }
            addView(column, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
    }
}
