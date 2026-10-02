package noox.bzr.customer

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.FieldVisualState
import noox.bzr.design.R
import noox.bzr.design.SelectionState
import noox.bzr.design.views.AppTextFieldView
import noox.bzr.design.views.SelectableChipView
import noox.bzr.gallery.databinding.ScreenC15AddressFormBinding

/** SCR-C15 (DEC-047): add / edit address. */
class C15AddressFormView(context: Context, private val editing: Boolean = false) : CustomerScreenView(context) {
    private val binding = ScreenC15AddressFormBinding.inflate(inflater, content)
    var onAreaSelect: (Int) -> Unit = {}
    var onFieldChange: (String, String) -> Unit = { _, _ -> }
    var onDefaultChange: () -> Unit = {}
    var onSave: () -> Unit = {}
    var onMapStateRender: (Double?, Double?) -> Unit = { _, _ -> }

    private data class Area(val index: Int, val label: String, val selected: Boolean)

    private val areas = RowAdapter<Area, SelectableChipView>(
        create = { parent ->
            SelectableChipView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
        },
        bind = { chip, area, _ ->
            chip.text = area.label
            chip.state = selection(area.selected)
            chip.setOnClickListener { onAreaSelect(area.index) }
        },
        key = { it.index },
    )

    fun attachNativeMap(view: View, onMyLocation: () -> Unit) {
        binding.map.setNativeContent(view)
        binding.map.onMyLocation = onMyLocation
    }

    private val fields by lazy {
        listOf(
            binding.label to "label", binding.details to "address_text", binding.building to "building",
            binding.floor to "floor", binding.apartment to "apartment", binding.landmark to "landmark",
        )
    }

    init {
        binding.areas.rows(areas, resources.getDimensionPixelSize(R.dimen.bremo_space_m), horizontal = true)
        fields.forEach { (field, name) -> field.onValueChange = { onFieldChange(name, it) } }
        binding.isDefault.setOnClickListener { onDefaultChange() }
        binding.save.onClick = { onSave() }
    }

    override fun title(state: CustomerUiState) =
        string(if (editing || state.isEditing) R.string.address_form_edit_title else R.string.address_form_add_title)

    override fun renderContent(state: CustomerUiState) {
        onMapStateRender(state.mapLatitude, state.mapLongitude)
        val values = state.fieldValues + List((6 - state.fieldValues.size).coerceAtLeast(0)) { "" }
        areas.submitList(
            state.options.mapIndexed { index, label ->
                Area(index, label, state.selectedOptionIndex == index || (state.selectedOptionIndex == -1 && index == 0))
            },
        )
        fields.forEachIndexed { index, (field, _) ->
            val errorKey = when (index) {
                0 -> "address.validation.label_required"
                1 -> "address.validation.details_required"
                else -> null
            }
            field(field, values[index], errorKey != null && errorKey in state.fieldErrors, index == 0)
        }
        binding.isDefault.checked = state.isDefault
        binding.areaRequired.isVisible = "address.validation.area_required" in state.fieldErrors
        binding.locationRequired.isVisible = "address.validation.location_required" in state.fieldErrors
        binding.unserved.isVisible = state.messageKey != null
        binding.save.state = when {
            state.isBusy -> ButtonVisualState.Loading
            state.canContinue -> ButtonVisualState.Normal
            else -> ButtonVisualState.Disabled
        }
    }

    private fun field(view: AppTextFieldView, value: String, hasError: Boolean, isLabel: Boolean) {
        view.value = value
        view.state = if (hasError) FieldVisualState.Error else if (value.isBlank()) FieldVisualState.Empty else FieldVisualState.Filled
        view.error = if (hasError) string(if (isLabel) R.string.address_validation_label_required else R.string.address_validation_details_required) else null
    }

    private fun selection(selected: Boolean) = if (selected) SelectionState.Selected else SelectionState.Unselected
}
