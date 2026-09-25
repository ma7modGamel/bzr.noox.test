package noox.bzr.customer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import noox.bzr.design.AppBottomSheet
import noox.bzr.design.AppTextField
import noox.bzr.design.DangerTextButton
import noox.bzr.design.FieldVisualState
import noox.bzr.design.InfoBanner
import noox.bzr.design.MenuRow
import noox.bzr.design.PrimaryButton
import noox.bzr.design.R
import noox.bzr.design.SecureTextField

@Composable
fun C33AccountSettingsScreen(
    state: CustomerUiState,
    onAction: (String) -> Unit = {},
    onFieldChange: (Int, String) -> Unit = { _, _ -> },
) {
    val mode = state.items.firstOrNull() ?: "overview"
    Box(Modifier.fillMaxSize()) {
        CustomerScreen(stringResource(R.string.account_settings_title), state) {
            state.messageKey?.let { InfoBanner(stringResource(if (it == "account.delete.active_order") R.string.account_delete_active_order else R.string.account_settings_success)) }
            when (mode) {
                "edit" -> {
                    AppTextField(
                        stringResource(R.string.account_settings_name), "", state.fieldValues.getOrElse(0) { "" },
                        accountFieldState(state, "account.validation.name"),
                        state.fieldErrors.firstOrNull { it == "account.validation.name" }?.let { stringResource(R.string.account_validation_name) },
                        onValueChange = { onFieldChange(0, it) },
                    )
                    AppTextField(stringResource(R.string.account_settings_email), "", state.fieldValues.getOrElse(1) { "" }, FieldVisualState.Disabled)
                    AppTextField(stringResource(R.string.account_settings_phone), "", state.fieldValues.getOrElse(2) { "" }, FieldVisualState.Empty, onValueChange = { onFieldChange(2, it) })
                    PrimaryButton(stringResource(R.string.account_settings_save), buttonState(state), onClick = { onAction("update_account") })
                }
                "password" -> {
                    SecureTextField(stringResource(R.string.account_settings_current_password), "", state.fieldValues.getOrElse(0) { "" }, accountFieldState(state, "account.validation.current_password"), onValueChange = { onFieldChange(0, it) })
                    SecureTextField(stringResource(R.string.account_settings_new_password), "", state.fieldValues.getOrElse(1) { "" }, accountFieldState(state, "account.validation.new_password"), onValueChange = { onFieldChange(1, it) })
                    SecureTextField(
                        stringResource(R.string.account_settings_confirm_password), "", state.fieldValues.getOrElse(2) { "" },
                        accountFieldState(state, "account.validation.password_confirmation"),
                        state.fieldErrors.firstOrNull { it == "account.validation.password_confirmation" }
                            ?.let { stringResource(R.string.account_validation_password_confirmation) },
                        onValueChange = { onFieldChange(2, it) },
                    )
                    PrimaryButton(stringResource(R.string.account_settings_password), buttonState(state), onClick = { onAction("change_password") })
                }
                else -> {
                    ScreenHeading(stringResource(R.string.account_settings_profile))
                    MenuRow(R.drawable.ic_account, state.fieldValues.getOrElse(0) { stringResource(R.string.account_settings_name) }, onClick = { onAction("update_account") })
                    MenuRow(R.drawable.ic_info, state.fieldValues.getOrElse(1) { stringResource(R.string.account_settings_email) })
                    MenuRow(R.drawable.ic_phone, state.fieldValues.getOrElse(2) { stringResource(R.string.account_settings_phone) })
                    if ("change_password" in state.visibleActions) MenuRow(R.drawable.ic_shield, stringResource(R.string.account_settings_password), onClick = { onAction("change_password") })
                    MenuRow(R.drawable.ic_info, stringResource(R.string.terms_title), onClick = { onAction("open_terms") })
                    if ("logout" in state.visibleActions) MenuRow(R.drawable.ic_close, stringResource(R.string.customer_account_logout), onClick = { onAction("logout") })
                    if ("delete_account" in state.visibleActions) DangerTextButton(stringResource(R.string.account_delete), onClick = { onAction("delete_account") })
                }
            }
        }
        if (state.showConfirmation) {
            Box(Modifier.align(Alignment.BottomCenter)) {
                AppBottomSheet(
                    stringResource(R.string.account_delete_title), stringResource(R.string.account_delete_body),
                    stringResource(R.string.account_delete_confirm), buttonState(state),
                    stringResource(R.string.common_cancel),
                    onAction = { onAction("confirm_delete_account") }, onSecondary = { onAction("dismiss_delete") },
                )
            }
        }
    }
}

private fun accountFieldState(state: CustomerUiState, key: String): FieldVisualState = when {
    state.isBusy -> FieldVisualState.Disabled
    key in state.fieldErrors -> FieldVisualState.Error
    else -> FieldVisualState.Empty
}
