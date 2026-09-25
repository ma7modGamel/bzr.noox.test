package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import noox.bzr.design.AvatarSize
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewDrawerMenuBinding

/** 43 §3 DrawerMenu: the content placed inside the NavigationView of a DrawerLayout. */
class DrawerMenuView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val binding: ViewDrawerMenuBinding
    private val rowViews = mutableListOf<MenuRowView>()

    var name: String = ""
        set(value) {
            field = value
            binding.header.name = value
        }
    var rating: String = ""
        set(value) {
            field = value
            binding.header.rating = value
        }
    var verifiedLabel: String = ""
        set(value) {
            field = value
            binding.header.verifiedLabel = value
        }
    var rows: List<Pair<Int, String>> = emptyList()
        set(value) {
            field = value
            renderRows()
        }
    var action: String = ""
        set(value) {
            field = value
            binding.action.text = value
        }
    var onAction: () -> Unit = {}

    init {
        orientation = VERTICAL
        gap(R.drawable.bremo_gap_s)
        binding = ViewDrawerMenuBinding.inflate(inflater, this)
        binding.header.size = AvatarSize.Large
        binding.header.services = null
        binding.action.onClick = { onAction() }
    }

    private fun renderRows() {
        rowViews.forEach { removeView(it) }
        rowViews.clear()
        val insertAt = indexOfChild(binding.bottomSpacer)
        rows.forEachIndexed { index, (icon, text) ->
            val row = MenuRowView(context).apply {
                this.icon = icon
                this.text = text
            }
            addView(row, insertAt + index, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            rowViews += row
        }
    }
}
