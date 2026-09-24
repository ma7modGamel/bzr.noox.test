import BzrCore
import DesignSystem
import SwiftUI

public struct C18MessagesView: View {
    let state: CustomerUIState
    let onSelect: (Int) -> Void

    public init(state: CustomerUIState, onSelect: @escaping (Int) -> Void = { _ in }) {
        self.state = state
        self.onSelect = onSelect
    }

    public var body: some View {
        CustomerScreen(title: bzrString("messages.title"), state: state) {
            ScreenHeading(bzrString("messages.heading"), detail: bzrString("messages.body"))
            if state.phase == .empty {
                EmptyState(title: bzrString("messages.empty.title"), body: bzrString("messages.empty.body"))
            } else {
                ForEach(state.items.indices, id: \.self) { index in
                    Button {
                        onSelect(index)
                    } label: {
                        BzrCard {
                            ProviderHeader(
                                name: state.items[index], rating: "", services: nil, size: .small,
                                verifiedLabel: verified(index) ? bzrString("provider.verified") : nil)
                            BzrText(value(state.itemDetails, index), style: DesignType.secondary)
                            HStack(spacing: DesignSpace.s) {
                                BzrText(
                                    value(state.options, index).isEmpty
                                        ? bzrString("messages.no_messages") : value(state.options, index)
                                )
                                .frame(maxWidth: .infinity, alignment: .leading)
                                BzrText(value(state.fieldValues, index), style: DesignType.caption)
                            }
                        }
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func value(_ values: [String], _ index: Int) -> String {
        values.indices.contains(index) ? values[index] : ""
    }

    private func verified(_ index: Int) -> Bool { value(state.fieldErrors, index) == "verified" }
}
