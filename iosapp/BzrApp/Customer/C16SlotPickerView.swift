import BzrCore
import DesignSystem
import SwiftUI

public struct C16SlotPickerView: View {
    let state: CustomerUIState
    let onDaySelect: (Int) -> Void
    let onSlotSelect: (Int) -> Void
    let onConfirm: () -> Void

    public init(
        state: CustomerUIState, onDaySelect: @escaping (Int) -> Void = { _ in },
        onSlotSelect: @escaping (Int) -> Void = { _ in }, onConfirm: @escaping () -> Void = {}
    ) {
        self.state = state
        self.onDaySelect = onDaySelect
        self.onSlotSelect = onSlotSelect
        self.onConfirm = onConfirm
    }

    public var body: some View {
        CustomerScreen(title: bzrString("slot.title"), state: state) {
            ScreenHeading(bzrString("slot.day.title"))
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: DesignSpace.s) {
                    ForEach(Array(state.options.enumerated()), id: \.offset) { index, day in
                        Button(
                            action: { onDaySelect(index) },
                            label: {
                                SelectableChip(
                                    text: day, state: state.selectedOptionIndex == index ? .selected : .unselected)
                            }
                        )
                        .buttonStyle(BzrPressStyle())
                    }
                }
            }
            ScreenHeading(bzrString("slot.period.title"))
            if state.items.isEmpty {
                EmptyState(title: bzrString("slot.empty.title"), body: bzrString("slot.empty.body"))
            } else {
                ForEach(Array(stride(from: 0, to: state.items.count, by: 2)), id: \.self) { start in
                    HStack(spacing: DesignSpace.m) {
                        SlotChip(label: state.items[start], index: start, state: state, onSelect: onSlotSelect)
                        if state.items.indices.contains(start + 1) {
                            SlotChip(
                                label: state.items[start + 1], index: start + 1, state: state,
                                onSelect: onSlotSelect)
                        } else {
                            Spacer().frame(maxWidth: .infinity)
                        }
                    }
                }
            }
            PrimaryButton(
                text: bzrString("slot.confirm"), state: state.canContinue ? .normal : .disabled,
                onClick: onConfirm)
        }
    }
}

private struct SlotChip: View {
    let label: String
    let index: Int
    let state: CustomerUIState
    let onSelect: (Int) -> Void

    var body: some View {
        Button(
            action: { onSelect(index) },
            label: {
                SelectableChip(text: label, state: state.selectedIndex == index ? .selected : .unselected)
            }
        )
        .buttonStyle(BzrPressStyle())
    }
}
