package noox.bzr.customer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import noox.bzr.design.AvatarSize
import noox.bzr.design.InfoBanner
import noox.bzr.design.MenuRow
import noox.bzr.design.ProviderHeader
import noox.bzr.design.R

@Composable
fun C02AccountScreen(state: CustomerUiState, onOpen: (String) -> Unit = {}) {
    CustomerScreen(stringResource(R.string.customer_account_title), state) {
        ProviderHeader(stringResource(R.string.drawer_name), "", stringResource(R.string.customer_account_phone), AvatarSize.Large, stringResource(if (state.messageKey == "account.verified") R.string.account_verified else R.string.account_unverified))
        if (state.messageKey == "account.unverified") InfoBanner(stringResource(R.string.account_unverified))
        MenuRow(R.drawable.ic_account, stringResource(R.string.customer_account_personal), onClick = { onOpen("SCR-C33") })
        MenuRow(R.drawable.ic_location, stringResource(R.string.customer_account_addresses), onClick = { onOpen("SCR-C14") })
        MenuRow(R.drawable.ic_orders, stringResource(R.string.customer_account_payments))
        MenuRow(R.drawable.ic_messages, stringResource(R.string.notifications_title), onClick = { onOpen("SCR-C32") })
        MenuRow(R.drawable.ic_info, stringResource(R.string.customer_account_help), onClick = { onOpen("SCR-C29") })
        MenuRow(R.drawable.ic_close, stringResource(R.string.customer_account_logout), onClick = { onOpen("SCR-C10") })
    }
}
