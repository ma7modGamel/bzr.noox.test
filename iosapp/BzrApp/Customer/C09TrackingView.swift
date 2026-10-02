import BzrCore
import CoreLocation
import DesignSystem
import SwiftUI

public struct C09TrackingView: View {
    let state: CustomerUIState
    let onAction: (String) -> Void
    let tracking: CustomerTrackingPayload?
    let destinationLatitude: Double?
    let destinationLongitude: Double?
    let onRefreshTracking: () async -> Void

    public init(
        state: CustomerUIState,
        tracking: CustomerTrackingPayload? = nil,
        destinationLatitude: Double? = nil,
        destinationLongitude: Double? = nil,
        onRefreshTracking: @escaping () async -> Void = {},
        onAction: @escaping (String) -> Void = { _ in }
    ) {
        self.state = state
        self.tracking = tracking
        self.destinationLatitude = destinationLatitude
        self.destinationLongitude = destinationLongitude
        self.onRefreshTracking = onRefreshTracking
        self.onAction = onAction
    }

    public var body: some View {
        CustomerScreen(title: bzrString("tracking.title"), state: state) {
            if state.phase == .offline { OfflineBanner(text: bzrString("offline.message")) }
            if !state.displayStatus.isEmpty {
                Badge(text: localizedCustomerDisplayStatus(state.displayStatus), kind: .status)
            }
            if state.showStatusCard {
                InfoBanner(text: bzrString(state.messageKey ?? "tracking.status.cancelled"))
            }
            if state.showEta {
                EtaCard(
                    value: tracking?.etaMinutes.map(String.init) ?? "—",
                    unit: bzrString("unit.minute"), title: bzrString("eta.title"),
                    subtitle: bzrString(
                        (tracking?.etaApproximate ?? state.etaApproximate) ? "eta.approximate" : "eta.subtitle")
                )
            }
            if state.showMap {
                NativeMapView(
                    providerCoordinate: coordinate(
                        tracking?.lastLocation?.latitude, tracking?.lastLocation?.longitude),
                    destinationCoordinate: coordinate(destinationLatitude, destinationLongitude)
                )
                .frame(height: DesignSize.mapCardHeight)
                .clipShape(RoundedRectangle(cornerRadius: DesignRadius.card))
                .accessibilityLabel(bzrString("map.title"))
            }
            if state.showWaiting { InfoBanner(text: bzrString("tracking.waiting")) }
            if state.showStepper {
                let labels = [
                    "status.confirmed", "status.on_the_way", "status.arrived", "status.in_progress",
                    "status.payment", "status.closed",
                ]
                let fallback = ["done", "active", "pending", "pending", "pending", "pending"]
                StatusStepper(
                    steps: zip(labels, state.stepStates.isEmpty ? fallback : state.stepStates).map {
                        (bzrString($0.0), stepState($0.1))
                    })
            }
            if state.messageKey == "status.reviewing" { WarningBox(text: bzrString("status.reviewing")) }
            SummaryCard(
                title: bzrString("summary.title"),
                rows: [
                    (
                        .orders,
                        state.items.indices.contains(0) ? state.items[0] : bzrString("tracking.order.number")
                    ),
                    (
                        .location, state.items.indices.contains(1) ? state.items[1] : bzrString("tracking.area")
                    ),
                ],
                editText: bzrString("common.edit"))
            ActionButtons(actions: state.visibleActions, onAction: onAction)
        }
        .task(id: state.showMap) {
            while state.showMap && !Task.isCancelled {
                await onRefreshTracking()
                try? await Task.sleep(for: .seconds(30))
            }
        }
    }

    private func coordinate(_ latitude: Double?, _ longitude: Double?) -> CLLocationCoordinate2D? {
        guard let latitude, let longitude else { return nil }
        return CLLocationCoordinate2D(latitude: latitude, longitude: longitude)
    }
}

private func stepState(_ value: String) -> StepState {
    switch value {
    case "done": .done
    case "active": .active
    case "on_hold": .onHold
    default: .pending
    }
}

func localizedCustomerDisplayStatus(_ key: String) -> String {
    switch key {
    case "order.status.customer.OPEN",
        "order.status.customer.CONFIRMED",
        "order.status.customer.ON_THE_WAY",
        "order.status.customer.ARRIVED",
        "order.status.customer.AWAITING_QUOTE_APPROVAL",
        "order.status.customer.IN_PROGRESS",
        "order.status.customer.AWAITING_PAYMENT",
        "order.status.customer.AWAITING_TRANSFER_VERIFICATION",
        "order.status.customer.AWAITING_CONFIRMATION",
        "order.status.customer.DISPUTED",
        "order.status.customer.CLOSED",
        "order.status.customer.CANCELLED",
        "order.status.customer.EXPIRED":
        bzrString(key)
    default:
        // A status the app has no key for is shown as the server sent it, never as sample text.
        key
    }
}
