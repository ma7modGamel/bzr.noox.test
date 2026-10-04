import BzrCore
import DesignSystem
import SwiftUI

public struct C01HomeView: View {
    let state: CustomerUIState
    let onOpen: (String) -> Void
    let onCategorySelect: (Int) -> Void
    let onOrderSelect: (Int) -> Void

    public init(
        state: CustomerUIState, onOpen: @escaping (String) -> Void = { _ in },
        onCategorySelect: @escaping (Int) -> Void = { _ in },
        onOrderSelect: @escaping (Int) -> Void = { _ in }
    ) {
        self.state = state
        self.onOpen = onOpen
        self.onCategorySelect = onCategorySelect
        self.onOrderSelect = onOrderSelect
    }

    private var greeting: String {
        let name = state.fieldValues.first ?? ""
        return name.isEmpty
            ? bzrString("customer.greeting.plain") : String(format: bzrString("customer.greeting"), name)
    }

    public var body: some View {
        CustomerScreen(title: bzrString("nav.home"), state: state) {
            BzrCard {
                BzrText(greeting, style: DesignType.heroTitle)
                    .accessibilityAddTraits(.isHeader)
                BzrText(bzrString("customer.home.subtitle"), style: DesignType.secondary)
                PrimaryButton(text: bzrString("customer.new_request"), onClick: { onOpen("SCR-C03") })
            }
            ScreenHeading(bzrString("customer.categories.title"))
            BzrServiceGrid {
                ForEach(Array(state.options.enumerated()), id: \.offset) { index, label in
                    Button(
                        action: { onCategorySelect(index) },
                        label: {
                            SelectableTile(
                                text: label, icon: .home,
                                state: index == state.selectedIndex ? .selected : .unselected,
                                categoryIcon: state.optionIcons.indices.contains(index)
                                    ? state.optionIcons[index] : nil)
                        }
                    )
                    .buttonStyle(BzrPressStyle())
                }
            }
            ScreenHeading(bzrString("customer.current_orders.title"))
            if state.phase == .empty {
                EmptyState(title: bzrString("empty.title"), body: bzrString("empty.body"))
            } else {
                ForEach(Array(state.items.enumerated()), id: \.offset) { index, title in
                    OrderCard(
                        title: title,
                        subtitle: state.itemDetails.indices.contains(index) ? state.itemDetails[index] : "",
                        status: state.itemStates.indices.contains(index) ? state.itemStates[index] : "",
                        onClick: { onOrderSelect(index) })
                }
            }
        }
    }
}
