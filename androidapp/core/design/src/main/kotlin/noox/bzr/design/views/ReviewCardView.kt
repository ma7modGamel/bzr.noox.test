package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import com.google.android.material.card.MaterialCardView
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewReviewCardBinding

/** 43 §3 ReviewCard: avatar, name, stars + verified mark, time, body, optional tag. */
class ReviewCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    MaterialCardView(context, attrs, com.google.android.material.R.attr.materialCardViewStyle) {
    private val binding = ViewReviewCardBinding.inflate(inflater, this)

    var name: String = ""
        set(value) {
            field = value
            binding.name.text = value
        }
    var body: String = ""
        set(value) {
            field = value
            binding.body.text = value
        }
    var verified: String = ""
        set(value) {
            field = value
            binding.verified.text = value
        }
    var time: String = ""
        set(value) {
            field = value
            binding.time.text = value
        }

    @get:JvmName("reviewTag")
    @set:JvmName("setReviewTag")
    var tag: String? = null
        set(value) {
            field = value
            binding.tag.text = value.orEmpty()
            binding.tag.showIf(value != null)
        }
    var rating: Int = 0
        set(value) {
            field = value
            renderStars()
        }

    init {
        val padding = px(R.dimen.bremo_space_card_padding)
        setContentPadding(padding, padding, padding, padding)
        binding.tag.showIf(false)
    }

    private fun renderStars() {
        binding.stars.removeAllViews()
        repeat(rating) {
            val star = ImageView(context).apply { icon(R.drawable.ic_star_filled, R.color.bremo_star) }
            binding.stars.addView(star, LinearLayout.LayoutParams(px(R.dimen.bremo_size_icon_small), px(R.dimen.bremo_size_icon_small)))
        }
    }
}
