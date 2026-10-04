import BzrCore
import DesignSystem
import PhotosUI
import SwiftUI

public struct C19ChatView: View {
    let state: CustomerUIState
    let onDraftChange: (String) -> Void
    let onSend: () -> Void
    let onPhotoSelected: (CustomerMediaUpload) -> Void
    let onBack: () -> Void
    @State private var selectedPhoto: PhotosPickerItem?
    @State private var photoPickerPresented = false

    public init(
        state: CustomerUIState,
        onDraftChange: @escaping (String) -> Void = { _ in },
        onSend: @escaping () -> Void = {},
        onPhotoSelected: @escaping (CustomerMediaUpload) -> Void = { _ in },
        onBack: @escaping () -> Void = {}
    ) {
        self.state = state
        self.onDraftChange = onDraftChange
        self.onSend = onSend
        self.onPhotoSelected = onPhotoSelected
        self.onBack = onBack
    }

    public var body: some View {
        CustomerScreen(
            title: providerName.isEmpty ? bzrString("messages.title") : providerName,
            state: state, onBack: onBack, bottomBar: AnyView(composer)
        ) {
            ProviderHeader(
                name: providerName, rating: "", services: nil, size: .small,
                verifiedLabel: state.providerVerified ? bzrString("provider.verified") : nil)
            BzrText(value(state.options, 1), style: DesignType.secondary)
            if state.messageKey == "chat.masking.notice" {
                InfoBanner(text: bzrString("chat.masking.notice"))
            }
            if state.phase == .empty {
                EmptyState(title: bzrString("chat.empty.title"), body: bzrString("chat.empty.body"))
            }
            ForEach(state.items.indices, id: \.self) { index in
                VStack(alignment: .leading, spacing: DesignSpace.xs) {
                    let descriptor = value(state.itemStates, index)
                    ChatBubble(
                        text: state.items[index].isEmpty && descriptor.hasSuffix("_image")
                            ? bzrString("chat.image") : state.items[index],
                        sent: descriptor.hasPrefix("sent_"),
                        kind: descriptor.hasSuffix("_image")
                            ? .image
                            : (descriptor.hasSuffix("_blocked") ? .blocked : .text))
                    BzrText(value(state.itemDetails, index), style: DesignType.caption)
                }
            }

        }
    }

    @ViewBuilder private var composer: some View {
        VStack(spacing: DesignSpace.s) {
            if value(state.options, 2) == "READ_ONLY" {
                InfoBanner(text: bzrString("chat.read_only"))
            } else {
                ChatInput(
                    placeholder: bzrString("chat.placeholder"), value: state.fieldValues.first ?? "",
                    onValueChange: onDraftChange, onSend: onSend)
                SecondaryButton(
                    text: bzrString("chat.add_photo"),
                    onClick: { photoPickerPresented = true }
                )
                .photosPicker(
                    isPresented: $photoPickerPresented, selection: $selectedPhoto, matching: .images
                )
                .onChange(of: selectedPhoto) { _, item in
                    guard let item else { return }
                    Task {
                        guard let data = try? await item.loadTransferable(type: Data.self) else { return }
                        onPhotoSelected(
                            CustomerMediaUpload(fileName: "chat-image.jpg", mimeType: "image/jpeg", data: data))
                    }
                }
            }
        }
    }

    private var providerName: String { state.options.first ?? "" }
    private func value(_ values: [String], _ index: Int) -> String {
        values.indices.contains(index) ? values[index] : ""
    }
}
