import BzrCore
import DesignSystem
import SwiftUI

public struct C33AccountSettingsView: View {
    let state: CustomerUIState
    let onAction: (String) -> Void
    let onFieldChange: (Int, String) -> Void
    let onRatingRemindersChange: (Bool) -> Void

    public init(
        state: CustomerUIState, onAction: @escaping (String) -> Void = { _ in },
        onFieldChange: @escaping (Int, String) -> Void = { _, _ in },
        onRatingRemindersChange: @escaping (Bool) -> Void = { _ in }
    ) {
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
            MenuRow(
                icon: .account, text: value(0, fallback: "account.settings.name"),
                onClick: { onAction("update_account") })
            MenuRow(icon: .info, text: value(1, fallback: "account.settings.email"))
            MenuRow(icon: .phone, text: value(2, fallback: "account.settings.phone"))
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
                state: fieldState("account.validation.name"),
                onValueChange: { onFieldChange(0, $0) })
            AppTextField(
                label: bzrString("account.settings.email"), placeholder: "", value: rawValue(1),
                state: .disabled)
            AppTextField(
                label: bzrString("account.settings.phone"), placeholder: "", value: rawValue(2),
                state: .empty, onValueChange: { onFieldChange(2, $0) })
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
    private func fieldState(_ key: String) -> FieldVisualState {
        state.isBusy ? .disabled : (state.fieldErrors.contains(key) ? .error : .empty)
    }
}
