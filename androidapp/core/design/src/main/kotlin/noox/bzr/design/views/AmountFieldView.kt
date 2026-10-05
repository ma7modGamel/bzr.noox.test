package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import noox.bzr.design.FieldVisualState
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewAmountFieldBinding

/** 43 §3 AmountField: number with the currency box at the end. */
class AmountFieldView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FieldShellView(context, attrs) {
    private val binding = ViewAmountFieldBinding.inflate(inflater, this)
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
    var currency: String = ""
        set(value) {
            field = value
            binding.currency.text = value
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
    var onValueChange: ((String) -> Unit)? = null
        set(value) {
            field = value
            render()
        }

    init {
        val end = px(R.dimen.bremo_space_xs) + px(R.dimen.bremo_size_currency_box_width) + px(R.dimen.bremo_space_s)
        binding.edit.setPaddingRelative(binding.edit.paddingStart, 0, end, 0)
        bindShell()
        readShell(attrs) { label, placeholder, value, state, error, optional, currency ->
            this.label = label
            this.placeholder = placeholder
            this.value = value
            this.state = state
            this.error = error
            this.optionalLabel = optional
            this.currency = currency.orEmpty()
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
    }
}
