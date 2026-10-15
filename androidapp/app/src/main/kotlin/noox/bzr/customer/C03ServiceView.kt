package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.design.SelectionState
import noox.bzr.design.views.SelectableTileView
import noox.bzr.gallery.databinding.ScreenC03ServiceBinding

/** SCR-C03, step 1 of 5 (DEC-071): search and pick the service. */
class C03ServiceView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC03ServiceBinding.inflate(inflater, content)
    var onCategorySelect: (Int) -> Unit = {}
    var onSearch: (String) -> Unit = {}
    var onOpen: (String) -> Unit = {}

    init {
        pinAction(binding.selectedRow)
        pinAction(binding.next)
        addRequestProgress(1)
        binding.search.onValueChange = { onSearch(it) }
        binding.next.onClick = { onOpen("SCR-C37") }
        binding.noResults.text = string(R.string.request_search_no_results)
    }

    override fun title(state: CustomerUiState) = string(R.string.request_service_title)

    override fun renderContent(state: CustomerUiState) {
        binding.search.value = state.fieldValues.firstOrNull().orEmpty()
        binding.noResults.isVisible = state.messageKey == "request.search.no_results"
        binding.sectionTitle.isVisible = state.options.isNotEmpty()
        binding.categoryGrid.removeAllViews()
        state.options.forEachIndexed { index, label ->
            binding.categoryGrid.addView(
                SelectableTileView(context).apply {
                    text = label
                    detail = state.itemDetails.getOrElse(index) { "" }.split('\n').joinToString(string(R.string.format_list_separator))
                    categoryIcon = state.optionIcons.getOrNull(index)
                    this.state = if (index == state.selectedIndex) SelectionState.Selected else SelectionState.Unselected
                    setOnClickListener { onCategorySelect(index) }
                },
            )
        }
        val selected = state.options.getOrNull(state.selectedIndex)
        binding.selectedRow.isVisible = selected != null
        binding.selectedName.text = selected.orEmpty()
        binding.next.state = continueState(state)
    }
}
