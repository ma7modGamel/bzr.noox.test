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
        binding.providerMode.setOnClickListener { onOpen("SCR-P01") }
        binding.providerMode.contentDescription = string(R.string.menu_provider_mode)
        binding.logout.onClick = { onOpen("SCR-C10") }
        binding.logout.danger = true
        // DEC-063: white cards lift off the tinted screen with the quiet card shadow.
        listOf(binding.profileCard, binding.menuGroup, binding.logout.parent as android.view.View).forEach(::liftCard)
    }

    private fun liftCard(view: android.view.View) {
        view.elevation = resources.getDimension(R.dimen.bremo_elevation_card)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            val shadow = androidx.core.content.ContextCompat.getColor(context, R.color.bremo_shadow_card)
            view.outlineSpotShadowColor = shadow
            view.outlineAmbientShadowColor = shadow
        }
    }

    override fun title(state: CustomerUiState) = string(R.string.customer_account_title)

    override fun renderContent(state: CustomerUiState) {
        binding.header.rating = ""
        binding.header.name = state.fieldValues.getOrElse(0) { "" }
        binding.header.services = state.fieldValues.getOrNull(1)?.takeIf(String::isNotBlank)
        binding.header.verifiedLabel = string(R.string.account_verified).takeIf { state.messageKey == "account.verified" }
        binding.unverified.isVisible = state.messageKey == "account.unverified"
    }
}
