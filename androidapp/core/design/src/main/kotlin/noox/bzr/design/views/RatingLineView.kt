package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewRatingLineBinding

/** Star + rating, then the optional services count (ProviderHeader, OfferCard). */
class RatingLineView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : BremoLinearLayout(context, attrs) {
    private val binding: ViewRatingLineBinding

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        gap(R.drawable.bremo_gap_s)
        binding = ViewRatingLineBinding.inflate(inflater, this)
    }

    fun bind(rating: String, services: String?) {
        (binding.rating.parent as android.view.View).showIf(rating.isNotBlank())
        binding.rating.text = rating
        binding.services.text = services.orEmpty()
        binding.services.showIf(!services.isNullOrBlank())
    }
}
