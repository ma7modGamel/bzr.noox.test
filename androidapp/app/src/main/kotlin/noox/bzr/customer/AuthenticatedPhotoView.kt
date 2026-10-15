package noox.bzr.customer

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.view.Gravity
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.design.views.SecondaryButtonView

/** Private image preview, with retry and a zoomable viewer. The injected loader keeps credentials out of URLs. */
internal class AuthenticatedPhotoView(context: Context) : LinearLayout(context) {
    var loadImage: (String, (Bitmap?) -> Unit) -> Unit = { _, ready -> ready(null) }
    private val photo = ImageView(context).apply { scaleType = ImageView.ScaleType.FIT_CENTER; contentDescription = context.getString(R.string.media_viewer_open) }
    private val progress = ProgressBar(context)
    private val failure = TextView(context).apply { text = context.getString(R.string.media_viewer_failed) }
    private val retry = SecondaryButtonView(context).apply { text = context.getString(R.string.media_viewer_retry); onClick = { fetch() } }
    private var url = ""
    private var bitmap: Bitmap? = null
    private var request = 0

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        addView(photo, LayoutParams(LayoutParams.MATCH_PARENT, resources.getDimensionPixelSize(R.dimen.bremo_size_map_card_height)))
        addView(progress)
        addView(failure)
        addView(retry)
        photo.setOnClickListener { bitmap?.let(::open) }
        isVisible = false
    }

    fun bind(url: String) {
        if (this.url == url && isVisible) return
        this.url = url
        isVisible = url.isNotBlank()
        if (isVisible) fetch()
    }

    private fun fetch() {
        val sequence = ++request
        bitmap = null
        photo.isVisible = false
        failure.isVisible = false
        retry.isVisible = false
        progress.isVisible = true
        progress.contentDescription = context.getString(R.string.media_viewer_loading)
        loadImage(url) { result ->
            if (request == sequence) {
                bitmap = result
                progress.isVisible = false
                photo.isVisible = result != null
                failure.isVisible = result == null
                retry.isVisible = result == null
                photo.setImageBitmap(result)
            }
        }
    }

    private fun open(bitmap: Bitmap) {
        val dialog = Dialog(context)
        val body = LinearLayout(context).apply { orientation = VERTICAL; setBackgroundResource(R.color.bremo_surface) }
        val image = ImageView(context).apply { setImageBitmap(bitmap); scaleType = ImageView.ScaleType.FIT_CENTER; contentDescription = context.getString(R.string.media_viewer_open) }
        var zoom = 1f
        val detector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                zoom = (zoom * detector.scaleFactor).coerceIn(1f, 4f)
                image.scaleX = zoom
                image.scaleY = zoom
                return true
            }
        })
        image.setOnTouchListener { _, event -> detector.onTouchEvent(event); true }
        body.addView(image, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        body.addView(SecondaryButtonView(context).apply { text = context.getString(R.string.media_viewer_close); onClick = { dialog.dismiss() } })
        dialog.setContentView(body)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
}

internal fun photoSection(context: Context, title: String, urls: List<String>, loader: (String, (Bitmap?) -> Unit) -> Unit): View = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL
    isVisible = urls.isNotEmpty()
    if (urls.isNotEmpty()) {
        addView(TextView(context).apply { text = title; androidx.core.widget.TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Bremo_HomeSection) })
        urls.forEach { url -> addView(AuthenticatedPhotoView(context).apply { loadImage = loader; bind(url) }) }
    }
}
