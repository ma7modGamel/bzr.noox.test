package noox.bzr.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import noox.bzr.design.AvatarSize
import noox.bzr.design.BzrText
import noox.bzr.design.ChatBubble
import noox.bzr.design.ChatInput
import noox.bzr.design.ChatKind
import noox.bzr.design.EmptyState
import noox.bzr.design.InfoBanner
import noox.bzr.design.ProviderHeader
import noox.bzr.design.R
import noox.bzr.design.SecondaryButton
import noox.bzr.design.generated.DesignSpace
import noox.bzr.design.generated.DesignType

@Composable
fun C19ChatScreen(
    state: CustomerUiState,
    onDraftChange: (String) -> Unit = {},
    onSend: () -> Unit = {},
    onAddPhoto: () -> Unit = {},
) {
    CustomerScreen(state.options.firstOrNull().orEmpty().ifBlank { stringResource(R.string.messages_title) }, state) {
        ProviderHeader(
            state.options.firstOrNull().orEmpty(), "", null, AvatarSize.Small,
            verifiedLabel = stringResource(R.string.provider_verified),
        )
        BzrText(state.options.getOrElse(1) { "" }, DesignType.secondary)
        if (state.messageKey == "chat.masking.notice") InfoBanner(stringResource(R.string.chat_masking_notice))
        if (state.phase == CustomerPhase.Empty) {
            EmptyState(stringResource(R.string.chat_empty_title), stringResource(R.string.chat_empty_body))
        }
        state.items.indices.forEach { index ->
            val descriptor = state.itemStates.getOrElse(index) { "received_text" }
            Column(verticalArrangement = Arrangement.spacedBy(DesignSpace.xs)) {
                ChatBubble(
                    text = state.items[index].ifBlank {
                        if (descriptor.endsWith("_image")) stringResource(R.string.chat_image) else ""
                    }, sent = descriptor.startsWith("sent_"),
                    kind = when {
                        descriptor.endsWith("_image") -> ChatKind.Image
                        descriptor.endsWith("_blocked") -> ChatKind.Blocked
                        else -> ChatKind.Text
                    },
                )
                BzrText(state.itemDetails.getOrElse(index) { "" }, DesignType.caption)
            }
        }
        if (state.options.getOrElse(2) { "" } == "READ_ONLY") {
            InfoBanner(stringResource(R.string.chat_read_only))
        } else {
            ChatInput(
                stringResource(R.string.chat_placeholder), state.fieldValues.firstOrNull().orEmpty(),
                onValueChange = onDraftChange, onSend = onSend,
            )
            SecondaryButton(stringResource(R.string.chat_add_photo), onClick = onAddPhoto)
        }
    }
}
