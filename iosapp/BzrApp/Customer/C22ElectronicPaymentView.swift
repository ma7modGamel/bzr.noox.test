import BzrCore
import DesignSystem
import SwiftUI
import PhotosUI
import UIKit

public struct C22ElectronicPaymentView: View {
    let state: CustomerUIState
    let onChannelSelect: (Int) -> Void
    let onCreate: () -> Void
    let onOpenCheckout: () -> Void
    let onCopyCode: () -> Void
    let onReceiptSelected: (CustomerMediaUpload) -> Void
    let onReferenceChange: (String) -> Void
    let onRefresh: () -> Void
    @State private var selectedReceipt: PhotosPickerItem?
    @State private var pickingReceipt = false

    public init(
        state: CustomerUIState,
        onChannelSelect: @escaping (Int) -> Void = { _ in },
        onCreate: @escaping () -> Void = {},
        onOpenCheckout: @escaping () -> Void = {},
        onCopyCode: @escaping () -> Void = {},
        onReceiptSelected: @escaping (CustomerMediaUpload) -> Void = { _ in },
        onReferenceChange: @escaping (String) -> Void = { _ in },
        onRefresh: @escaping () -> Void = {}
    ) {
        self.state = state
        self.onChannelSelect = onChannelSelect
        self.onCreate = onCreate
        self.onOpenCheckout = onOpenCheckout
        self.onCopyCode = onCopyCode
        self.onReceiptSelected = onReceiptSelected
        self.onReferenceChange = onReferenceChange
        self.onRefresh = onRefresh
    }

    public var body: some View {
        CustomerScreen(title: bzrString("payment.electronic.title"), state: state, bottomBar: AnyView(createAction)) {
            ScreenHeading(bzrString("payment.electronic.heading"))
            SummaryCard(
                title: bzrString("payment.amount.total"), rows: [(.check, state.items[safe: 0] ?? "")],
                editText: nil)
            ScreenHeading(bzrString("payment.channel.title"))
            ForEach(state.options.indices, id: \.self) { index in
                channelCard(index, state.options[index])
            }
            if state.messageKey == "payment.creating" { InfoBanner(text: bzrString("payment.creating")) }
            if state.isInstapay {
                SummaryCard(title: bzrString("payment.instapay.address"), rows: state.fieldValues.dropFirst().prefix(3).filter { !$0.isEmpty }.map { (.info, $0) }, editText: nil)
                if paymentStatus.isEmpty || ["FAILED", "EXPIRED"].contains(paymentStatus) {
                    InfoBanner(text: bzrString("payment.instapay.instructions"))
                    TextAreaField(
                        label: bzrString("payment.instapay.reference"), placeholder: "", value: state.fieldValues.first ?? "", state: state.isBusy ? .disabled : .empty,
                        onValueChange: onReferenceChange)
                    SecondaryButton(
                        text: bzrString(state.hasPhoto ? "payment.instapay.receipt_attached" : "payment.instapay.receipt"), enabled: !state.isBusy,
                        onClick: { pickingReceipt = true }
                    )
                    .photosPicker(isPresented: $pickingReceipt, selection: $selectedReceipt, matching: .images)
                    .onChange(of: selectedReceipt) { _, item in
                        guard let item else { return }
                        Task {
                            guard let originalData = try? await item.loadTransferable(type: Data.self),
                                let data = UIImage(data: originalData)?.jpegData(compressionQuality: 0.9)
                            else { return }
                            onReceiptSelected(CustomerMediaUpload(fileName: "transfer-receipt.jpg", mimeType: "image/jpeg", data: data))
                        }
                    }
                }
            }
            if state.messageKey == "media.upload_error" { WarningBox(text: bzrString("media.upload_error"), isError: true) }
            resultContent
            SecondaryButton(text: bzrString("payment.refresh"), enabled: !state.isBusy, onClick: onRefresh)

        }
    }

    @ViewBuilder private var createAction: some View {
        if shouldShowCreateButton {
            PrimaryButton(
                text: bzrString(state.isInstapay ? "payment.instapay.submit" : "payment.create"), state: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled),
                onClick: onCreate)
        }
    }

    private var paymentStatus: String { state.itemStates.first ?? "" }
    private var shouldShowCreateButton: Bool {
        (state.visibleActions.contains("pay_electronic") || state.visibleActions.contains("submit_instapay_transfer"))
            && (paymentStatus.isEmpty || ["FAILED", "EXPIRED"].contains(paymentStatus))
    }

    private func channelCard(_ index: Int, _ title: String) -> some View {
        Button(
            action: { onChannelSelect(index) },
            label: {
                RadioCard(
                    title: title, body: bzrString("payment.option.body"),
                    selected: state.selectedOptionIndex == index)
            }
        )
        .buttonStyle(.plain)
        .disabled((!paymentStatus.isEmpty && !["FAILED", "EXPIRED"].contains(paymentStatus)) || state.isBusy)
    }

    @ViewBuilder private var resultContent: some View {
        switch paymentStatus {
        case "PENDING" where !(state.items[safe: 1] ?? "").isEmpty:
            InfoBanner(text: bzrString("payment.kiosk.ready"))
            SummaryCard(
                title: bzrString("payment.kiosk.reference"), rows: [(.info, state.items[safe: 1] ?? "")],
                editText: nil)
            SecondaryButton(text: bzrString("payment.kiosk.copy"), onClick: onCopyCode)
            Countdown(seconds: state.countdownSeconds, label: bzrString("payment.expires"))
        case "PENDING":
            InfoBanner(text: bzrString("payment.checkout.ready"))
            SecondaryButton(text: bzrString("payment.checkout.open"), onClick: onOpenCheckout)
        case "PENDING_VERIFICATION": InfoBanner(text: bzrString("payment.instapay.pending"))
        case "SUCCEEDED": InfoBanner(text: bzrString("payment.success"))
        case "FAILED":
            let message = state.isInstapay ? bzrString("payment.instapay.retry") : bzrString("payment.failed")
            let reason = state.fieldValues[safe: 4] ?? ""
            WarningBox(text: [reason, message].filter { !$0.isEmpty }.joined(separator: "\n"), isError: true)
        case "EXPIRED": WarningBox(text: bzrString("payment.expired"))
        default: EmptyView()
        }
    }
}

extension Array {
    fileprivate subscript(safe index: Index) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
