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
    var onAction: (String) -> Unit = {}
    var onFieldChange: (Int, String) -> Unit = { _, _ -> }

    init {
        binding.editName.onValueChange = { onFieldChange(0, it) }
        binding.editPhone.onValueChange = { onFieldChange(2, it) }
        binding.save.onClick = { onAction("update_account") }
        listOf(binding.currentPassword, binding.newPassword, binding.confirmPassword).forEachIndexed { index, field ->
            field.onValueChange = { onFieldChange(index, it) }
        }
        binding.changePassword.onClick = { onAction("change_password") }
        binding.name.onClick = { onAction("update_account") }
        binding.passwordRow.onClick = { onAction("change_password") }
        binding.terms.onClick = { onAction("open_terms") }
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
        state.messageKey?.let {
            binding.message.text = string(if (it == "account.delete.active_order") R.string.account_delete_active_order else R.string.account_settings_success)
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
                binding.editPhone.value = value(2)
                binding.save.state = customerButtonState(state)
            }
            "password" -> {
                binding.currentPassword.value = value(0)
                binding.currentPassword.state = fieldState(state, "account.validation.current_password")
                binding.newPassword.value = value(1)
                binding.newPassword.state = fieldState(state, "account.validation.new_password")
                binding.confirmPassword.value = value(2)
                binding.confirmPassword.state = fieldState(state, "account.validation.password_confirmation")
                binding.confirmPassword.error =
                    if ("account.validation.password_confirmation" in state.fieldErrors) string(R.string.account_validation_password_confirmation) else null
                binding.changePassword.state = customerButtonState(state)
            }
            else -> {
                binding.name.text = state.fieldValues.getOrElse(0) { string(R.string.account_settings_name) }
                binding.email.text = state.fieldValues.getOrElse(1) { string(R.string.account_settings_email) }
                binding.phone.text = state.fieldValues.getOrElse(2) { string(R.string.account_settings_phone) }
                binding.passwordRow.isVisible = "change_password" in state.visibleActions
                binding.logout.isVisible = "logout" in state.visibleActions
                binding.delete.isVisible = "delete_account" in state.visibleActions
            }
        }
    }

    override fun renderOverlay(state: CustomerUiState) {
        sheet.isVisible = state.showConfirmation
        sheet.actionState = customerButtonState(state)
    }

    private fun fieldState(state: CustomerUiState, key: String): FieldVisualState = when {
        state.isBusy -> FieldVisualState.Disabled
        key in state.fieldErrors -> FieldVisualState.Error
        else -> FieldVisualState.Empty
    }
}
