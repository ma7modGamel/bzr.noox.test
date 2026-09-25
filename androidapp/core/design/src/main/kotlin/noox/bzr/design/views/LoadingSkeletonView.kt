package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.google.android.material.card.MaterialCardView
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewLoadingSkeletonBinding

/** 43 §3 LoadingSkeleton: static placeholder card (circle + two lines, the second at DesignRatio.skeletonShortLine). */
class LoadingSkeletonView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    MaterialCardView(context, attrs, com.google.android.material.R.attr.materialCardViewStyle) {
    init {
        val binding = ViewLoadingSkeletonBinding.inflate(inflater, this)
        val padding = px(R.dimen.bremo_space_card_padding)
        setContentPadding(padding, padding, padding, padding)
        contentDescription = resources.getString(R.string.a11y_loading)
        binding.shortRow.weightSum = 1f
        binding.shortLine.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, fraction(R.dimen.bremo_ratio_skeleton_short_line))
    }
}
