package noox.bzr.customer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import noox.bzr.design.AvatarSize
import noox.bzr.design.EtaCard
import noox.bzr.design.ProviderHeader
import noox.bzr.design.R
import noox.bzr.design.SummaryCard
import noox.bzr.design.WarningBox

@Composable
fun C08OfferDetailsScreen(state: CustomerUiState, onAction: (String) -> Unit = {}) {
    CustomerScreen(stringResource(R.string.offer_details_title), state) {
        ProviderHeader(stringResource(R.string.offer_provider), "4.9", "86", AvatarSize.Medium, stringResource(R.string.provider_verified))
        SummaryCard(stringResource(R.string.offer_details_title), listOf(R.drawable.ic_orders to stringResource(R.string.offer_price), R.drawable.ic_clock to if (state.showEta) "25 " + stringResource(R.string.unit_minute) else stringResource(R.string.offer_appointment), R.drawable.ic_shield to stringResource(R.string.offer_inspection_deductible)), stringResource(R.string.common_edit))
        if (state.showEta) EtaCard("25", stringResource(R.string.unit_minute), stringResource(R.string.eta_title), stringResource(R.string.eta_subtitle))
        WarningBox(stringResource(R.string.offer_warning))
        ActionButtons(state.visibleActions, onAction)
    }
}
