package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenC02AccountBinding

/** SCR-C02 (DEC-047): account header and menu. */
class C02AccountView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC02AccountBinding.inflate(inflater, content)
    var onOpen: (String) -> Unit = {}

    init {
        binding.personal.onClick = { onOpen("SCR-C33") }
        binding.addresses.onClick = { onOpen("SCR-C14") }
        binding.notifications.onClick = { onOpen("SCR-C32") }
        binding.help.onClick = { onOpen("SCR-C29") }
        binding.logout.onClick = { onOpen("SCR-C10") }
    }

    override fun title(state: CustomerUiState) = string(R.string.customer_account_title)

    override fun renderContent(state: CustomerUiState) {
        binding.header.verifiedLabel = string(if (state.messageKey == "account.verified") R.string.account_verified else R.string.account_unverified)
        binding.unverified.isVisible = state.messageKey == "account.unverified"
    }
}
