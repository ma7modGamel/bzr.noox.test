package noox.bzr.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.AppBottomSheet
import noox.bzr.design.DangerTextButton
import noox.bzr.design.EmptyState
import noox.bzr.design.PrimaryButton
import noox.bzr.design.R
import noox.bzr.design.SelectionState
import noox.bzr.design.SelectableChip
import noox.bzr.design.SummaryCard
import noox.bzr.design.generated.DesignSpace

@Composable
fun C14AddressesScreen(
    state: CustomerUiState,
    onSelect: (Int) -> Unit = {},
    onEdit: (Int) -> Unit = {},
    onDelete: (Int) -> Unit = {},
    onCancelDelete: () -> Unit = {},
    onConfirmDelete: () -> Unit = {},
    onAdd: () -> Unit = {},
) {
    Box(Modifier.fillMaxSize()) {
        CustomerScreen(stringResource(R.string.address_list_title), state) {
            ScreenHeading(stringResource(R.string.address_list_heading), stringResource(R.string.address_list_body))
            if (state.items.isEmpty()) {
                EmptyState(stringResource(R.string.address_empty_title), stringResource(R.string.address_empty_body))
            } else {
                state.items.forEachIndexed { index, label ->
                    Column(verticalArrangement = Arrangement.spacedBy(DesignSpace.s)) {
                        SummaryCard(
                            label,
                            listOf(R.drawable.ic_location to state.itemDetails.getOrElse(index) { "" }),
                            stringResource(R.string.common_edit),
                            onEdit = { onEdit(index) },
                        )
                        if (state.selectionMode) {
                            Box(Modifier.clickable { onSelect(index) }) {
                                SelectableChip(
                                    stringResource(R.string.address_select),
                                    if (state.selectedIndex == index) SelectionState.Selected else SelectionState.Unselected,
                                )
                            }
                        }
                        DangerTextButton(stringResource(R.string.address_delete), onClick = { onDelete(index) })
                    }
                }
            }
            PrimaryButton(
                stringResource(R.string.address_add),
                if (state.isBusy) ButtonVisualState.Disabled else ButtonVisualState.Normal,
                onClick = onAdd,
            )
        }
        if (state.showConfirmation) {
            Box(Modifier.align(Alignment.BottomCenter)) {
                AppBottomSheet(
                    title = stringResource(R.string.address_delete_confirm_title),
                    body = stringResource(R.string.address_delete_confirm_body),
                    action = stringResource(R.string.address_delete_confirm_action),
                    secondaryAction = stringResource(R.string.common_cancel),
                    onAction = onConfirmDelete,
                    onSecondary = onCancelDelete,
                )
            }
        }
    }
}
