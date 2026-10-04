package noox.bzr.customer

import android.content.Context
import android.widget.LinearLayout
import androidx.core.view.isVisible
import noox.bzr.design.FieldVisualState
import noox.bzr.design.MediaKind
import noox.bzr.design.MediaState
import noox.bzr.design.R
import noox.bzr.design.SelectionState
import noox.bzr.design.views.SelectableChipView
import noox.bzr.design.views.SelectableTileView
import noox.bzr.gallery.databinding.ScreenC03ProblemBinding

/** SCR-C03 (DEC-047): problem type, description, media. */
class C03ProblemView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC03ProblemBinding.inflate(inflater, content)
    var onCategorySelect: (Int) -> Unit = {}
    var onProblemSelect: (Int) -> Unit = {}
    var onDescriptionChange: (String) -> Unit = {}
    var onOpen: (String) -> Unit = {}

    init {
        pinAction(binding.next)
        addRequestProgress(1)
        binding.description.onValueChange = { onDescriptionChange(it) }
        binding.addMedia.onClick = { onOpen("SCR-C17") }
        binding.next.onClick = { onOpen("SCR-C04") }
        binding.thumb.title = string(R.string.media_photo)
        binding.thumb.stateLabel = string(R.string.media_uploaded)
        binding.thumb.kind = MediaKind.Photo
        binding.thumb.state = MediaState.Uploaded
    }

    override fun title(state: CustomerUiState) = string(R.string.request_problem_title)

    override fun renderContent(state: CustomerUiState) {
        renderCategories(state)
        binding.problemList.removeAllViews()
        state.itemDetails.forEachIndexed { index, label ->
            binding.problemList.addView(
                SelectableChipView(context).apply {
                    text = label
                    this.state = if (index == state.selectedOptionIndex) SelectionState.Selected else SelectionState.Unselected
                    setOnClickListener { onProblemSelect(index) }
                },
                LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
            )
        }
        binding.description.value = state.fieldValues.firstOrNull().orEmpty()
        binding.description.state = if (state.descriptionError == null) FieldVisualState.Filled else FieldVisualState.Error
        binding.description.error = state.descriptionError?.let {
            string(
                when (it) {
                    "request.description.other_min" -> R.string.request_description_other_min
                    "request.description.max" -> R.string.request_description_max
                    else -> R.string.request_problem_required
                },
            )
        }
        binding.thumb.isVisible = state.fieldValues.getOrNull(1)?.toIntOrNull()?.let { it > 0 } == true
        binding.next.state = continueState(state)
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
