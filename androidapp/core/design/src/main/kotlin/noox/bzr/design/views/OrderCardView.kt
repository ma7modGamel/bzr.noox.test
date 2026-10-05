package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import com.google.android.material.card.MaterialCardView
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewOrderCardBinding

/** 43 §3 OrderCard: title, subtitle, status badge, chevron; the whole card opens the order. */
class OrderCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    MaterialCardView(context, attrs, com.google.android.material.R.attr.materialCardViewStyle) {
    private val binding = ViewOrderCardBinding.inflate(inflater, this)

    var title: String = ""
        set(value) {
            field = value
            binding.title.text = value
        }
    var subtitle: String = ""
        set(value) {
            field = value
            binding.subtitle.text = value
        }
    var status: String = ""
        set(value) {
            field = value
            binding.status.text = value
        }
    var onClick: () -> Unit = {}

    init {
        val padding = px(R.dimen.bremo_space_card_padding)
        setContentPadding(padding, padding, padding, padding)
        setOnClickListener { onClick() }
        context.withStyledAttributes(attrs, R.styleable.OrderCardView) {
            title = getString(R.styleable.OrderCardView_bremoTitle).orEmpty()
            subtitle = getString(R.styleable.OrderCardView_subtitle).orEmpty()
            status = getString(R.styleable.OrderCardView_status).orEmpty()
        }
    }
}
