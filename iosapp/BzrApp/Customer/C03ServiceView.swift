import BzrCore
import DesignSystem
import SwiftUI

/// SCR-C03, step 1 of 5 (DEC-071): search and pick the service.
public struct C03ServiceView: View {
    let state: CustomerUIState
    let onCategorySelect: (Int) -> Void
    let onSearch: (String) -> Void
    let onOpen: (String) -> Void

    public init(
        state: CustomerUIState, onCategorySelect: @escaping (Int) -> Void = { _ in },
        onSearch: @escaping (String) -> Void = { _ in }, onOpen: @escaping (String) -> Void = { _ in }
    ) {
        self.state = state
        self.onCategorySelect = onCategorySelect
        self.onSearch = onSearch
        self.onOpen = onOpen
    }

    public var body: some View {
        CustomerScreen(title: bzrString("request.service.title"), state: state, bottomBar: AnyView(dock)) {
            RequestProgress(current: 1, total: state.requestSteps)
            SearchField(
                placeholder: bzrString("request.service.search"), value: state.fieldValues.first ?? "",
                onValueChange: onSearch)
            if state.messageKey == "request.search.no_results" {
                InfoBanner(text: bzrString("request.search.no_results"))
            }
            if !state.options.isEmpty {
                ScreenHeading(bzrString("request.service.section"))
            }
            BzrServiceGrid(columns: DesignSize.serviceTileColumns) {
                ForEach(Array(state.options.enumerated()), id: \.offset) { index, label in
                    Button(
                        action: { onCategorySelect(index) },
                        label: {
                            SelectableTile(
                                text: label, icon: .wrench,
                                state: index == state.selectedIndex ? .selected : .unselected,
                                categoryIcon: state.optionIcons.indices.contains(index) ? state.optionIcons[index] : nil,
                                detail: state.itemDetails.indices.contains(index)
                                    ? state.itemDetails[index].components(separatedBy: "\n")
                                        .joined(separator: bzrString("format.list_separator")) : "")
                        }
                    )
                    .buttonStyle(BzrPressStyle())
                }
            }
        }
    }

    private var dock: some View {
        VStack(spacing: DesignSpace.m) {
            if state.options.indices.contains(state.selectedIndex) {
                HStack {
                    BzrText(bzrString("request.service.selected"), style: DesignType.caption)
                    Spacer(minLength: DesignSpace.s)
                    BzrText(state.options[state.selectedIndex], style: DesignType.tileTitle.colored(DesignColors.primary600))
                }
            }
            PrimaryButton(
                text: bzrString("common.next"), state: state.canContinue ? .normal : .disabled, icon: .arrowForward,
                onClick: { onOpen("SCR-C37") })
        }
    }
}
