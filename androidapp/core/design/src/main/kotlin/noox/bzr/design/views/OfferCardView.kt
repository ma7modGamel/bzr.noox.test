package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewOfferCardBinding

/**
 * 43 §3 OfferCard, after Figma «Primo App» 3002:446 (DEC-073): avatar (initial) with a backend availability indicator, name and
 * «موثّق», subtitle, rating and review count, price and its caption; an arrival / materials strip; the
 * technician's note; «اختيار العرض ←», «عرض الملف» and chat.
 */
class OfferCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : BremoLinearLayout(context, attrs) {
    private val binding: ViewOfferCardBinding

    var provider: String = ""
        set(value) {
            field = value
            binding.provider.text = value
            binding.avatar.text = value.trim().take(1)
        }
    var subtitle: String = ""
        set(value) {
            field = value
            binding.subtitle.text = value
            binding.subtitle.showIf(value.isNotEmpty())
        }
    var rating: String = ""
        set(value) {
            field = value
            binding.rating.text = value
            binding.ratingRow.showIf(value.isNotEmpty())
        }
    var reviews: String = ""
        set(value) {
            field = value
            binding.reviews.text = value
            binding.reviews.showIf(value.isNotEmpty())
        }
    var price: String = ""
        set(value) {
            field = value
            binding.price.text = value
        }
    var priceCaption: String = ""
        set(value) {
            field = value
            binding.priceCaption.text = value
            binding.priceCaption.showIf(value.isNotEmpty())
        }
    var detail: String = ""
        set(value) {
            field = value
            binding.detail.text = value
            renderStrip()
        }
    var materials: String = ""
        set(value) {
            field = value
            binding.materials.text = value
            binding.materials.showIf(value.isNotEmpty())
            renderStrip()
        }
    var note: String = ""
        set(value) {
            field = value
            binding.note.text = value
            binding.note.showIf(value.isNotEmpty())
        }
    var verified: Boolean = false
        set(value) {
            field = value
            binding.verified.showIf(value)
        }
    var verifiedLabel: String = ""
        set(value) {
            field = value
            binding.verified.text = value
        }
    var button: String = ""
        set(value) {
            field = value
            binding.select.text = value
        }
    var profileLabel: String = ""
        set(value) {
            field = value
            binding.profile.text = value
        }
    var chatLabel: String = ""
        set(value) {
            field = value
            binding.chat.label = value
        }
    var showSelect: Boolean = true
        set(value) {
            field = value
            binding.select.showIf(value)
        }
    var showChat: Boolean = true
        set(value) {
            field = value
            binding.chat.showIf(value)
        }
    var availableNow: Boolean = false
        set(value) { field = value; binding.availability.showIf(value) }

    var onOpen: () -> Unit = {}
    var onSelect: () -> Unit = {}
    var onChat: () -> Unit = {}

    init {
        orientation = VERTICAL
        gravity = Gravity.FILL_HORIZONTAL
        gap(R.drawable.bremo_gap_m)
        setBackgroundResource(R.drawable.bremo_bg_card)
        cardShadow()
        val padding = px(R.dimen.bremo_space_xl)
        setPadding(padding, padding, padding, padding)
        binding = ViewOfferCardBinding.inflate(inflater, this)
        binding.select.onClick = { onSelect() }
        binding.profile.onClick = { onOpen() }
        binding.chat.onClick = { onChat() }
        binding.verified.showIf(false)
        binding.verified.sizeCompoundIcons(R.dimen.bremo_size_badge_icon)
        binding.detail.sizeCompoundIcons(R.dimen.bremo_size_icon_small)
        binding.materials.sizeCompoundIcons(R.dimen.bremo_size_icon_small)
    }

    private fun renderStrip() {
        binding.detail.showIf(detail.isNotEmpty())
        binding.strip.showIf(detail.isNotEmpty() || materials.isNotEmpty())
    }
}
