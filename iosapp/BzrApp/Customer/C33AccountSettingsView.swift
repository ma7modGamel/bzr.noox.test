import BzrCore
import DesignSystem
import SwiftUI
import PhotosUI
import UIKit

public struct C33AccountSettingsView: View {
    let state: CustomerUIState
    let onAction: (String) -> Void
    let onFieldChange: (Int, String) -> Void
    let onPhotoSelected: (CustomerMediaUpload) -> Void
    let loadImage: (String) async -> Data?
    @State private var pickingPhoto = false
    @State private var selectedPhoto: PhotosPickerItem?
    let onRatingRemindersChange: (Bool) -> Void

    public init(
        state: CustomerUIState, onAction: @escaping (String) -> Void = { _ in },
        onFieldChange: @escaping (Int, String) -> Void = { _, _ in },
        onPhotoSelected: @escaping (CustomerMediaUpload) -> Void = { _ in },
        loadImage: @escaping (String) async -> Data? = { _ in nil },
        onRatingRemindersChange: @escaping (Bool) -> Void = { _ in }
    ) {
        self.onPhotoSelected = onPhotoSelected
        self.loadImage = loadImage
        self.state = state
        self.onAction = onAction
        self.onFieldChange = onFieldChange
        self.onRatingRemindersChange = onRatingRemindersChange
    }

    public var body: some View {
        ZStack(alignment: .bottom) {
            CustomerScreen(title: bzrString("account.settings.title"), state: state) {
                if let key = state.messageKey { InfoBanner(text: bzrString(key)) }
                switch mode {
                case "edit": editForm
                case "password": passwordForm
                default: overview
                }
            }
            if state.showConfirmation {
                AppBottomSheet(
                    title: bzrString("account.delete.title"), body: bzrString("account.delete.body"),
                    action: bzrString("account.delete.confirm"), actionState: buttonState,
                    secondaryAction: bzrString("common.cancel"),
                    onAction: { onAction("confirm_delete_account") },
                    onSecondary: { onAction("dismiss_delete") })
            }
        }
    }

    private var overview: some View {
        Group {
            ScreenHeading(bzrString("account.settings.profile"))
            ProfileRemoteImage(url: state.accountAvatarURL, label: bzrString("account.settings.photo"), loadImage: loadImage) {
                BzrIcon(.account, tint: DesignColors.primary600)
            }
            .frame(width: DesignSize.profileAvatar, height: DesignSize.profileAvatar)
            .clipShape(Circle())
            SecondaryButton(text: bzrString("account.settings.photo"), enabled: !state.isBusy, onClick: { pickingPhoto = true })
                .photosPicker(isPresented: $pickingPhoto, selection: $selectedPhoto, matching: .images)
                .onChange(of: selectedPhoto) { _, item in
                    Task {
                        defer { selectedPhoto = nil }
                        guard let data = try? await item?.loadTransferable(type: Data.self),
                            let image = UIImage(data: data), let jpeg = image.jpegData(compressionQuality: 0.85), jpeg.count <= 10 * 1024 * 1024
                        else { return }
                        onPhotoSelected(CustomerMediaUpload(fileName: "profile-photo.jpg", mimeType: "image/jpeg", data: jpeg))
                    }
                }
            MenuRow(
                icon: .account, text: bzrString("account.settings.name") + ": " + rawValue(0),
                onClick: { onAction("update_account") })
            MenuRow(icon: .info, text: bzrString("account.settings.email") + ": " + rawValue(1), onClick: { onAction("update_account") })
            MenuRow(icon: .phone, text: bzrString("account.settings.phone") + ": " + rawValue(2), onClick: { onAction("update_account") })
            if state.visibleActions.contains("change_password") {
                MenuRow(
                    icon: .shield, text: bzrString("account.settings.password"),
                    onClick: { onAction("change_password") })
            }
            // NTF-18 فقط (17، DEC-058).
            Button(
                action: { onRatingRemindersChange(!state.ratingRemindersEnabled) },
                label: {
                    CheckRow(text: bzrString("account.rating_reminders"), checked: state.ratingRemindersEnabled)
                }
            ).buttonStyle(BzrPressStyle())
            BzrText(bzrString("account.rating_reminders.body"), style: DesignType.secondary)
            MenuRow(icon: .info, text: bzrString("terms.title"), onClick: { onAction("open_terms") })
            if state.visibleActions.contains("logout") {
                MenuRow(
                    icon: .close, text: bzrString("customer.account.logout"), onClick: { onAction("logout") })
            }
            if state.visibleActions.contains("delete_account") {
                DangerTextButton(text: bzrString("account.delete"), onClick: { onAction("delete_account") })
            }
        }
    }

    private var editForm: some View {
        Group {
            AppTextField(
                label: bzrString("account.settings.name"), placeholder: "", value: rawValue(0),
                state: fieldState("account.validation.name"), error: fieldError("account.validation.name"),
                onValueChange: { onFieldChange(0, $0) })
            AppTextField(
                label: bzrString("account.settings.email"), placeholder: "", value: rawValue(1),
                state: fieldState("account.validation.email"), error: fieldError("account.validation.email"), onValueChange: { onFieldChange(1, $0) })
            AppTextField(
                label: bzrString("account.settings.phone"), placeholder: "", value: rawValue(2),
                state: fieldState("account.validation.phone"), error: fieldError("account.validation.phone"), onValueChange: { onFieldChange(2, $0) })
            if state.emailChanged {
                InfoBanner(text: bzrString("account.settings.email_hint"))
                SecureTextField(
                    label: bzrString("account.settings.current_password"), placeholder: "", value: rawValue(3), state: fieldState("account.validation.current_password"),
                    error: fieldError("account.validation.current_password"), onValueChange: { onFieldChange(3, $0) })
            }
            PrimaryButton(
                text: bzrString("account.settings.save"), state: buttonState,
                onClick: { onAction("update_account") })
        }
    }

    private var passwordForm: some View {
        Group {
            SecureTextField(
                label: bzrString("account.settings.current_password"), placeholder: "", value: rawValue(0),
                state: fieldState("account.validation.current_password"),
                onValueChange: { onFieldChange(0, $0) })
            SecureTextField(
                label: bzrString("account.settings.new_password"), placeholder: "", value: rawValue(1),
                state: fieldState("account.validation.new_password"),
                onValueChange: { onFieldChange(1, $0) })
            SecureTextField(
                label: bzrString("account.settings.confirm_password"), placeholder: "", value: rawValue(2),
                state: fieldState("account.validation.password_confirmation"),
                onValueChange: { onFieldChange(2, $0) })
            PrimaryButton(
                text: bzrString("account.settings.password"), state: buttonState,
                onClick: { onAction("change_password") })
        }
    }

    private var mode: String { state.items.first ?? "overview" }
    private var buttonState: ButtonVisualState {
        state.isBusy ? .loading : (state.canContinue ? .normal : .disabled)
    }
    private func rawValue(_ index: Int) -> String {
        state.fieldValues.indices.contains(index) ? state.fieldValues[index] : ""
    }
    private func value(_ index: Int, fallback: String) -> String {
        rawValue(index).isEmpty ? bzrString(fallback) : rawValue(index)
    }
    private func fieldError(_ key: String) -> String? { state.fieldErrors.contains(key) ? bzrString(key) : nil }
    private func fieldState(_ key: String) -> FieldVisualState {
        state.isBusy ? .disabled : (state.fieldErrors.contains(key) ? .error : .empty)
    }
}
