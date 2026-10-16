import BzrCore
import DesignSystem
import SwiftUI

public struct C14AddressesView: View {
    let state: CustomerUIState
    let onSelect: (Int) -> Void
    let onEdit: (Int) -> Void
    let onDelete: (Int) -> Void
    let onMakeDefault: (Int) -> Void
    let onCancelDelete: () -> Void
    let onConfirmDelete: () -> Void
    let onAdd: () -> Void
    let onRefresh: () -> Void
    @State private var expanded: Set<String> = []
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    public init(
        state: CustomerUIState, onSelect: @escaping (Int) -> Void = { _ in },
        onEdit: @escaping (Int) -> Void = { _ in }, onDelete: @escaping (Int) -> Void = { _ in },
        onMakeDefault: @escaping (Int) -> Void = { _ in },
        onCancelDelete: @escaping () -> Void = {}, onConfirmDelete: @escaping () -> Void = {},
        onAdd: @escaping () -> Void = {}, onRefresh: @escaping () -> Void = {}
    ) {
        self.state = state
        self.onSelect = onSelect
        self.onEdit = onEdit
        self.onDelete = onDelete
        self.onMakeDefault = onMakeDefault
        self.onCancelDelete = onCancelDelete
        self.onConfirmDelete = onConfirmDelete
        self.onAdd = onAdd
        self.onRefresh = onRefresh
    }

    public var body: some View {
        CustomerScreen(
            title: bzrString("address.list.title"), state: state,
            bottomBar: AnyView(PrimaryButton(text: bzrString("address.add"), state: state.isBusy ? .disabled : .normal, icon: .plus, onClick: onAdd))
        ) {
            ScreenHeading(
                state.items.isEmpty ? bzrString("address.list.heading") : bzrPlural("account.addresses_count", count: state.items.count),
                detail: bzrString(state.selectionMode ? "address.list.selection.body" : "address.list.body"))
            if let message = state.messageKey {
                BzrText(bzrString(message), style: DesignType.caption.colored(failed ? DesignColors.danger : DesignColors.success))
                    .accessibilityAddTraits(.updatesFrequently)
                if failed { SecondaryButton(text: bzrString("common.retry"), onClick: onRefresh) }
            }
            if state.items.isEmpty {
                EmptyState(title: bzrString("address.empty.title"), body: bzrString("address.empty.body"))
            } else {
                if state.primaryAddressIndex < 0 {
                    BzrText(bzrString("address.default.missing"), style: DesignType.caption)
                }
                VStack(spacing: DesignSpace.s) {
                    ForEach(orderedIndices, id: \.self) { index in addressCard(index) }
                }
            }
        }
        .sheet(
            isPresented: Binding(
                get: { state.showConfirmation },
                set: { shown in
                    if !shown && !state.isBusy { onCancelDelete() }
                })
        ) {
            AppBottomSheet(
                title: BzrFormatter.fill(bzrString("address.delete.named"), values: [("label", pendingLabel)]),
                body: bzrString(state.pendingIndex == state.primaryAddressIndex ? "address.delete.primary.body" : "address.delete.confirm.body"),
                action: bzrString("address.delete.confirm.action"), actionState: state.isBusy ? .loading : .normal,
                secondaryAction: bzrString("common.cancel"), onAction: onConfirmDelete, onSecondary: onCancelDelete
            )
            .disabled(state.isBusy)
            .presentationDetents(dynamicTypeSize.isAccessibilitySize ? [.large] : [.medium, .large])
            .presentationDragIndicator(.hidden)
            .interactiveDismissDisabled(state.isBusy)
        }
    }

    private var failed: Bool { ["address.delete.failed", "address.default.failed"].contains(state.messageKey ?? "") }
    private var pendingLabel: String { state.items.indices.contains(state.pendingIndex) ? state.items[state.pendingIndex] : "" }
    private var orderedIndices: [Int] {
        state.items.indices.sorted { ($0 == state.primaryAddressIndex) && ($1 != state.primaryAddressIndex) }
    }

    private func addressCard(_ index: Int) -> some View {
        let primary = index == state.primaryAddressIndex
        let selected = state.selectionMode && index == state.selectedIndex
        let id = state.optionCodes.indices.contains(index) ? state.optionCodes[index] : String(index)
        let actions = state.addressActions.indices.contains(index) ? state.addressActions[index] : []
        return HStack(alignment: .top, spacing: DesignSpace.s) {
            Button {
                if state.selectionMode { onSelect(index) } else if expanded.contains(id) { expanded.remove(id) } else { expanded.insert(id) }
            } label: {
                HStack(alignment: .top, spacing: DesignSpace.s) {
                    BzrIcon(selected ? .check : .location, tint: DesignColors.primary600)
                    VStack(alignment: .leading, spacing: DesignSpace.xs) {
                        BzrText(state.items[index], style: DesignType.tileTitle)
                        if primary {
                            BzrText(bzrString("address.default.badge"), style: DesignType.caption.colored(DesignColors.primary600))
                        }
                        BzrText(state.itemDetails.indices.contains(index) ? state.itemDetails[index] : "", style: DesignType.secondary)
                            .lineLimit(expanded.contains(id) || dynamicTypeSize.isAccessibilitySize ? nil : 2)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .frame(minHeight: DesignSize.touchTargetMin, alignment: .leading)
            }
            .buttonStyle(BzrPressStyle())
            .disabled(state.isBusy)
            .accessibilityElement(children: .combine)
            .accessibilityHint(state.selectionMode ? BzrFormatter.fill(bzrString("address.use.for"), values: [("label", state.items[index])]) : bzrString("address.details"))
            .accessibilityAddTraits(selected ? .isSelected : [])
            if state.isBusy && state.pendingIndex == index {
                ProgressView().tint(DesignColors.primary600)
                    .frame(width: DesignSize.touchTargetMin, height: DesignSize.touchTargetMin)
            } else if !actions.isEmpty {
                Menu {
                    ForEach(actions, id: \.code) { action in
                        Button(action.label, role: action.code == "delete" ? .destructive : nil) {
                            switch action.code {
                            case "edit": onEdit(index)
                            case "set_default": onMakeDefault(index)
                            case "delete": onDelete(index)
                            default: break
                            }
                        }
                    }
                } label: {
                    BzrIcon(.moreHorizontal, tint: DesignColors.primary600)
                        .frame(width: DesignSize.touchTargetMin, height: DesignSize.touchTargetMin)
                }
                .disabled(state.isBusy)
                .accessibilityLabel(BzrFormatter.fill(bzrString("address.actions.for"), values: [("label", state.items[index])]))
            }
        }
        .padding(DesignSpace.m)
        .background(RoundedRectangle(cornerRadius: DesignRadius.card).fill(primary ? DesignColors.primaryWellSelected : DesignColors.surface))
        .overlay(RoundedRectangle(cornerRadius: DesignRadius.card).stroke(primary ? DesignColors.primary600 : DesignColors.border, lineWidth: DesignSize.controlStroke))
    }
}
