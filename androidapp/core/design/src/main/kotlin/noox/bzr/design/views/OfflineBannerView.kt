package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewOfflineBannerBinding

/** 43 §3 OfflineBanner (38 §9): icon and text on a tinted card. */
class OfflineBannerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val binding: ViewOfflineBannerBinding

    var text: String = ""
        set(value) {
            field = value
            binding.text.text = value
        }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        gap(R.drawable.bremo_gap_m)
        setBackgroundResource(R.drawable.bremo_bg_banner_offline)
        val padding = px(R.dimen.bremo_space_l)
        setPadding(padding, padding, padding, padding)
        binding = ViewOfflineBannerBinding.inflate(inflater, this)
        context.withStyledAttributes(attrs, R.styleable.BannerView) {
            text = getString(R.styleable.BannerView_bremoText).orEmpty()
        }
    }
}
