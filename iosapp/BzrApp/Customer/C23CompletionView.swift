import BzrCore
import DesignSystem
import SwiftUI

/// SCR-C23 — Figma «إنهاء الخدمة والدفع» 3002:4134 without the method picker (DEC-076): hero, provider card, invoice, the
/// method already used, «تأكيد انتهاء الخدمة» and «المشكلة ما زالت موجودة؟». `items`: provider, labor, materials, total, method.
public struct C23CompletionView: View {
    let loadImage: (String) async -> Data?
    let state: CustomerUIState
    let onAction: (String) -> Void

    public init(state: CustomerUIState, loadImage: @escaping (String) async -> Data? = { _ in nil }, onAction: @escaping (String) -> Void = { _ in }) {
        self.loadImage = loadImage
        self.state = state
        self.onAction = onAction
    }

    private func value(_ index: Int) -> String { state.fieldValues.indices.contains(index) ? state.fieldValues[index] : "" }
    private func item(_ index: Int) -> String { state.items.indices.contains(index) ? state.items[index] : "" }
    private var name: String { value(0).isEmpty ? item(0) : value(0) }

    public var body: some View {
        CustomerScreen(title: bzrString("completion.title"), state: state, bottomBar: AnyView(dock)) {
            if !state.warrantyCoverage.isEmpty { InfoBanner(text: state.warrantyCoverage) }
            ServicePhotoSection(title: bzrString("repair.photos.before"), urls: state.beforePhotoURLs, loadImage: loadImage)
            ServicePhotoSection(title: bzrString("repair.photos.after"), urls: state.afterPhotoURLs, loadImage: loadImage)
            FinishServiceBlocks.hero(name: name)
            FinishServiceBlocks.providerCard(name: name, verified: state.providerVerified, title: value(1), address: value(2))
            FinishServiceBlocks.invoice(labor: item(1), materials: item(2), total: item(3))
            if !value(3).isEmpty {
                InfoBanner(text: BzrFormatter.fill(bzrString("completion.method"), values: [("method", value(3))]))
            }
        }
    }

    @ViewBuilder private var dock: some View {
        VStack(spacing: DesignSpace.xs) {
            if state.visibleActions.contains("confirm_completion") {
                PrimaryButton(
                    text: BzrFormatter.fill(bzrString("completion.confirm"), values: [("amount", item(3))]),
                    state: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled), icon: .check,
                    onClick: { onAction("confirm_completion") })
            }
            if state.visibleActions.contains("request_warranty") {
                SecondaryButton(text: bzrString("warranty.request"), onClick: { onAction("request_warranty") })
            }
            if state.visibleActions.contains("open_dispute") {
                FinishServiceBlocks.problemLink { onAction("open_dispute") }
            }
        }
    }
}
