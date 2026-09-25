package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.content.withStyledAttributes
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewMenuRowBinding

/** 43 §3 MenuRow: icon box, text, chevron (C02 menu). */
class MenuRowView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val binding: ViewMenuRowBinding

    @DrawableRes
    var icon: Int = R.drawable.ic_home
        set(value) {
            field = value
            binding.icon.icon(value, R.color.bremo_navy800)
        }
    var text: String = ""
        set(value) {
            field = value
            binding.text.text = value
        }
    var onClick: () -> Unit = {}

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        gap(R.drawable.bremo_gap_m)
        val vertical = px(R.dimen.bremo_space_s)
        setPadding(0, vertical, 0, vertical)
        binding = ViewMenuRowBinding.inflate(inflater, this)
        setOnClickListener { onClick() }
        context.withStyledAttributes(attrs, R.styleable.MenuRowView) {
            resource(R.styleable.MenuRowView_bremoIcon)?.let { icon = it }
            text = getString(R.styleable.MenuRowView_bremoText).orEmpty()
        }
    }
}
