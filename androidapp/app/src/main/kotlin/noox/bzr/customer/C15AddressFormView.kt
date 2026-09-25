package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.FieldVisualState
import noox.bzr.design.R
import noox.bzr.design.SelectionState
import noox.bzr.design.views.AppTextFieldView
import noox.bzr.gallery.databinding.ScreenC15AddressFormBinding

/** SCR-C15 (DEC-047): add / edit address. */
class C15AddressFormView(context: Context, private val editing: Boolean = false) : CustomerScreenView(context) {
    private val binding = ScreenC15AddressFormBinding.inflate(inflater, content)
    var onAreaSelect: (Int) -> Unit = {}
    var onFieldChange: (String, String) -> Unit = { _, _ -> }
    var onDefaultChange: () -> Unit = {}
    var onSave: () -> Unit = {}

    private val fields by lazy {
        listOf(
            binding.label to "label", binding.details to "address_text", binding.building to "building",
            binding.floor to "floor", binding.apartment to "apartment", binding.landmark to "landmark",
        )
    }

    init {
        listOf(binding.area0, binding.area1, binding.area2).forEachIndexed { index, chip -> chip.setOnClickListener { onAreaSelect(index) } }
        fields.forEach { (field, name) -> field.onValueChange = { onFieldChange(name, it) } }
        binding.isDefault.setOnClickListener { onDefaultChange() }
        binding.save.onClick = { onSave() }
    }

    override fun title(state: CustomerUiState) =
        string(if (editing || state.isEditing) R.string.address_form_edit_title else R.string.address_form_add_title)

    override fun renderContent(state: CustomerUiState) {
        val values = state.fieldValues + List((6 - state.fieldValues.size).coerceAtLeast(0)) { "" }
        val areas = state.options.ifEmpty {
            listOf(string(R.string.address_area_nasr_city), string(R.string.address_area_heliopolis), string(R.string.address_area_maadi))
        }
        binding.area0.text = areas[0]
        binding.area0.state = selection(state.selectedOptionIndex in listOf(-1, 0))
        binding.area1.text = areas.getOrElse(1) { "" }
        binding.area1.state = selection(state.selectedOptionIndex == 1)
        binding.area2.isVisible = areas.size > 2
        binding.area2.text = areas.getOrElse(2) { "" }
        binding.area2.state = selection(state.selectedOptionIndex == 2)
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
