import BzrCore
import DesignSystem
import SwiftUI

public struct C09TrackingView: View {
    let state: CustomerUIState
    let onAction: (String) -> Void

    public init(state: CustomerUIState, onAction: @escaping (String) -> Void = { _ in }) { self.state = state; self.onAction = onAction }

    public var body: some View {
        CustomerScreen(title: bzrString("tracking.title"), state: state) {
            if state.phase == .offline { OfflineBanner(text: bzrString("offline.message")) }
            Badge(text: state.displayStatus.isEmpty ? bzrString("order.status") : state.displayStatus, kind: .status)
            if state.showStatusCard { InfoBanner(text: bzrString("tracking.status.cancelled")) }
            if state.showEta {
                EtaCard(value: "25", unit: bzrString("unit.minute"), title: bzrString("eta.title"), subtitle: bzrString(state.etaApproximate ? "eta.approximate" : "eta.subtitle"))
            }
            if state.showMap { MapCard(title: bzrString("map.title")) }
            if state.showWaiting { InfoBanner(text: bzrString("tracking.waiting")) }
            if state.showStepper {
                let labels = ["status.confirmed", "status.on_the_way", "status.arrived", "status.in_progress", "status.payment", "status.closed"]
                let fallback = ["done", "active", "pending", "pending", "pending", "pending"]
                StatusStepper(steps: zip(labels, state.stepStates.isEmpty ? fallback : state.stepStates).map { (bzrString($0.0), stepState($0.1)) })
            }
            if state.messageKey == "status.reviewing" { WarningBox(text: bzrString("status.reviewing")) }
            SummaryCard(
                title: bzrString("summary.title"), rows: [(.orders, bzrString("tracking.order.number")), (.location, bzrString("tracking.area"))],
                editText: bzrString("common.edit"))
            ActionButtons(actions: state.visibleActions, onAction: onAction)
        }
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
