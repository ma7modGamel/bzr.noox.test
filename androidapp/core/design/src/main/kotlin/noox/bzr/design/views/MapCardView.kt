package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewMapCardBinding

/**
 * 43 §3 MapCard frame. On screens the map itself is a SupportMapFragment/MapView placed inside
 * this frame (43 §7 system element); the gallery shows the placeholder, as SwiftUI does.
 */
class MapCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val binding = ViewMapCardBinding.inflate(inflater, this)

    var title: String = ""
        set(value) {
            field = value
            binding.title.text = value
        }

    var onMyLocation: () -> Unit = {}

    init {
        setBackgroundResource(R.drawable.bremo_bg_map_card)
        clipToOutline = true
        context.withStyledAttributes(attrs, R.styleable.MapCardView) {
            title = getString(R.styleable.MapCardView_bremoTitle).orEmpty()
        }
        binding.myLocation.setOnClickListener { onMyLocation() }
    }

    /** Places a native Google Maps / MapKit-equivalent surface behind the shared frame controls. */
    fun setNativeContent(view: View) {
        (view.parent as? ViewGroup)?.removeView(view)
        binding.mapContent.removeAllViews()
        binding.mapContent.addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        binding.mapPlaceholder.isVisible = false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) =
        super.onMeasure(widthMeasureSpec, fixedHeight(px(R.dimen.bremo_size_map_card_height)))
}
