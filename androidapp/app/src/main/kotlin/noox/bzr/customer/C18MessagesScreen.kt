package noox.bzr.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import noox.bzr.design.AvatarSize
import noox.bzr.design.BzrCard
import noox.bzr.design.BzrText
import noox.bzr.design.EmptyState
import noox.bzr.design.ProviderHeader
import noox.bzr.design.R
import noox.bzr.design.generated.DesignSpace
import noox.bzr.design.generated.DesignType

@Composable
fun C18MessagesScreen(state: CustomerUiState, onSelect: (Int) -> Unit = {}) {
    CustomerScreen(stringResource(R.string.messages_title), state) {
        ScreenHeading(stringResource(R.string.messages_heading), stringResource(R.string.messages_body))
        if (state.phase == CustomerPhase.Empty) {
            EmptyState(stringResource(R.string.messages_empty_title), stringResource(R.string.messages_empty_body))
        } else {
            state.items.indices.forEach { index ->
                BzrCard(Modifier.fillMaxWidth().clickable { onSelect(index) }) {
                    ProviderHeader(
                        name = state.items[index], rating = "", services = null, size = AvatarSize.Small,
                        verifiedLabel = state.fieldErrors.getOrNull(index)?.takeIf(String::isNotBlank)
                            ?.let { stringResource(R.string.provider_verified) },
                    )
                    BzrText(state.itemDetails.getOrElse(index) { "" }, DesignType.secondary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DesignSpace.s)) {
                        BzrText(
                            state.options.getOrElse(index) { "" }.ifBlank { stringResource(R.string.messages_no_messages) },
                            DesignType.body, Modifier.weight(1f),
                        )
                        BzrText(state.fieldValues.getOrElse(index) { "" }, DesignType.caption)
                    }
                }
            }
        }
    }
}
