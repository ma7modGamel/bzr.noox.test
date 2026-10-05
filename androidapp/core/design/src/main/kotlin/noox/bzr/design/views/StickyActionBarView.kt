package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewStickyActionBarBinding

/** 43 §3 StickyActionBar: price, primary action, chat square (C07). */
class StickyActionBarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : BremoLinearLayout(context, attrs) {
    private val binding: ViewStickyActionBarBinding

    var priceLabel: String = ""
        set(value) {
            field = value
            binding.priceLabel.text = value
        }
    var price: String = ""
        set(value) {
            field = value
            binding.price.text = value
        }
    var action: String = ""
        set(value) {
            field = value
            binding.action.text = value
        }
    var chatLabel: String = ""
        set(value) {
            field = value
            binding.chat.label = value
        }
    var onAction: () -> Unit = {}
    var onChat: () -> Unit = {}

    init {
        orientation = VERTICAL
        setBackgroundResource(R.color.bremo_surface)
        binding = ViewStickyActionBarBinding.inflate(inflater, this)
        binding.action.onClick = { onAction() }
        binding.chat.onClick = { onChat() }
    }
}
