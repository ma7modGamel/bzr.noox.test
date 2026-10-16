package noox.bzr.customer

import android.content.Context
import android.util.AttributeSet
import android.graphics.Bitmap
import androidx.appcompat.widget.AppCompatImageView
import noox.bzr.design.R

class AccountAvatarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : AppCompatImageView(context, attrs) {
    var loadImage: (String, (Bitmap?) -> Unit) -> Unit = { _, ready -> ready(null) }
    private var boundUrl = ""
    init {
        scaleType = ScaleType.CENTER_CROP
        setBackgroundResource(R.drawable.bremo_bg_avatar_ring)
        clipToOutline = true
        setImageResource(R.drawable.ic_account)
    }
    fun bind(url: String) {
        if (boundUrl == url) return
        boundUrl = url
        setImageResource(R.drawable.ic_account)
        if (url.isBlank()) return
        loadImage(url) { bitmap ->
            if (boundUrl == url && bitmap != null) setImageBitmap(bitmap)
        }
    }
}
