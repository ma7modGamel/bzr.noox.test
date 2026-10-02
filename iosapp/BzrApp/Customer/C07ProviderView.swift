import BzrCore
import DesignSystem
import SwiftUI

public struct C07ProviderView: View {
    let state: CustomerUIState
    let onAction: (String) -> Void

    public init(state: CustomerUIState, onAction: @escaping (String) -> Void = { _ in }) {
        self.state = state
        self.onAction = onAction
    }

    public var body: some View {
        CustomerScreen(title: bzrString("provider.profile.title"), state: state) {
            if state.phase == .empty {
                EmptyState(
                    title: bzrString("provider.unavailable.title"),
                    body: bzrString("provider.unavailable.body"), action: bzrString("common.back_to_offers"))
            } else {
                ProviderHeader(
                    name: field(0), rating: field(1), services: field(2), size: .large,
                    verifiedLabel: state.providerVerified ? bzrString("provider.verified") : nil)
                StatRow(items: [
                    StatItem(icon: .star, value: field(1), label: bzrString("stat.rating")),
                    StatItem(icon: .orders, value: field(2), label: bzrString("stat.services")),
                    StatItem(icon: .calendar, value: field(3), label: bzrString("stat.years")),
                ])
                ScreenHeading(bzrString("provider.about.title"), detail: field(4))
                ForEach(Array(state.options.enumerated()), id: \.offset) { _, specialty in
                    SelectableChip(text: specialty, state: .disabled)
                }
                RatingBars(
                    items: zip(
                        [
                            bzrString("rating.quality"), bzrString("rating.commitment"),
                            bzrString("rating.communication"),
                        ], state.itemDetails
                    ).compactMap { label, value in Double(value).map { (label, $0) } }, max: 5)
                ScreenHeading(bzrString("provider.reviews.title"))
                ForEach(Array(state.items.enumerated()), id: \.offset) { _, row in
                    let fields = row.split(separator: "|", omittingEmptySubsequences: false).map(String.init)
                    if fields.count >= 4 {
                        ReviewCard(
                            name: fields[0], body: fields[2], verified: "", time: fields[3],
                            tag: nil, rating: Int(fields[1]) ?? 0)
                    }
                }
            }
            ActionButtons(actions: state.visibleActions, onAction: onAction)
        }
    }

    private func field(_ index: Int) -> String {
        state.fieldValues.indices.contains(index) ? state.fieldValues[index] : ""
    }
}
