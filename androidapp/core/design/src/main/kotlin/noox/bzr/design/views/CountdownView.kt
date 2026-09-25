package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import noox.bzr.design.BzrFormat
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewCountdownBinding

/** 43 §3 Countdown: turns danger below the urgent threshold ("أقل من 5 دقائق"). */
class CountdownView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val binding: ViewCountdownBinding

    var seconds: Int = 0
        set(value) {
            field = value
            binding.value.text = BzrFormat.countdown(value)
            val urgent = value < resources.getInteger(R.integer.bremo_threshold_countdown_urgent_seconds)
            binding.value.setTextColor(color(if (urgent) R.color.bremo_danger else R.color.bremo_primary700))
        }
    var label: String = ""
        set(value) {
            field = value
            binding.label.text = value
        }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        binding = ViewCountdownBinding.inflate(inflater, this)
        context.withStyledAttributes(attrs, R.styleable.CountdownView) {
            seconds = getInt(R.styleable.CountdownView_seconds, 0)
            label = getString(R.styleable.CountdownView_bremoLabel).orEmpty()
        }
    }
}
