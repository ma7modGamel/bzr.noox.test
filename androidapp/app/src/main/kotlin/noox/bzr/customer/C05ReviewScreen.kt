package noox.bzr.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.CheckRow
import noox.bzr.design.InfoBanner
import noox.bzr.design.PrimaryButton
import noox.bzr.design.R
import noox.bzr.design.SummaryCard

@Composable
fun C05ReviewScreen(
    state: CustomerUiState,
    onTermsChange: () -> Unit = {},
    onPublish: () -> Unit = {},
) {
    CustomerScreen(stringResource(R.string.request_review_title), state) {
        SummaryCard(stringResource(R.string.summary_title), listOf(R.drawable.ic_home to stringResource(R.string.request_review_service), R.drawable.ic_edit to stringResource(R.string.request_review_description), R.drawable.ic_location to stringResource(R.string.request_address_value), R.drawable.ic_calendar to stringResource(R.string.request_slot_value)), stringResource(R.string.common_edit))
        if (!state.showPricing) InfoBanner(stringResource(R.string.request_staff_pricing))
        Box(Modifier.clickable(onClick = onTermsChange)) {
            CheckRow(stringResource(R.string.request_review_terms), state.termsError == null)
        }
        state.termsError?.let { InfoBanner(stringResource(R.string.request_terms_required)) }
        PrimaryButton(stringResource(R.string.request_publish), if (state.canContinue) ButtonVisualState.Normal else ButtonVisualState.Disabled, onClick = onPublish)
    }
}
