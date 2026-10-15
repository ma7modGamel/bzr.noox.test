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

/**
 * 43 §3 StatusStepper (C09): the line toward the previous step is teal once this step is reached.
 * DEC-076 (Figma 3002:1631/2540): [vertical] lists the steps top to bottom with optional [subtitles].
 */
class StatusStepperView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : BremoLinearLayout(context, attrs) {
    var steps: List<Pair<String, StepState>> = emptyList()
        set(value) {
            field = value
            render()
        }

    var subtitles: List<String> = emptyList()
        set(value) {
            field = value
            render()
        }
    var vertical: Boolean = false
        set(value) {
            field = value
            orientation = if (value) VERTICAL else HORIZONTAL
            render()
        }

    init {
        orientation = HORIZONTAL
    }

    private fun render() {
        removeAllViews()
        if (vertical) {
            renderVertical()
            return
        }
        steps.forEachIndexed { index, (label, state) ->
            val column = BremoLinearLayout(context).apply {
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
            val bold = ResourcesCompat.getFont(context, R.font.ibmplexsansarabic_bold)
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

    private fun renderVertical() {
        steps.forEachIndexed { index, (label, state) ->
            val row = BremoLinearLayout(context).apply { orientation = HORIZONTAL }
            val dotSize = px(R.dimen.bremo_size_vertical_step_dot)
            val column = android.widget.FrameLayout(context)
            if (index < steps.lastIndex) {
                column.addView(android.view.View(context).apply {
                    setBackgroundResource(if (steps[index + 1].second != StepState.Pending) R.color.bremo_primary400 else R.color.bremo_surface_container)
                }, android.widget.FrameLayout.LayoutParams(px(R.dimen.bremo_size_stepper_line), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL))
            }
            val dot = android.widget.FrameLayout(context).apply {
                setBackgroundResource(
                    when (state) {
                        StepState.Done -> R.drawable.bremo_bg_vstep_done
                        StepState.Active, StepState.OnHold -> R.drawable.bremo_bg_vstep_active
                        StepState.Pending -> R.drawable.bremo_bg_vstep_pending
                    },
                )
                if (state == StepState.Done) {
                    addView(android.widget.ImageView(context).apply {
                        setImageResource(R.drawable.ic_check)
                        androidx.core.widget.ImageViewCompat.setImageTintList(this, android.content.res.ColorStateList.valueOf(color(R.color.bremo_on_primary)))
                    }, android.widget.FrameLayout.LayoutParams(px(R.dimen.bremo_size_icon_small), px(R.dimen.bremo_size_icon_small), Gravity.CENTER))
                } else {
                    addView(android.view.View(context).apply {
                        setBackgroundResource(if (state == StepState.Pending) R.drawable.bremo_bg_status_dot else R.drawable.bremo_dot_primary)
                        alpha = if (state == StepState.Pending) fraction(R.dimen.bremo_ratio_loading_dim) else 1f
                    }, android.widget.FrameLayout.LayoutParams(px(R.dimen.bremo_size_radio_dot), px(R.dimen.bremo_size_radio_dot), Gravity.CENTER))
                }
            }
            column.addView(dot, android.widget.FrameLayout.LayoutParams(dotSize, dotSize, Gravity.CENTER_HORIZONTAL or Gravity.TOP))
            row.addView(column, LayoutParams(dotSize, ViewGroup.LayoutParams.MATCH_PARENT))
            val texts = BremoLinearLayout(context).apply {
                orientation = VERTICAL
                setPaddingRelative(px(R.dimen.bremo_space_m), 0, 0, px(R.dimen.bremo_space_l))
            }
            val active = state == StepState.Active || state == StepState.OnHold
            texts.addView(android.widget.TextView(context).apply {
                text = label
                androidx.core.widget.TextViewCompat.setTextAppearance(this, if (active) R.style.TextAppearance_Bremo_TileTitle else R.style.TextAppearance_Bremo_DetailStrong)
                setTextColor(color(when { active -> R.color.bremo_primary600; state == StepState.Pending -> R.color.bremo_slate500; else -> R.color.bremo_navy900 }))
            })
            subtitles.getOrNull(index)?.takeIf(String::isNotEmpty)?.let { subtitle ->
                texts.addView(android.widget.TextView(context).apply {
                    text = subtitle
                    androidx.core.widget.TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Bremo_Hint)
                    if (active || index == 0 && state == StepState.Done) setTextColor(color(R.color.bremo_primary600))
                })
            }
            row.addView(texts, LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(row, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }
}
