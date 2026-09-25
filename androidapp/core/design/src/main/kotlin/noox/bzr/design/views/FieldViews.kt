package noox.bzr.design.views

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.withStyledAttributes
import android.view.View
import noox.bzr.design.FieldVisualState
import noox.bzr.design.R

/**
 * Shared shell of the four text fields (43 §3): label row, framed EditText, error line.
 * The label sits above the box (38), so the frame is a generated selector drawable rather than
 * TextInputLayout's outline, whose label lives inside the stroke.
 * States: empty, filled, focused (teal 1.5), error (red 1.5 + text), disabled (40%).
 * Without [onValueChange] the field is static (gallery and snapshots), as in Compose and SwiftUI.
 */
abstract class FieldShellView internal constructor(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    internal abstract val labelView: TextView
    internal abstract val optionalView: TextView
    internal abstract val frameView: View
    internal abstract val editText: EditText
    internal abstract val errorView: TextView
    private var syncing = false

    init {
        orientation = VERTICAL
        gap(R.drawable.bremo_gap_xs)
    }

    internal fun bindShell() {
        // See SnapshotMode: layoutlib only.
        if (SnapshotMode.enabled) editText.setHorizontallyScrolling(false)
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (!syncing) changed(s?.toString().orEmpty())
            }
        })
    }

    internal abstract fun changed(value: String)

    internal fun readShell(attrs: AttributeSet?, apply: (label: String, placeholder: String, value: String, state: FieldVisualState, error: String?, optional: String?, currency: String?) -> Unit) {
        context.withStyledAttributes(attrs, R.styleable.FieldView) {
            apply(
                getString(R.styleable.FieldView_bremoLabel).orEmpty(),
                getString(R.styleable.FieldView_bremoPlaceholder).orEmpty(),
                getString(R.styleable.FieldView_bremoValue).orEmpty(),
                enumValue(R.styleable.FieldView_fieldState, FieldVisualState.entries.toTypedArray(), FieldVisualState.Empty),
                getString(R.styleable.FieldView_bremoError),
                getString(R.styleable.FieldView_bremoOptionalLabel),
                getString(R.styleable.FieldView_bremoCurrency),
            )
        }
    }

    internal fun renderShell(label: String, placeholder: String, value: String, state: FieldVisualState, error: String?, optionalLabel: String?, interactive: Boolean) {
        labelView.text = label
        optionalView.text = optionalLabel.orEmpty()
        optionalView.showIf(optionalLabel != null)
        editText.hint = placeholder(placeholder)
        if (editText.text.toString() != value) {
            syncing = true
            editText.setText(value)
            syncing = false
        }
        errorView.text = error.orEmpty()
        errorView.showIf(error != null)
        val enabled = state != FieldVisualState.Disabled
        editText.isEnabled = enabled
        editText.isFocusable = interactive && enabled
        editText.isFocusableInTouchMode = interactive && enabled
        editText.isCursorVisible = interactive
        applyEnabledAlpha(enabled)
        frameView.setBackgroundResource(
            when (state) {
                FieldVisualState.Error -> R.drawable.bremo_bg_field_error
                FieldVisualState.Focused -> R.drawable.bremo_bg_field_focused
                else -> R.drawable.bremo_bg_field_selector
            },
        )
    }
}
