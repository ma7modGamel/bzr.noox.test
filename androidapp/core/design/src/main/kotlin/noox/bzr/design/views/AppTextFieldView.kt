package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import noox.bzr.design.FieldImeAction
import noox.bzr.design.FieldVisualState
import noox.bzr.design.databinding.ViewAppTextFieldBinding

/** 43 §3 AppTextField: single-line text field. */
class AppTextFieldView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FieldShellView(context, attrs) {
    private val binding = ViewAppTextFieldBinding.inflate(inflater, this)
    override val labelView get() = binding.label
    override val optionalView get() = binding.optional
    override val frameView get() = binding.frame
    override val editText get() = binding.edit
    override val errorView get() = binding.error
    private var ready = false

    var label: String = ""
        set(value) {
            field = value
            render()
        }
    var placeholder: String = ""
        set(value) {
            field = value
            render()
        }
    var value: String = ""
        set(value) {
            field = value
            render()
        }
    var state: FieldVisualState = FieldVisualState.Empty
        set(value) {
            field = value
            render()
        }
    var error: String? = null
        set(value) {
            field = value
            render()
        }
    var optionalLabel: String? = null
        set(value) {
            field = value
            render()
        }
    var imeAction: FieldImeAction = FieldImeAction.Next
        set(value) {
            field = value
            render()
        }
    var onValueChange: ((String) -> Unit)? = null
        set(value) {
            field = value
            render()
        }

    init {
        bindShell()
        readShell(attrs) { label, placeholder, value, state, error, optional, _ ->
            this.label = label
            this.placeholder = placeholder
            this.value = value
            this.state = state
            this.error = error
            this.optionalLabel = optional

        }
        ready = true
        render()
    }

    override fun changed(value: String) {
        onValueChange?.invoke(value)
    }

    private fun render() {
        if (!ready) return
        renderShell(label, placeholder, value, state, error, optionalLabel, onValueChange != null)
        editText.imeOptions = when (imeAction) {
            FieldImeAction.Next -> EditorInfo.IME_ACTION_NEXT
            FieldImeAction.Done -> EditorInfo.IME_ACTION_DONE
        }
    }
}
