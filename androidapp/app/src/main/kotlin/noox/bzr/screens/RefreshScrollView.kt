package noox.bzr.screens

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.core.widget.NestedScrollView
import kotlin.math.abs

/** Pulls starting at the top refresh server data without intercepting ordinary taps or horizontal gestures. */
class RefreshScrollView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : NestedScrollView(context, attrs) {
    var onRefresh: (() -> Unit)? = null
    var refreshEnabled = false
    var refreshing = false
    private var startX = 0f
    private var startY = 0f
    private var startedAtTop = false
    private var cancelled = false
    private val threshold = maxOf(ViewConfiguration.get(context).scaledTouchSlop * 4f, 72f * resources.displayMetrics.density)

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                startedAtTop = !canScrollVertically(-1)
                cancelled = false
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_CANCEL -> cancelled = true
            MotionEvent.ACTION_MOVE -> if (abs(event.x - startX) > threshold) cancelled = true
            MotionEvent.ACTION_UP -> {
                val trigger = abs(event.x - startX) < threshold && !cancelled && startedAtTop && !canScrollVertically(-1) && refreshEnabled && !refreshing && event.y - startY >= threshold
                val consumed = super.dispatchTouchEvent(event)
                if (trigger) onRefresh?.invoke()
                return consumed || trigger
            }
        }
        return super.dispatchTouchEvent(event)
    }
}
