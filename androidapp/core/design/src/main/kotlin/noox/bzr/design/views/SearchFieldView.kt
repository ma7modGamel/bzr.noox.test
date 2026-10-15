package noox.bzr.design.views

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import androidx.core.content.withStyledAttributes
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewSearchFieldBinding

/**
 * DEC-070 SearchField (Figma «Primo App» 3002:2): a filled 44dp box with a search glyph at the start and a clear
 * button once there is text. Without [onValueChange] the field is static (gallery and snapshots).
 */
class SearchFieldView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : BremoLinearLayout(context, attrs) {
    private val binding: ViewSearchFieldBinding
    private var syncing = false

    var onValueChange: ((String) -> Unit)? = null
        set(value) {
            field = value
            binding.input.isFocusable = value != null
            binding.input.isFocusableInTouchMode = value != null
        }

    var placeholder: String = ""
        set(value) {
            field = value
            binding.input.hint = value
            binding.input.contentDescription = value
        }

    var value: String = ""
        set(value) {
            field = value
            if (binding.input.text.toString() != value) {
                syncing = true
                binding.input.setText(value)
                syncing = false
            }
            binding.clear.showIf(value.isNotEmpty())
        }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundResource(R.drawable.bremo_bg_search)
        setPaddingRelative(px(R.dimen.bremo_space_m), 0, 0, 0)
        binding = ViewSearchFieldBinding.inflate(inflater, this)
        if (SnapshotMode.enabled) binding.input.setHorizontallyScrolling(false)
        binding.input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (syncing) return
                val text = s?.toString().orEmpty()
                binding.clear.showIf(text.isNotEmpty())
                onValueChange?.invoke(text)
            }
        })
        binding.clear.setOnClickListener {
            binding.input.setText("")
        }
        context.withStyledAttributes(attrs, R.styleable.FieldView) {
            placeholder = getString(R.styleable.FieldView_bremoPlaceholder).orEmpty()
            value = getString(R.styleable.FieldView_bremoValue).orEmpty()
        }
        onValueChange = null
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) =
        super.onMeasure(widthMeasureSpec, fixedHeight(px(R.dimen.bremo_size_search_field_height)))
}
