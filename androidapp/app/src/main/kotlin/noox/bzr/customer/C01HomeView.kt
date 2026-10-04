package noox.bzr.customer

import android.content.Context
import android.widget.LinearLayout
import androidx.core.view.isVisible
import noox.bzr.design.SelectionState
import noox.bzr.design.R
import noox.bzr.design.views.OrderCardView
import noox.bzr.design.views.SelectableTileView
import noox.bzr.gallery.databinding.ScreenC01HomeBinding

/** SCR-C01 (DEC-047): categories, new request, current order. The bottom navigation lives in MainActivity (DEC-062). */
class C01HomeView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC01HomeBinding.inflate(inflater, content)
    var onOpen: (String) -> Unit = {}
    var onCategorySelect: (Int) -> Unit = {}
    var onOrderSelect: (Int) -> Unit = {}

    init {
        binding.newRequest.onClick = { onOpen("SCR-C03") }
    }

    override fun title(state: CustomerUiState) = string(R.string.nav_home)

    override fun renderContent(state: CustomerUiState) {
        val name = state.fieldValues.firstOrNull().orEmpty()
        binding.greeting.text = if (name.isBlank()) string(R.string.customer_greeting_plain) else context.getString(R.string.customer_greeting, name)
        binding.empty.isVisible = state.phase == CustomerPhase.Empty
        renderCategories(state)
        binding.orderList.removeAllViews()
        state.items.forEachIndexed { index, title ->
            binding.orderList.addView(
                OrderCardView(context).apply {
                    this.title = title
                    subtitle = state.itemDetails.getOrElse(index) { "" }
                    status = state.itemStates.getOrElse(index) { "" }
                    onClick = { onOrderSelect(index) }
                },
                LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
            )
        }
    }

    private fun renderCategories(state: CustomerUiState) {
        binding.categoryGrid.removeAllViews()
        state.options.chunked(categoryColumns()).forEachIndexed { rowIndex, labels ->
            val row = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                dividerDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bremo_gap_m)
                showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
            }
            labels.forEachIndexed { columnIndex, label ->
                val index = rowIndex * categoryColumns() + columnIndex
                row.addView(
                    SelectableTileView(context).apply {
                        text = label
                        categoryIcon = state.optionIcons.getOrNull(index)
                        this.state = if (index == state.selectedIndex) SelectionState.Selected else SelectionState.Unselected
                        setOnClickListener { onCategorySelect(index) }
                    },
                    LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f),
                )
            }
            repeat(categoryColumns() - labels.size) {
                row.addView(android.widget.Space(context), LinearLayout.LayoutParams(0, 0, 1f))
            }
            binding.categoryGrid.addView(row, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
    }

    private fun categoryColumns(): Int {
        val configuration = resources.configuration
        val available = configuration.screenWidthDp * resources.displayMetrics.density -
            resources.getDimension(R.dimen.bremo_space_screen_horizontal) * 2
        val cell = resources.getDimension(R.dimen.bremo_size_category_column_min_width) * configuration.fontScale
        val gap = resources.getDimension(R.dimen.bremo_space_m)
        return ((available + gap) / (cell + gap)).toInt().coerceIn(1, 3)
    }
}
