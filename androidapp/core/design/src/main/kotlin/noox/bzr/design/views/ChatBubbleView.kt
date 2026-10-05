package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import noox.bzr.design.ChatKind
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewChatBubbleBinding

/** 43 §3 ChatBubble: sent (teal, start edge) or received (outlined, end edge); text, image, blocked. */
class ChatBubbleView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val binding = ViewChatBubbleBinding.inflate(inflater, this)

    var text: String = ""
        set(value) {
            field = value
            binding.text.text = value
        }
    var sent: Boolean = false
        set(value) {
            field = value
            render()
        }
    var kind: ChatKind = ChatKind.Text
        set(value) {
            field = value
            render()
        }

    init {
        context.withStyledAttributes(attrs, R.styleable.ChatBubbleView) {
            text = getString(R.styleable.ChatBubbleView_bremoText).orEmpty()
            sent = getBoolean(R.styleable.ChatBubbleView_bremoSent, false)
            kind = enumValue(R.styleable.ChatBubbleView_chatKind, ChatKind.entries.toTypedArray(), ChatKind.Text)
        }
        render()
    }

    private fun render() {
        val blocked = kind == ChatKind.Blocked
        val foreground = when {
            blocked -> R.color.bremo_danger
            sent -> R.color.bremo_on_primary
            else -> R.color.bremo_navy800
        }
        binding.bubble.setBackgroundResource(
            when {
                blocked -> R.drawable.bremo_bg_bubble_blocked
                sent -> R.drawable.bremo_bg_bubble_sent
                else -> R.drawable.bremo_bg_bubble_received
            },
        )
        (binding.bubble.layoutParams as LayoutParams).gravity = (if (sent) Gravity.START else Gravity.END) or Gravity.CENTER_VERTICAL
        binding.bubble.requestLayout()
        binding.text.setTextColor(color(foreground))
        binding.icon.showIf(kind != ChatKind.Text)
        when (kind) {
            ChatKind.Image -> binding.icon.icon(R.drawable.ic_image, foreground)
            ChatKind.Blocked -> binding.icon.icon(R.drawable.ic_warning, foreground)
            ChatKind.Text -> Unit
        }
    }
}
