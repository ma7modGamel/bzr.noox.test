package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.content.withStyledAttributes
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewAppTopBarBinding

/** 43 §3 AppTopBar: back at the start edge, centred title, optional action at the end edge. */
class AppTopBarView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private val binding = ViewAppTopBarBinding.inflate(inflater, this)

    var title: String = ""
        set(value) {
            field = value
            binding.title.text = value
        }

    @DrawableRes
    var actionIcon: Int? = null
        set(value) {
            field = value
            renderAction()
        }
    var actionLabel: String? = null
        set(value) {
            field = value
            renderAction()
        }
    var onBack: () -> Unit = {}
    var onAction: () -> Unit = {}

    init {
        setBackgroundResource(R.color.bremo_surface)
        val horizontal = px(R.dimen.bremo_space_screen_horizontal)
        setPaddingRelative(horizontal, 0, horizontal, 0)
        binding.back.setOnClickListener { onBack() }
        binding.action.setOnClickListener { onAction() }
        context.withStyledAttributes(attrs, R.styleable.AppTopBarView) {
            title = getString(R.styleable.AppTopBarView_bremoTitle).orEmpty()
            actionIcon = resource(R.styleable.AppTopBarView_actionIcon)
            actionLabel = getString(R.styleable.AppTopBarView_actionLabel)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) =
        super.onMeasure(widthMeasureSpec, fixedHeight(px(R.dimen.bremo_size_top_bar_height)))

    private fun renderAction() {
        binding.action.showIf(actionIcon != null)
        actionIcon?.let { binding.action.icon(it, R.color.bremo_navy800) }
        binding.action.contentDescription = actionLabel
    }
}
