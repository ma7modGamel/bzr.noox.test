package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.FieldVisualState
import noox.bzr.design.R
import noox.bzr.design.views.AppBottomSheetView
import noox.bzr.gallery.databinding.ScreenC33AccountSettingsBinding

/** SCR-C33 (DEC-047): profile overview, edit, change password, delete account sheet. */
class C33AccountSettingsView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC33AccountSettingsBinding.inflate(inflater, content)
    private val sheet = AppBottomSheetView(context)
    var loadImage: (String, (android.graphics.Bitmap?) -> Unit) -> Unit = { _, ready -> ready(null) }
    var onPickPhoto: () -> Unit = {}
    var onAction: (String) -> Unit = {}
    var onFieldChange: (Int, String) -> Unit = { _, _ -> }
    var onRatingRemindersChange: (Boolean) -> Unit = {}

    init {
        binding.editName.onValueChange = { onFieldChange(0, it) }
        binding.editEmail.onValueChange = { onFieldChange(1, it) }
        binding.emailPassword.onValueChange = { onFieldChange(3, it) }
        binding.changePhoto.onClick = { onPickPhoto() }
        binding.editPhone.onValueChange = { onFieldChange(2, it) }
        binding.save.onClick = { onAction("update_account") }
        listOf(binding.currentPassword, binding.newPassword, binding.confirmPassword).forEachIndexed { index, field ->
            field.onValueChange = { onFieldChange(index, it) }
        }
        binding.changePassword.onClick = { onAction("change_password") }
        listOf(binding.name, binding.email, binding.phone).forEach { row -> row.onClick = { onAction("update_account") } }
        binding.passwordRow.onClick = { onAction("change_password") }
        binding.terms.onClick = { onAction("open_terms") }
        binding.ratingReminders.setOnClickListener { onRatingRemindersChange(!binding.ratingReminders.checked) }
        binding.logout.onClick = { onAction("logout") }
        binding.delete.onClick = { onAction("delete_account") }
        sheet.title = string(R.string.account_delete_title)
        sheet.body = string(R.string.account_delete_body)
        sheet.action = string(R.string.account_delete_confirm)
        sheet.secondaryAction = string(R.string.common_cancel)
        sheet.onAction = { onAction("confirm_delete_account") }
        sheet.onSecondary = { onAction("dismiss_delete") }
        overlay.addView(sheet)
    }

    override fun title(state: CustomerUiState) = string(R.string.account_settings_title)

    override fun renderContent(state: CustomerUiState) {
        val mode = state.items.firstOrNull() ?: "overview"
        binding.message.isVisible = state.messageKey != null
        binding.message.isError = state.messageKey == "account.settings.failed"
        state.messageKey?.let {
            binding.message.text = string(when (it) { "account.delete.active_order" -> R.string.account_delete_active_order; "account.settings.failed" -> R.string.account_settings_failed; "account.settings.email_saved" -> R.string.account_settings_email_saved; else -> R.string.account_settings_success })
        }
        binding.edit.isVisible = mode == "edit"
        binding.password.isVisible = mode == "password"
        binding.overview.isVisible = mode != "edit" && mode != "password"
        val value = { index: Int -> state.fieldValues.getOrElse(index) { "" } }
        when (mode) {
            "edit" -> {
                binding.editName.value = value(0)
                binding.editName.state = fieldState(state, "account.validation.name")
                binding.editName.error = if ("account.validation.name" in state.fieldErrors) string(R.string.account_validation_name) else null
                binding.editEmail.value = value(1)
                binding.editEmail.state = fieldState(state, "account.validation.email")
                binding.editEmail.error = if ("account.validation.email" in state.fieldErrors) string(R.string.account_validation_email) else null
                binding.editPhone.state = fieldState(state, "account.validation.phone")
                binding.editPhone.error = if ("account.validation.phone" in state.fieldErrors) string(R.string.account_validation_phone) else null
                binding.emailHint.isVisible = state.emailChanged
                binding.emailPassword.isVisible = state.emailChanged
                binding.emailPassword.value = value(3)
                binding.emailPassword.state = fieldState(state, "account.validation.current_password")
                binding.editPhone.value = value(2)
                binding.save.state = customerButtonState(state)
            }
            "password" -> {
                binding.currentPassword.value = value(0)
                binding.currentPassword.state = fieldState(state, "account.validation.current_password")
                binding.currentPassword.error =
                    if ("account.validation.current_password" in state.fieldErrors) string(R.string.account_validation_current_password) else null
                binding.newPassword.value = value(1)
                binding.newPassword.state = fieldState(state, "account.validation.new_password")
                binding.newPassword.error =
                    if ("account.validation.new_password" in state.fieldErrors) string(R.string.account_validation_new_password) else null
                binding.confirmPassword.value = value(2)
                binding.confirmPassword.state = fieldState(state, "account.validation.password_confirmation")
                binding.confirmPassword.error =
                    if ("account.validation.password_confirmation" in state.fieldErrors) string(R.string.account_validation_password_confirmation) else null
                binding.changePassword.state = customerButtonState(state)
            }
            else -> {
                binding.avatar.loadImage = loadImage
                binding.avatar.isVisible = state.accountAvatarUrl.isNotBlank()
                if (state.accountAvatarUrl.isNotBlank()) binding.avatar.bind(state.accountAvatarUrl)
                binding.changePhoto.enabled = !state.isBusy
                binding.name.text = string(R.string.account_settings_name) + ": " + value(0)
                binding.email.text = string(R.string.account_settings_email) + ": " + value(1)
                binding.phone.text = string(R.string.account_settings_phone) + ": " + value(2)
                binding.passwordRow.isVisible = "change_password" in state.visibleActions
                binding.ratingReminders.checked = state.ratingRemindersEnabled
                binding.ratingReminders.contentDescription = string(R.string.account_rating_reminders)
                binding.logout.isVisible = "logout" in state.visibleActions
                binding.delete.isVisible = "delete_account" in state.visibleActions
            }
        }
    }

    override fun renderOverlay(state: CustomerUiState) {
        val logout = state.showLogoutConfirmation
        sheet.title = string(if (logout) R.string.account_logout_confirm_title else R.string.account_delete_title)
        sheet.body = string(if (logout) R.string.account_logout_confirm_body else R.string.account_delete_body)
        sheet.action = string(if (logout) R.string.customer_account_logout else R.string.account_delete_confirm)
        sheet.onAction = { onAction(if (logout) "confirm_logout" else "confirm_delete_account") }
        sheet.onSecondary = { onAction(if (logout) "dismiss_logout" else "dismiss_delete") }
        sheet.isVisible = state.showConfirmation || logout
        sheet.actionState = customerButtonState(state)
    }

    private fun fieldState(state: CustomerUiState, key: String): FieldVisualState = when {
        state.isBusy -> FieldVisualState.Disabled
        key in state.fieldErrors -> FieldVisualState.Error
        else -> FieldVisualState.Empty
    }
}
