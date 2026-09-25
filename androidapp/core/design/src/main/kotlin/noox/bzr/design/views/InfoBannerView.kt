package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewInfoBannerBinding

/** 43 §3 InfoBanner (teal): icon and text on a tinted card. */
class InfoBannerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val binding: ViewInfoBannerBinding

    var text: String = ""
        set(value) {
            field = value
            binding.text.text = value
        }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        gap(R.drawable.bremo_gap_m)
        setBackgroundResource(R.drawable.bremo_bg_banner_info)
        val padding = px(R.dimen.bremo_space_l)
        setPadding(padding, padding, padding, padding)
        binding = ViewInfoBannerBinding.inflate(inflater, this)
        context.withStyledAttributes(attrs, R.styleable.BannerView) {
            text = getString(R.styleable.BannerView_bremoText).orEmpty()
        }
    }
}
