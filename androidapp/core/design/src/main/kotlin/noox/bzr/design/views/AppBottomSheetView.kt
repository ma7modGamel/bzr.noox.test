package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewAppBottomSheetBinding

/**
 * 43 §3 AppBottomSheet content: handle, title, body, primary action, optional secondary action.
 * Screens show it inside a BottomSheetDialogFragment ([AppBottomSheetFragment]).
 */
class AppBottomSheetView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : BremoLinearLayout(context, attrs) {
    private val binding: ViewAppBottomSheetBinding

    var title: String = ""
        set(value) {
            field = value
            binding.title.text = value
        }
    var body: String = ""
        set(value) {
            field = value
            binding.body.text = value
        }
    var action: String = ""
        set(value) {
            field = value
            binding.action.text = value
        }
    var actionState: ButtonVisualState = ButtonVisualState.Normal
        set(value) {
            field = value
            binding.action.state = value
        }
    var secondaryAction: String? = null
        set(value) {
            field = value
            binding.secondary.text = value.orEmpty()
            binding.secondary.showIf(value != null)
        }
    var onAction: () -> Unit = {}
    var onSecondary: () -> Unit = {}

    init {
        orientation = VERTICAL
        gap(R.drawable.bremo_gap_m)
        setBackgroundResource(R.drawable.bremo_bg_sheet)
        val horizontal = px(R.dimen.bremo_space_screen_horizontal)
        val vertical = px(R.dimen.bremo_space_l)
        setPaddingRelative(horizontal, vertical, horizontal, vertical)
        binding = ViewAppBottomSheetBinding.inflate(inflater, this)
        binding.action.onClick = { onAction() }
        binding.secondary.onClick = { onSecondary() }
        binding.secondary.showIf(false)
        context.withStyledAttributes(attrs, R.styleable.AppBottomSheetView) {
            title = getString(R.styleable.AppBottomSheetView_bremoTitle).orEmpty()
            body = getString(R.styleable.AppBottomSheetView_bremoBody).orEmpty()
            action = getString(R.styleable.AppBottomSheetView_action).orEmpty()
            actionState = enumValue(R.styleable.AppBottomSheetView_buttonState, ButtonVisualState.entries.toTypedArray(), ButtonVisualState.Normal)
            secondaryAction = getString(R.styleable.AppBottomSheetView_secondaryAction)
        }
    }
}
