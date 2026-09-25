package noox.bzr.design.views.gallery

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.widget.NestedScrollView
import noox.bzr.design.AvatarSize
import noox.bzr.design.BzrFormat
import noox.bzr.design.MediaKind
import noox.bzr.design.MediaState
import noox.bzr.design.OfferVariant
import noox.bzr.design.R
import noox.bzr.design.StatItem
import noox.bzr.design.StepState
import noox.bzr.design.databinding.GalleryPageButtonsBinding
import noox.bzr.design.databinding.GalleryPageCardsBinding
import noox.bzr.design.databinding.GalleryPageChatBinding
import noox.bzr.design.databinding.GalleryPageFeedbackBinding
import noox.bzr.design.databinding.GalleryPageFieldsBinding
import noox.bzr.design.databinding.GalleryPageMediaSheetBinding
import noox.bzr.design.databinding.GalleryPageNavigationBinding
import noox.bzr.design.databinding.GalleryPageProviderBinding
import noox.bzr.design.databinding.GalleryPageSelectionBinding
import noox.bzr.design.databinding.GalleryPageTextAreaBinding
import noox.bzr.design.databinding.GalleryPageTrackingBinding
import noox.bzr.design.databinding.ViewGalleryPageBinding
import noox.bzr.design.generated.GalleryFixtures
import noox.bzr.design.views.OfferCardView

// XML mirror of ComponentGallery.swift: same pages, same order, same keys, same fixture data (43 §13).

/** Scroll container that can open anchored at its end (the "-bottom" snapshot cases). */
class GalleryScrollView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : NestedScrollView(context, attrs) {
    var anchorBottom: Boolean = false

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (anchorBottom && childCount > 0) scrollTo(0, maxOf(0, getChildAt(0).height - height))
    }
}

object ComponentGalleryViews {
    val pageCount: Int = GalleryFixtures.snapshotPages.size

