package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.card.MaterialCardView
import noox.bzr.design.OfferVariant
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewOfferCardBinding

/** 43 §3 OfferCard: execution, inspection (price caption), scheduled (calendar instead of ETA). */
class OfferCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    MaterialCardView(context, attrs, com.google.android.material.R.attr.materialCardViewStyle) {
    private val binding = ViewOfferCardBinding.inflate(inflater, this)

    var provider: String = ""
        set(value) {
            field = value
            binding.provider.text = value
        }
    var rating: String = ""
        set(value) {
            field = value
            binding.ratingLine.bind(value, services)
        }
    var services: String = ""
        set(value) {
            field = value
            binding.ratingLine.bind(rating, value)
        }
    var price: String = ""
        set(value) {
            field = value
            binding.price.text = value
        }
    var detail: String = ""
        set(value) {
            field = value
            binding.detail.text = value
        }
    var badge: String = ""
        set(value) {
            field = value
            binding.badge.text = value
        }
    var button: String = ""
        set(value) {
            field = value
            binding.select.text = value
        }
    var chatLabel: String = ""
        set(value) {
            field = value
            binding.chat.label = value
        }
    var variant: OfferVariant = OfferVariant.Execution
        set(value) {
            field = value
            render()
        }
    var priceCaption: String? = null
        set(value) {
            field = value
            render()
        }
    var onSelect: () -> Unit = {}
    var onChat: () -> Unit = {}

    init {
        val padding = px(R.dimen.bremo_space_card_padding)
        setContentPadding(padding, padding, padding, padding)
        binding.select.onClick = { onSelect() }
        binding.chat.onClick = { onChat() }
        render()
    }

    private fun render() {
        binding.priceCaption.text = priceCaption.orEmpty()
        binding.priceCaption.showIf(variant == OfferVariant.Inspection && priceCaption != null)
        binding.detailIcon.icon(if (variant == OfferVariant.Scheduled) R.drawable.ic_calendar else R.drawable.ic_clock, R.color.bremo_slate500)
    }
}
