package noox.bzr.customer

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import noox.bzr.design.AppTextField
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.CheckRow
import noox.bzr.design.FieldVisualState
import noox.bzr.design.InfoBanner
import noox.bzr.design.MapCard
import noox.bzr.design.PrimaryButton
import noox.bzr.design.R
import noox.bzr.design.SelectionState
import noox.bzr.design.SelectableChip

@Composable
fun C15AddressFormScreen(
    state: CustomerUiState,
    editing: Boolean = false,
    onAreaSelect: (Int) -> Unit = {},
    onFieldChange: (String, String) -> Unit = { _, _ -> },
    onDefaultChange: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    val values = state.fieldValues + List((6 - state.fieldValues.size).coerceAtLeast(0)) { "" }
    val areas = state.options.ifEmpty {
        listOf(
            stringResource(R.string.address_area_nasr_city),
            stringResource(R.string.address_area_heliopolis),
            stringResource(R.string.address_area_maadi),
        )
    }
    CustomerScreen(
        stringResource(if (editing || state.isEditing) R.string.address_form_edit_title else R.string.address_form_add_title),
        state,
    ) {
        MapCard(stringResource(R.string.address_map_title))
        ScreenHeading(stringResource(R.string.address_area_title))
        TwoColumns(
            { androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.clickable { onAreaSelect(0) }) { SelectableChip(areas[0], if (state.selectedOptionIndex in listOf(-1, 0)) SelectionState.Selected else SelectionState.Unselected) } },
            { androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.clickable { onAreaSelect(1) }) { SelectableChip(areas.getOrElse(1) { "" }, if (state.selectedOptionIndex == 1) SelectionState.Selected else SelectionState.Unselected) } },
        )
        if (areas.size > 2) androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.clickable { onAreaSelect(2) }) { SelectableChip(areas[2], if (state.selectedOptionIndex == 2) SelectionState.Selected else SelectionState.Unselected) }
        AddressField(R.string.address_field_label, R.string.address_field_label_placeholder, values[0], "address.validation.label_required" in state.fieldErrors) { onFieldChange("label", it) }
        AddressField(R.string.address_field_details, R.string.address_field_details_placeholder, values[1], "address.validation.details_required" in state.fieldErrors) { onFieldChange("address_text", it) }
        TwoColumns(
            { AddressField(R.string.address_field_building, R.string.address_field_building, values[2], false) { onFieldChange("building", it) } },
            { AddressField(R.string.address_field_floor, R.string.address_field_floor, values[3], false) { onFieldChange("floor", it) } },
        )
        TwoColumns(
            { AddressField(R.string.address_field_apartment, R.string.address_field_apartment, values[4], false) { onFieldChange("apartment", it) } },
            { AddressField(R.string.address_field_landmark, R.string.address_field_landmark, values[5], false) { onFieldChange("landmark", it) } },
        )
        androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.clickable(onClick = onDefaultChange)) {
            CheckRow(stringResource(R.string.address_default), checked = state.isDefault)
        }
        if ("address.validation.area_required" in state.fieldErrors) {
            InfoBanner(stringResource(R.string.address_validation_area_required))
        }
        if ("address.validation.location_required" in state.fieldErrors) {
            InfoBanner(stringResource(R.string.address_validation_location_required))
        }
        state.messageKey?.let { InfoBanner(stringResource(R.string.address_validation_unserved_area)) }
        PrimaryButton(
            stringResource(R.string.common_save),
            when {
                state.isBusy -> ButtonVisualState.Loading
                state.canContinue -> ButtonVisualState.Normal
                else -> ButtonVisualState.Disabled
            },
            onClick = onSave,
        )
    }
}

@Composable
private fun AddressField(label: Int, placeholder: Int, value: String, hasError: Boolean, onValueChange: (String) -> Unit) {
    AppTextField(
        stringResource(label), stringResource(placeholder), value,
        if (hasError) FieldVisualState.Error else if (value.isBlank()) FieldVisualState.Empty else FieldVisualState.Filled,
        if (hasError) stringResource(if (label == R.string.address_field_label) R.string.address_validation_label_required else R.string.address_validation_details_required) else null,
        onValueChange = onValueChange,
    )
}
