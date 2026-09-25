package noox.bzr.customer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import noox.bzr.design.BzrText
import noox.bzr.design.InfoBanner
import noox.bzr.design.R
import noox.bzr.design.generated.DesignType

@Composable
fun C34TermsScreen(state: CustomerUiState) {
    CustomerScreen(stringResource(R.string.terms_title), state) {
        if (!state.canContinue) {
            InfoBanner(stringResource(R.string.terms_empty))
        } else {
            BzrText("${stringResource(R.string.terms_version)}: ${state.itemDetails.getOrElse(0) { "" }}", DesignType.caption)
            BzrText("${stringResource(R.string.terms_effective)}: ${state.itemDetails.getOrElse(1) { "" }}", DesignType.caption)
            BzrText(state.items.firstOrNull().orEmpty(), DesignType.body)
        }
    }
}