    fun page(context: Context, page: Int, anchorBottom: Boolean = false): View {
        val inflater = LayoutInflater.from(context)
        val root = ViewGalleryPageBinding.inflate(inflater)
        root.scroll.anchorBottom = anchorBottom
        val content = root.content
        val s = { id: Int -> context.getString(id) }
        val rating = BzrFormat.number(GalleryFixtures.offerRating)
        val services = BzrFormat.fill(s(R.string.format_services), GalleryFixtures.offerServices)
        when (page) {
            0 -> GalleryPageButtonsBinding.inflate(inflater, content)
            1 -> GalleryPageNavigationBinding.inflate(inflater, content).apply {
                val items = listOf(
                    R.drawable.ic_home to s(R.string.nav_home),
                    R.drawable.ic_orders to s(R.string.nav_orders),
                    R.drawable.ic_messages to s(R.string.nav_messages),
                    R.drawable.ic_account to s(R.string.nav_account),
                )
                bottomNav.items = items
                bottomNav.selectedIndex = 0
                drawer.name = s(R.string.drawer_name)
                drawer.rating = rating
                drawer.verifiedLabel = s(R.string.drawer_verified)
                drawer.rows = listOf(
                    R.drawable.ic_home to s(R.string.nav_home),
                    R.drawable.ic_orders to s(R.string.nav_orders),
                    R.drawable.ic_messages to s(R.string.nav_messages),
                    R.drawable.ic_info to s(R.string.menu_help),
                )
                drawer.action = s(R.string.menu_provider_mode)
            }
            2 -> GalleryPageSelectionBinding.inflate(inflater, content).apply {
                stepIndicator.total = GalleryFixtures.stepTotal
                stepIndicator.current = GalleryFixtures.stepCurrent
                stepIndicator.label = BzrFormat.fill(
                    s(R.string.format_step),
                    "current" to BzrFormat.number(GalleryFixtures.stepCurrent),
                    "total" to BzrFormat.number(GalleryFixtures.stepTotal),
                )
            }
            3 -> GalleryPageFieldsBinding.inflate(inflater, content).apply {
                val amount = BzrFormat.number(GalleryFixtures.offerPrice)
                listOf(amountFilled, amountFocused, amountDisabled).forEach { it.value = amount }
            }
            4 -> GalleryPageCardsBinding.inflate(inflater, content).apply {
                summary.rows = listOf(R.drawable.ic_orders to s(R.string.summary_service), R.drawable.ic_clock to s(R.string.summary_visit))
                val price = BzrFormat.fill(s(R.string.format_amount), GalleryFixtures.offerPrice)
                val eta = BzrFormat.fill(s(R.string.format_eta), GalleryFixtures.offerEtaMinutes)
                fun OfferCardView.fill(detail: String, variant: OfferVariant, caption: String? = null) {
                    provider = s(R.string.offer_provider)
                    this.rating = rating
                    this.services = services
                    this.price = price
                    this.detail = detail
                    badge = s(R.string.badge_top_rated)
                    button = s(R.string.offer_select)
                    chatLabel = s(R.string.button_chat)
                    this.variant = variant
                    priceCaption = caption
                }
                offerExecution.fill(eta, OfferVariant.Execution)
                offerInspection.fill(eta, OfferVariant.Inspection, s(R.string.offer_inspection_fee))
                offerScheduled.fill(s(R.string.offer_scheduled), OfferVariant.Scheduled)
            }
            5 -> GalleryPageProviderBinding.inflate(inflater, content).apply {
                sortChips.title = s(R.string.sort_title)
                sortChips.labels = listOf(s(R.string.sort_top_rated), s(R.string.sort_lowest_price), s(R.string.sort_fastest))
                sortChips.selectedIndex = 0
                listOf(providerSmall to AvatarSize.Small, providerMedium to AvatarSize.Medium, providerLarge to AvatarSize.Large).forEach { (header, size) ->
                    header.name = s(R.string.offer_provider)
                    header.rating = rating
                    header.services = services
                    header.size = size
                }
                stats.items = listOf(
                    StatItem(R.drawable.ic_star, rating, s(R.string.stat_rating)),
                    StatItem(R.drawable.ic_orders, BzrFormat.number(GalleryFixtures.offerServices), s(R.string.stat_services)),
                    StatItem(R.drawable.ic_clock, BzrFormat.number(GalleryFixtures.providerYears), s(R.string.stat_years)),
                )
                ratingBars.max = GalleryFixtures.ratingMaxStars
                ratingBars.items = listOf(s(R.string.rating_quality), s(R.string.rating_commitment), s(R.string.rating_communication)).zip(GalleryFixtures.ratingBars)
                review.name = s(R.string.review_name)
                review.body = s(R.string.review_body)
                review.verified = s(R.string.review_verified)
                review.time = s(R.string.review_time)
                review.tag = s(R.string.review_tag)
                review.rating = GalleryFixtures.ratingMaxStars
                sticky.priceLabel = s(R.string.sticky_price_label)
                sticky.price = BzrFormat.fill(s(R.string.format_amount), GalleryFixtures.offerPrice)
                sticky.action = s(R.string.offer_select)
                sticky.chatLabel = s(R.string.button_chat)
            }
            6 -> GalleryPageTrackingBinding.inflate(inflater, content).apply {
                eta.value = BzrFormat.number(GalleryFixtures.etaMinutes)
                stepper.steps = listOf(
                    s(R.string.status_confirmed) to StepState.Done,
                    s(R.string.status_on_the_way) to StepState.Done,
                    s(R.string.status_arrived) to StepState.OnHold,
                    s(R.string.status_in_progress) to StepState.Pending,
                    s(R.string.status_payment) to StepState.Pending,
                    s(R.string.status_closed) to StepState.Pending,
                )
            }
            7 -> GalleryPageFeedbackBinding.inflate(inflater, content).apply {
                countdownNormal.seconds = GalleryFixtures.countdownNormalSeconds
                countdownUrgent.seconds = GalleryFixtures.countdownUrgentSeconds
            }
            8 -> GalleryPageChatBinding.inflate(inflater, content)
            9 -> GalleryPageMediaSheetBinding.inflate(inflater, content).apply {
                mediaPhoto.apply { title = s(R.string.media_photo); stateLabel = s(R.string.media_uploading); kind = MediaKind.Photo; state = MediaState.Uploading }
                mediaVideo.apply { title = s(R.string.media_video); stateLabel = s(R.string.media_uploaded); kind = MediaKind.Video; state = MediaState.Uploaded }
                mediaAudio.apply { title = s(R.string.media_audio); stateLabel = s(R.string.media_failed); kind = MediaKind.Audio; state = MediaState.Failed }
            }
            else -> GalleryPageTextAreaBinding.inflate(inflater, content)
        }
        return root.root
    }
}
