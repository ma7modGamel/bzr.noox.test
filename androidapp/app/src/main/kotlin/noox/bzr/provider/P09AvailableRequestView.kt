package noox.bzr.provider

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.customer.RowAdapter
import noox.bzr.customer.rows
import noox.bzr.design.AvatarSize
import noox.bzr.design.MediaKind
import noox.bzr.design.R
import noox.bzr.design.views.MediaThumbView
import noox.bzr.design.views.PrimaryButtonView
import noox.bzr.design.views.SecondaryButtonView
import noox.bzr.gallery.databinding.ScreenP09AvailableRequestBinding

class P09AvailableRequestView(context: Context) : ProviderScreenView(context) {
    private val binding = ScreenP09AvailableRequestBinding.inflate(inflater, content)
    var onAction: (String) -> Unit = {}

    private val mediaAdapter = RowAdapter<String, MediaThumbView>(
        create = { MediaThumbView(it.context) },
        bind = { view, label, _ ->
            val (type, rawIndex) = label.split(':').let { it.first() to it.getOrElse(1) { "1" } }
            val index = rawIndex.toIntOrNull() ?: 1
            view.title = context.getString(
                when (type) {
                    "VIDEO" -> R.string.provider_market_request_media_video
                    "AUDIO" -> R.string.provider_market_request_media_audio
                    else -> R.string.provider_market_request_media_photo
                },
                index,
            )
            view.kind = when (type) {
                "VIDEO" -> MediaKind.Video
                "AUDIO" -> MediaKind.Audio
                else -> MediaKind.Photo
            }
            view.readOnly = true
            view.stateLabel = ""
        },
    )

    init {
        binding.media.rows(mediaAdapter, resources.getDimensionPixelSize(R.dimen.bremo_space_m))
        binding.customer.size = AvatarSize.Medium
        binding.customer.verifiedLabel = null
    }

    override fun title() = string(R.string.provider_market_request_title)

    override fun renderContent(state: ProviderUiState) {
        binding.customer.name = state.customerName
        binding.customer.rating = state.customerRating
        binding.status.isVisible = state.displayStatus.isNotBlank()
        binding.status.text = providerDisplayStatus(state.displayStatus)
        binding.details.title = string(R.string.provider_market_request_details)
        binding.details.rows = state.items.take(3).map { R.drawable.ic_info to it }
        binding.conditions.title = string(R.string.provider_market_request_conditions)
        binding.conditions.rows = state.items.drop(3).map { R.drawable.ic_clock to it }
        binding.conditions.isVisible = state.items.size > 3
        binding.noMedia.isVisible = state.options.isEmpty()
        binding.noMedia.title = string(R.string.provider_market_request_no_media)
        binding.noMedia.body = ""
        binding.media.isVisible = state.options.isNotEmpty()
        mediaAdapter.submitList(state.options)
        binding.ownOffer.isVisible = state.secondaryOptions.isNotEmpty()
        binding.ownOffer.text = if (state.secondaryOptions.size >= 3) {
            listOf(
                context.getString(R.string.provider_offer_price) + ": " + state.secondaryOptions[0] + " " + string(R.string.common_currency_egp),
                context.getString(R.string.provider_offer_net, state.secondaryOptions[1]),
                state.secondaryOptions[2],
            ).joinToString("\n")
        } else {
            state.secondaryOptions.joinToString("\n")
        }
        binding.warning.isVisible = state.messageKey != null
        binding.warning.text = string(
            if (state.messageKey == "provider.offers.withdraw.success") {
                R.string.provider_offers_withdraw_success
            } else {
                R.string.provider_market_request_unavailable
            },
        )
        binding.actions.removeAllViews()
        state.visibleActions.forEach { action ->
            val visualState = buttonState(state)
            val button = if (action == "submit_offer") {
                PrimaryButtonView(context).apply {
                    text = string(R.string.provider_offer_submit)
                    this.state = visualState
                    onClick = { onAction(action) }
                }
            } else {
                SecondaryButtonView(context).apply {
                    text = string(if (action == "withdraw_offer") R.string.action_withdraw_offer else R.string.action_chat)
                    enabled = !state.isBusy
                    onClick = { onAction(action) }
                }
            }
            binding.actions.addView(button)
        }
    }
}
