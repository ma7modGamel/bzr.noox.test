import BzrCore
import DesignSystem
import SwiftUI

public struct C14AddressesView: View {
    let state: CustomerUIState
    let onSelect: (Int) -> Void
    let onEdit: (Int) -> Void
    let onDelete: (Int) -> Void
    let onCancelDelete: () -> Void
    let onConfirmDelete: () -> Void
    let onAdd: () -> Void

    public init(
        state: CustomerUIState, onSelect: @escaping (Int) -> Void = { _ in },
        onEdit: @escaping (Int) -> Void = { _ in },
        onDelete: @escaping (Int) -> Void = { _ in },
        onCancelDelete: @escaping () -> Void = {}, onConfirmDelete: @escaping () -> Void = {},
        onAdd: @escaping () -> Void = {}
    ) {
        self.state = state
        self.onSelect = onSelect
        self.onEdit = onEdit
        self.onDelete = onDelete
        self.onCancelDelete = onCancelDelete
        self.onConfirmDelete = onConfirmDelete
        self.onAdd = onAdd
    }

    public var body: some View {
        ZStack(alignment: .bottom) {
            CustomerScreen(title: bzrString("address.list.title"), state: state) {
                ScreenHeading(bzrString("address.list.heading"), detail: bzrString("address.list.body"))
                if state.items.isEmpty {
                    EmptyState(title: bzrString("address.empty.title"), body: bzrString("address.empty.body"))
                } else {
                    ForEach(Array(state.items.enumerated()), id: \.offset) { index, label in
                        VStack(spacing: DesignSpace.s) {
                            SummaryCard(
                                title: label,
                                rows: [
                                    (
                                        .location,
                                        state.itemDetails.indices.contains(index) ? state.itemDetails[index] : ""
                                    )
                                ],
                                editText: bzrString("common.edit"), onEdit: { onEdit(index) })
                            if state.selectionMode {
                                Button(
                                    action: { onSelect(index) },
                                    label: {
                                        SelectableChip(
                                            text: bzrString("address.select"),
                                            state: state.selectedIndex == index ? .selected : .unselected)
                                    }
                                )
                                .buttonStyle(BzrPressStyle())
                            }
                            DangerTextButton(text: bzrString("address.delete"), onClick: { onDelete(index) })
                        }
                    }
                }
                PrimaryButton(
                    text: bzrString("address.add"), state: state.isBusy ? .disabled : .normal,
                    onClick: onAdd)
            }
            if state.showConfirmation {
                AppBottomSheet(
                    title: bzrString("address.delete.confirm.title"),
                    body: bzrString("address.delete.confirm.body"),
                    action: bzrString("address.delete.confirm.action"),
                    secondaryAction: bzrString("common.cancel"),
                    onAction: onConfirmDelete, onSecondary: onCancelDelete)
            }
        }
    }
}
