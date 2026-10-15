import BzrCore
import DesignSystem
import PhotosUI
import SwiftUI

public struct C27DisputeView: View {
    let state: CustomerUIState
    let onReasonSelect: (Int) -> Void
    let onDescriptionChange: (String) -> Void
    let onPhotoSelected: (CustomerMediaUpload) -> Void
    let onDeletePhoto: (Int) -> Void
    let onSubmit: () -> Void
    let onOpenVisit: () -> Void
    @State private var selectedPhoto: PhotosPickerItem?
    @State private var photoPickerPresented = false

    public init(
        state: CustomerUIState, onReasonSelect: @escaping (Int) -> Void = { _ in },
        onDescriptionChange: @escaping (String) -> Void = { _ in },
        onPhotoSelected: @escaping (CustomerMediaUpload) -> Void = { _ in },
        onDeletePhoto: @escaping (Int) -> Void = { _ in }, onSubmit: @escaping () -> Void = {}, onOpenVisit: @escaping () -> Void = {}
    ) {
        self.state = state
        self.onReasonSelect = onReasonSelect
        self.onDescriptionChange = onDescriptionChange
        self.onPhotoSelected = onPhotoSelected
        self.onDeletePhoto = onDeletePhoto
        self.onSubmit = onSubmit
        self.onOpenVisit = onOpenVisit
    }

    public var body: some View {
        CustomerScreen(title: bzrString(state.isWarranty ? "warranty.title" : "dispute.title"), state: state) {
            if state.isWarranty && state.warrantyVisitID != nil { SecondaryButton(text: bzrString("warranty.follow_visit"), onClick: onOpenVisit) }
            if state.messageKey == "dispute.success" || state.messageKey == "warranty.success" {
                InfoBanner(text: bzrString(state.isWarranty ? "warranty.success" : "dispute.success"))
            } else if !state.items.isEmpty {
                Badge(text: state.itemStates.first ?? "")
                SummaryCard(
                    title: bzrString("dispute.existing.title"),
                    rows: state.items.map { (.info, $0) }, editText: nil)
                if state.itemStates.count > 1, !state.itemStates[1].isEmpty {
                    InfoBanner(text: state.itemStates[1])
                }
            } else {
                ScreenHeading(bzrString(state.isWarranty ? "warranty.title" : "dispute.reason.title"), detail: bzrString(state.isWarranty ? "warranty.body" : "dispute.body"))
                ForEach((state.isWarranty ? [] : Array(state.options.indices)), id: \.self) { index in
                    Button(
                        action: { onReasonSelect(index) },
                        label: {
                            RadioCard(
                                title: state.options[index], body: bzrString("payment.option.body"),
                                selected: state.selectedOptionIndex == index)
                        }
                    ).buttonStyle(.plain).disabled(state.isBusy)
                }
                TextAreaField(
                    label: bzrString(state.isWarranty ? "warranty.description" : "dispute.description.label"),
                    placeholder: bzrString("dispute.description.placeholder"),
                    value: state.fieldValues.first ?? "", state: state.isBusy ? .disabled : .empty,
                    onValueChange: onDescriptionChange)
                SecondaryButton(
                    text: bzrString("dispute.photo.add"), onClick: { photoPickerPresented = true }
                )
                .disabled(state.isWarranty)
                .opacity(state.isWarranty ? 0 : 1)
                .photosPicker(
                    isPresented: $photoPickerPresented, selection: $selectedPhoto, matching: .images
                )
                .onChange(of: selectedPhoto) { _, item in
                    guard let item else { return }
                    Task {
                        guard let data = try? await item.loadTransferable(type: Data.self) else { return }
                        onPhotoSelected(
                            CustomerMediaUpload(fileName: "dispute-image.jpg", mimeType: "image/jpeg", data: data)
                        )
                    }
                }
                if state.hasPhoto {
                    MediaThumb(
                        title: bzrString("dispute.photo.title"), stateLabel: bzrString("media.uploaded"),
                        kind: .photo, state: .uploaded, onDelete: { onDeletePhoto(0) })
                }
                if state.visibleActions.contains("open_dispute") || state.visibleActions.contains("request_warranty") {
                    PrimaryButton(
                        text: bzrString(state.isWarranty ? "warranty.submit" : "dispute.submit"),
                        state: state.isBusy ? .loading : (state.canContinue ? .normal : .disabled),
                        onClick: onSubmit)
                }
            }
        }
    }
}
