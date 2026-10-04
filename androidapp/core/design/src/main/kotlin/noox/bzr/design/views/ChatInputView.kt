package noox.bzr.design.views

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewChatInputBinding

/** 43 §3 ChatInput: text box with the send icon. Static without [onValueChange], as in the gallery. */
class ChatInputView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val binding: ViewChatInputBinding
    private var syncing = false

    var placeholder: String = ""
        set(value) {
            field = value
            binding.edit.hint = placeholder(value)
        }
    var value: String = ""
        set(value) {
            field = value
            binding.send.isEnabled = value.isNotBlank()
            binding.send.applyEnabledAlpha(value.isNotBlank())
            if (binding.edit.text.toString() != value) {
                syncing = true
                binding.edit.setText(value)
                syncing = false
            }
        }
    var onValueChange: ((String) -> Unit)? = null
        set(value) {
            field = value
            binding.edit.isFocusable = value != null
            binding.edit.isFocusableInTouchMode = value != null
            binding.edit.isCursorVisible = value != null
        }
    var onSend: () -> Unit = {}

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = px(R.dimen.bremo_size_field_height)
        gap(R.drawable.bremo_gap_s)
        setBackgroundResource(R.drawable.bremo_bg_field)
        val horizontal = px(R.dimen.bremo_space_m)
        setPaddingRelative(horizontal, 0, horizontal, 0)
        binding = ViewChatInputBinding.inflate(inflater, this)
        binding.send.setOnClickListener { if (binding.edit.text.isNotBlank()) onSend() }
        binding.edit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                binding.send.isEnabled = !s.isNullOrBlank()
                binding.send.applyEnabledAlpha(!s.isNullOrBlank())
                if (!syncing) onValueChange?.invoke(s?.toString().orEmpty())
            }
        })
        onValueChange = null
        context.withStyledAttributes(attrs, R.styleable.ChatInputView) {
            placeholder = getString(R.styleable.ChatInputView_bremoPlaceholder).orEmpty()
            value = getString(R.styleable.ChatInputView_bremoValue).orEmpty()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) =
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
}
