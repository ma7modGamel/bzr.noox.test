import BzrCore
import DesignSystem
import SwiftUI

public struct C07ProviderView: View {
    let state: CustomerUIState
    let onAction: (String) -> Void

    public init(state: CustomerUIState, onAction: @escaping (String) -> Void = { _ in }) { self.state = state; self.onAction = onAction }

    public var body: some View {
        CustomerScreen(title: bzrString("provider.profile.title"), state: state) {
            if state.phase == .empty {
                EmptyState(title: bzrString("provider.unavailable.title"), body: bzrString("provider.unavailable.body"), action: bzrString("common.back_to_offers"))
            } else {
                ProviderHeader(name: bzrString("offer.provider"), rating: "4.9", services: "86", size: .large, verifiedLabel: bzrString("provider.verified"))
                StatRow(items: [
                    StatItem(icon: .star, value: "4.9", label: bzrString("stat.rating")), StatItem(icon: .orders, value: "86", label: bzrString("stat.services")),
                    StatItem(icon: .calendar, value: "7", label: bzrString("stat.years")),
                ])
                ScreenHeading(bzrString("provider.about.title"), detail: bzrString("provider.about.body"))
                RatingBars(items: [(bzrString("rating.quality"), 4.9), (bzrString("rating.commitment"), 4.8), (bzrString("rating.communication"), 4.9)], max: 5)
                ScreenHeading(bzrString("provider.reviews.title"))
                ReviewCard(
                    name: bzrString("review.name"), body: bzrString("review.body"), verified: bzrString("review.verified"), time: bzrString("review.time"),
                    tag: bzrString("review.tag"), rating: 5)
            }
            ActionButtons(actions: state.visibleActions, onAction: onAction)
        }
    }
}
