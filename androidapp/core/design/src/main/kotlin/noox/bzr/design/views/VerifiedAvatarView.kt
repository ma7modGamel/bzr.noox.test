package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import noox.bzr.design.databinding.ViewVerifiedAvatarBinding

/** Avatar circle with the verified shield at its bottom-start corner (ProviderHeader, OfferCard). */
class VerifiedAvatarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val binding = ViewVerifiedAvatarBinding.inflate(inflater, this)
    var verified: Boolean = false
        set(value) { field = value; binding.shield.showIf(value) }

    init { binding.shield.showIf(false) }
}
