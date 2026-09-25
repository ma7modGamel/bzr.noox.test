package noox.bzr.customer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.EmptyState
import noox.bzr.design.PrimaryButton
import noox.bzr.design.R
import noox.bzr.design.SelectionState
import noox.bzr.design.SelectableChip
import noox.bzr.design.generated.DesignSpace

@Composable
fun C16SlotPickerScreen(
    state: CustomerUiState,
    onDaySelect: (Int) -> Unit = {},
    onSlotSelect: (Int) -> Unit = {},
    onConfirm: () -> Unit = {},
) {
    CustomerScreen(stringResource(R.string.slot_title), state) {
        ScreenHeading(stringResource(R.string.slot_day_title))
        Row(
            horizontalArrangement = Arrangement.spacedBy(DesignSpace.s),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            state.options.forEachIndexed { index, day ->
                Box(Modifier.clickable { onDaySelect(index) }) {
                    SelectableChip(day, if (state.selectedOptionIndex == index) SelectionState.Selected else SelectionState.Unselected)
                }
            }
        }
        ScreenHeading(stringResource(R.string.slot_period_title))
        if (state.items.isEmpty()) {
            EmptyState(stringResource(R.string.slot_empty_title), stringResource(R.string.slot_empty_body))
        } else {
            state.items.chunked(2).forEach { row ->
                TwoColumns(
                    { SlotChip(row[0], state.items.indexOf(row[0]), state, onSlotSelect) },
                    { row.getOrNull(1)?.let { SlotChip(it, state.items.indexOf(it), state, onSlotSelect) } },
                )
            }
        }
        PrimaryButton(
            stringResource(R.string.slot_confirm),
            if (state.canContinue) ButtonVisualState.Normal else ButtonVisualState.Disabled,
            onClick = onConfirm,
        )
    }
}

@Composable
private fun SlotChip(label: String, index: Int, state: CustomerUiState, onSelect: (Int) -> Unit) {
    Box(Modifier.clickable { onSelect(index) }) {
        SelectableChip(label, if (state.selectedIndex == index) SelectionState.Selected else SelectionState.Unselected)
    }
}
