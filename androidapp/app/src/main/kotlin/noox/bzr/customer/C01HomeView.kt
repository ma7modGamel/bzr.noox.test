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
    var onUrgent: () -> Unit = {}

    override val showsTopBar = false

    init {
        binding.newRequest.setOnClickListener { onOpen("SCR-C03") }
        binding.urgent.setOnClickListener { onUrgent() }
        binding.urgentAction.setOnClickListener { onUrgent() }
        binding.location.setOnClickListener { onOpen("SCR-C14") }
        binding.bell.setOnClickListener { onOpen("SCR-C32") }
        binding.bell.contentDescription = string(R.string.notifications_title)
        binding.locationIcon.setIcon(R.drawable.ic_location)
        binding.bellIcon.setIcon(R.drawable.ic_bell)
        binding.urgentIcon.setIcon(R.drawable.ic_siren)
        binding.urgentIcon.setGradient(color(R.color.bremo_gradient_urgent_button_from), color(R.color.bremo_gradient_urgent_button_to))
        binding.newRequest.contentDescription = string(R.string.customer_home_hero_title)
    }

    override fun title(state: CustomerUiState) = string(R.string.nav_home)

    override fun renderContent(state: CustomerUiState) {
        val name = state.fieldValues.firstOrNull().orEmpty()
        binding.greeting.text = if (name.isBlank()) string(R.string.customer_greeting_plain) else context.getString(R.string.customer_greeting, name)
        binding.avatar.text = name.trim().take(1)
        binding.avatar.visibility = if (name.isBlank()) GONE else VISIBLE
        binding.locationLabel.text = state.fieldValues.getOrNull(1).orEmpty().ifBlank { string(R.string.customer_home_location_empty) }
        binding.heroBody.text = string(if (state.showPricing) R.string.customer_home_hero_body_marketplace else R.string.customer_home_hero_body_staff)
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

    /** DEC-063: equal columns from [noox.bzr.design.views.TileGridLayout], so no tile is clipped or pushed. */
    private fun renderCategories(state: CustomerUiState) {
        binding.categoryGrid.removeAllViews()
        state.options.forEachIndexed { index, label ->
            binding.categoryGrid.addView(
                SelectableTileView(context).apply {
                    text = label
                    categoryIcon = state.optionIcons.getOrNull(index)
                    this.state = if (index == state.selectedIndex) SelectionState.Selected else SelectionState.Unselected
                    setOnClickListener { onCategorySelect(index) }
                },
            )
        }
    }
}

private fun android.view.View.color(id: Int): Int = androidx.core.content.ContextCompat.getColor(context, id)
