import BzrCore
import DesignSystem
import SwiftUI

public struct C26OrderHistoryView: View {
    let loadImage: (String) async -> Data?
    let state: CustomerUIState
    let onAction: (String) -> Void

    public init(state: CustomerUIState, loadImage: @escaping (String) async -> Data? = { _ in nil }, onAction: @escaping (String) -> Void = { _ in }) {
        self.loadImage = loadImage
        self.state = state
        self.onAction = onAction
    }

    public var body: some View {
        CustomerScreen(title: bzrString("order.details.title"), state: state) {
            if !state.warrantyCoverage.isEmpty { InfoBanner(text: state.warrantyCoverage) }
            ServicePhotoSection(title: bzrString("repair.photos.before"), urls: state.beforePhotoURLs, loadImage: loadImage)
            ServicePhotoSection(title: bzrString("repair.photos.after"), urls: state.afterPhotoURLs, loadImage: loadImage)
            Badge(text: localizedCustomerDisplayStatus(state.displayStatus))
            SummaryCard(
                title: bzrString("order.details.service"),
                rows: rows(Array(state.items.prefix(5)), labels: serviceLabels), editText: nil)
            if let provider = state.options.first, !provider.isEmpty {
                BzrText(bzrString("order.details.provider"), style: DesignType.sectionTitle)
                ProviderHeader(
                    name: provider, rating: "", services: nil, size: .small,
                    verifiedLabel: bzrString("provider.verified"))
            }
            if state.items.count > 5 {
                SummaryCard(
                    title: bzrString("receipt.title"),
                    rows: rows(Array(state.items.dropFirst(5)), labels: receiptLabels), editText: nil)
            } else if let message = state.messageKey {
                InfoBanner(text: "\(bzrString(message)): \(value(state.options, 1))")
            }
            if !state.itemDetails.isEmpty {
                SummaryCard(
                    title: bzrString("receipt.proposals"),
                    rows: state.itemDetails.map { (.info, $0) }, editText: nil)
            }
            ActionButtons(actions: state.visibleActions, onAction: onAction)
        }
    }

    private var serviceLabels: [String] {
        ["receipt.number", "receipt.category", "receipt.problem", "receipt.area", "receipt.visit"].map(
            bzrString)
    }

    private var receiptLabels: [String] {
        [
            "receipt.labor", "receipt.materials", "receipt.total", "receipt.payment_method",
            "receipt.payment_status",
        ].map(bzrString)
    }

    private func rows(_ values: [String], labels: [String]) -> [(BzrIconKey, String)] {
        values.enumerated().map { index, value in
            (.info, "\(labels.indices.contains(index) ? labels[index] : ""): \(value)")
        }
    }

    private func value(_ values: [String], _ index: Int) -> String {
        values.indices.contains(index) ? values[index] : ""
    }
}
