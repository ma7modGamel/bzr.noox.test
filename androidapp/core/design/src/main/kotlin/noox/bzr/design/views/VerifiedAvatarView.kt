package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import noox.bzr.design.databinding.ViewVerifiedAvatarBinding

/** Avatar circle with the verified shield at its bottom-start corner (ProviderHeader, OfferCard). */
class VerifiedAvatarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    init {
        ViewVerifiedAvatarBinding.inflate(inflater, this)
    }
}
