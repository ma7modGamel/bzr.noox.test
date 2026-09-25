package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.LinearLayout
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewBottomNavBinding

/** 43 §3 BottomNav on BottomNavigationView: items are (icon, label) pairs, as in SwiftUI. */
class BottomNavView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val binding: ViewBottomNavBinding

    var items: List<Pair<Int, String>> = emptyList()
        set(value) {
            field = value
            render()
        }
    var selectedIndex: Int = 0
        set(value) {
            field = value
            if (value in items.indices) binding.navigation.selectedItemId = value
        }
    var onSelect: (Int) -> Unit = {}

    init {
        orientation = VERTICAL
        binding = ViewBottomNavBinding.inflate(inflater, this)
        centreContent()
        binding.navigation.setOnItemSelectedListener { item ->
            onSelect(item.itemId)
            true
        }
    }

    private fun render() {
        val menu = binding.navigation.menu
        menu.clear()
        items.forEachIndexed { index, (icon, label) -> menu.add(Menu.NONE, index, index, label).setIcon(icon) }
        if (selectedIndex in items.indices) binding.navigation.selectedItemId = selectedIndex
    }

    /** Icon + xs + label centred in the bar height, as Compose and SwiftUI lay it out. */
    private fun centreContent() {
        val label = TextView(context).apply { appearance(R.style.TextAppearance_Bremo_Caption) }
        val labelHeight = label.paint.fontMetricsInt.let { it.descent - it.ascent }
        val content = px(R.dimen.bremo_size_icon) + px(R.dimen.bremo_space_xs) + labelHeight
        val vertical = maxOf(0, (px(R.dimen.bremo_size_bottom_nav_height) - content) / 2)
        binding.navigation.itemPaddingTop = vertical
        binding.navigation.itemPaddingBottom = vertical
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Item views are rebuilt lazily by the menu presenter, so this runs before every measure.
        tightenLabels(binding.navigation)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /** Labels without font padding, so icon + label keep the xs gap of the other platforms. */
    private fun tightenLabels(view: View) {
        if (view is TextView && view.includeFontPadding) view.includeFontPadding = false
        if (view is ViewGroup) (0 until view.childCount).forEach { tightenLabels(view.getChildAt(it)) }
    }
}
