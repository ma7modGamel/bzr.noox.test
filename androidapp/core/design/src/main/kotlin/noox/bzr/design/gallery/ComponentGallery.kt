package noox.bzr.design.gallery

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import noox.bzr.design.AmountField
import noox.bzr.design.AppBottomSheet
import noox.bzr.design.AppTextField
import noox.bzr.design.AppTopBar
import noox.bzr.design.AvatarSize
import noox.bzr.design.Badge
import noox.bzr.design.BadgeKind
import noox.bzr.design.BottomNav
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.BzrFormat
import noox.bzr.design.BzrText
import noox.bzr.design.BzrTheme
import noox.bzr.design.ChatBubble
import noox.bzr.design.ChatInput
import noox.bzr.design.ChatKind
import noox.bzr.design.CheckRow
import noox.bzr.design.Countdown
import noox.bzr.design.DangerTextButton
import noox.bzr.design.DrawerMenu
import noox.bzr.design.EmptyState
import noox.bzr.design.ErrorState
import noox.bzr.design.EtaCard
import noox.bzr.design.FieldVisualState
import noox.bzr.design.IconSquareButton
import noox.bzr.design.InfoBanner
import noox.bzr.design.LoadingSkeleton
import noox.bzr.design.MapCard
import noox.bzr.design.MediaKind
import noox.bzr.design.MediaState
import noox.bzr.design.MediaThumb
import noox.bzr.design.OfferCard
import noox.bzr.design.OfferVariant
import noox.bzr.design.OfflineBanner
import noox.bzr.design.OrderCard
import noox.bzr.design.PrimaryButton
import noox.bzr.design.ProviderHeader
import noox.bzr.design.R
import noox.bzr.design.RadioCard
import noox.bzr.design.RatingBars
import noox.bzr.design.ReviewCard
import noox.bzr.design.SecondaryButton
import noox.bzr.design.SelectableChip
import noox.bzr.design.SelectableTile
import noox.bzr.design.SelectionState
import noox.bzr.design.SortChips
import noox.bzr.design.StatItem
import noox.bzr.design.StatRow
import noox.bzr.design.StatusStepper
import noox.bzr.design.StepIndicator
import noox.bzr.design.StepState
import noox.bzr.design.StickyActionBar
import noox.bzr.design.SummaryCard
import noox.bzr.design.TextAreaField
import noox.bzr.design.WarningBox
import noox.bzr.design.generated.DesignColors
import noox.bzr.design.generated.DesignSpace
import noox.bzr.design.generated.DesignType
import noox.bzr.design.generated.GalleryFixtures

// Mirror of ComponentGallery.swift: same pages, same order, same keys, same fixture data.

val GalleryPageCount: Int = GalleryFixtures.snapshotPages.size

@Composable
fun ComponentGalleryApp() {
    var page by remember { mutableIntStateOf(0) }

    BzrTheme {
        Column(modifier = Modifier.fillMaxSize().background(DesignColors.surface)) {
            Box(modifier = Modifier.weight(1f)) {
                GalleryPage(page)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(DesignSpace.s),
                modifier = Modifier.fillMaxWidth().padding(DesignSpace.screenHorizontal),
            ) {
                SecondaryButton(stringResource(R.string.gallery_back), enabled = page > 0, modifier = Modifier.weight(1f)) { if (page > 0) page-- }
                PrimaryButton(
                    stringResource(R.string.gallery_next),
                    if (page == GalleryPageCount - 1) ButtonVisualState.Disabled else ButtonVisualState.Normal,
                    Modifier.weight(1f),
                ) { if (page < GalleryPageCount - 1) page++ }
            }
        }
    }
}

/** [anchorBottom] shows the end of a page taller than the viewport (the scroll value is clamped to its maximum). */
@Composable
fun GalleryPage(page: Int, anchorBottom: Boolean = false) {
    BzrTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(DesignSpace.m),
            modifier = Modifier
                .fillMaxSize()
                .background(DesignColors.surface)
                .verticalScroll(rememberScrollState(if (anchorBottom) Int.MAX_VALUE else 0))
                .padding(bottom = DesignSpace.sectionGap),
        ) {
            AppTopBar(stringResource(R.string.gallery_title), R.drawable.ic_more, stringResource(R.string.a11y_more))
            Column(
                verticalArrangement = Arrangement.spacedBy(DesignSpace.m),
                modifier = Modifier.padding(horizontal = DesignSpace.screenHorizontal),
            ) {
                when (page) {
                    0 -> ButtonsPage()
                    1 -> NavigationPage()
                    2 -> SelectionPage()
                    3 -> FieldsPage()
                    4 -> CardsPage()
                    5 -> ProviderPage()
                    6 -> TrackingPage()
                    7 -> FeedbackPage()
                    8 -> ChatPage()
                    9 -> MediaAndSheetPage()
                    else -> TextAreaPage()
                }
            }
        }
    }
}

@Composable
private fun ComponentTitle(@StringRes resource: Int) {
    BzrText(stringResource(resource), DesignType.sectionTitle)
}

@Composable
private fun s(@StringRes resource: Int): String = stringResource(resource)

@Composable
private fun ButtonsPage() {
    ComponentTitle(R.string.gallery_component_primary_button)
    PrimaryButton(s(R.string.gallery_state_normal))
    PrimaryButton(s(R.string.gallery_state_pressed), ButtonVisualState.Pressed)
    Row(horizontalArrangement = Arrangement.spacedBy(DesignSpace.s)) {
        PrimaryButton(s(R.string.gallery_state_disabled), ButtonVisualState.Disabled, Modifier.weight(1f))
        PrimaryButton(s(R.string.gallery_state_loading), ButtonVisualState.Loading, Modifier.weight(1f))
    }
    ComponentTitle(R.string.gallery_component_secondary_button)
    SecondaryButton(s(R.string.button_secondary))
    SecondaryButton(s(R.string.gallery_state_disabled), enabled = false)
    ComponentTitle(R.string.gallery_component_danger_text_button)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignSpace.m)) {
        DangerTextButton(s(R.string.button_danger))
        IconSquareButton(s(R.string.button_chat))
    }
}

@Composable
private fun NavigationPage() {
    ComponentTitle(R.string.gallery_component_app_top_bar)
    AppTopBar(s(R.string.summary_title), R.drawable.ic_edit, s(R.string.a11y_edit))
    ComponentTitle(R.string.gallery_component_navigation)
    BottomNav(
        items = listOf(
            R.drawable.ic_home to s(R.string.nav_home),
            R.drawable.ic_orders to s(R.string.nav_orders),
            R.drawable.ic_messages to s(R.string.nav_messages),
            R.drawable.ic_account to s(R.string.nav_account),
        ),
        selectedIndex = 0,
    )
    DrawerMenu(
        name = s(R.string.drawer_name),
        rating = BzrFormat.number(GalleryFixtures.offerRating),
        verifiedLabel = s(R.string.drawer_verified),
        rows = listOf(
            R.drawable.ic_home to s(R.string.nav_home),
            R.drawable.ic_orders to s(R.string.nav_orders),
            R.drawable.ic_messages to s(R.string.nav_messages),
            R.drawable.ic_info to s(R.string.menu_help),
        ),
        action = s(R.string.menu_provider_mode),
    )
}

@Composable
private fun SelectionPage() {
    ComponentTitle(R.string.gallery_component_step_indicator)
    StepIndicator(
        GalleryFixtures.stepCurrent,
        GalleryFixtures.stepTotal,
        BzrFormat.fill(
            s(R.string.format_step),
            "current" to BzrFormat.number(GalleryFixtures.stepCurrent),
            "total" to BzrFormat.number(GalleryFixtures.stepTotal),
        ),
    )
    ComponentTitle(R.string.gallery_component_selectable_tile)
    Row(horizontalArrangement = Arrangement.spacedBy(DesignSpace.cardGap)) {
        SelectableTile(s(R.string.tile_plumbing), R.drawable.ic_home, SelectionState.Selected, Modifier.weight(1f))
        SelectableTile(s(R.string.tile_electricity), R.drawable.ic_warning, SelectionState.Unselected, Modifier.weight(1f))
        SelectableTile(s(R.string.tile_disabled), R.drawable.ic_info, SelectionState.Disabled, Modifier.weight(1f))
    }
    ComponentTitle(R.string.gallery_component_selectable_chip)
    Row(horizontalArrangement = Arrangement.spacedBy(DesignSpace.cardGap)) {
        SelectableChip(s(R.string.chip_today), SelectionState.Selected, Modifier.weight(1f))
        SelectableChip(s(R.string.chip_tomorrow), SelectionState.Unselected, Modifier.weight(1f))
        SelectableChip(s(R.string.chip_unavailable), SelectionState.Disabled, Modifier.weight(1f))
    }
    ComponentTitle(R.string.gallery_component_radio_card)
    RadioCard(s(R.string.radio_now_title), s(R.string.radio_now_body), true)
    RadioCard(s(R.string.radio_scheduled_title), s(R.string.radio_scheduled_body), false)
    ComponentTitle(R.string.gallery_component_check_row)
    CheckRow(s(R.string.check_materials), true)
    CheckRow(s(R.string.check_materials), false)
}

private val FieldStates = listOf(
    FieldVisualState.Empty,
    FieldVisualState.Filled,
    FieldVisualState.Focused,
    FieldVisualState.Error,
    FieldVisualState.Disabled,
)

@Composable
private fun FieldsPage() {
    val states = FieldStates
    val amount = BzrFormat.number(GalleryFixtures.offerPrice)
    ComponentTitle(R.string.gallery_component_fields)
    states.forEach { state ->
        val value = if (state == FieldVisualState.Empty || state == FieldVisualState.Error) "" else s(R.string.field_value)
        AppTextField(s(R.string.field_label), s(R.string.field_placeholder), value, state, if (state == FieldVisualState.Error) s(R.string.field_error) else null)
    }
    states.forEach { state ->
        val value = if (state == FieldVisualState.Empty || state == FieldVisualState.Error) "" else amount
        AmountField(
            s(R.string.field_amount_label), s(R.string.field_amount_placeholder), value, s(R.string.common_currency_egp), state,
            if (state == FieldVisualState.Error) s(R.string.field_error) else null, s(R.string.common_optional),
        )
    }
}

@Composable
private fun TextAreaPage() {
    ComponentTitle(R.string.gallery_component_text_area)
    FieldStates.forEach { state ->
        val value = if (state == FieldVisualState.Empty || state == FieldVisualState.Error) "" else s(R.string.field_value)
        TextAreaField(s(R.string.field_label), s(R.string.field_placeholder), value, state, if (state == FieldVisualState.Error) s(R.string.field_error) else null)
    }
}

@Composable
private fun CardsPage() {
    val rating = BzrFormat.number(GalleryFixtures.offerRating)
    val services = BzrFormat.fill(s(R.string.format_services), GalleryFixtures.offerServices)
    val price = BzrFormat.fill(s(R.string.format_amount), GalleryFixtures.offerPrice)
    val eta = BzrFormat.fill(s(R.string.format_eta), GalleryFixtures.offerEtaMinutes)
    ComponentTitle(R.string.gallery_component_summary_card)
    SummaryCard(
        s(R.string.summary_title),
        listOf(R.drawable.ic_orders to s(R.string.summary_service), R.drawable.ic_clock to s(R.string.summary_visit)),
        s(R.string.common_edit),
    )
    ComponentTitle(R.string.gallery_component_offer_card)
    OfferCard(s(R.string.offer_provider), rating, services, price, eta, s(R.string.badge_top_rated), s(R.string.offer_select), s(R.string.button_chat), OfferVariant.Execution)
    OfferCard(
        s(R.string.offer_provider), rating, services, price, eta, s(R.string.badge_top_rated), s(R.string.offer_select), s(R.string.button_chat),
        OfferVariant.Inspection, s(R.string.offer_inspection_fee),
    )
    OfferCard(s(R.string.offer_provider), rating, services, price, s(R.string.offer_scheduled), s(R.string.badge_top_rated), s(R.string.offer_select), s(R.string.button_chat), OfferVariant.Scheduled)
}

@Composable
private fun ProviderPage() {
    val rating = BzrFormat.number(GalleryFixtures.offerRating)
    val services = BzrFormat.fill(s(R.string.format_services), GalleryFixtures.offerServices)
    ComponentTitle(R.string.gallery_component_sort_chips)
    SortChips(s(R.string.sort_title), listOf(s(R.string.sort_top_rated), s(R.string.sort_lowest_price), s(R.string.sort_fastest)), 0)
    Row(horizontalArrangement = Arrangement.spacedBy(DesignSpace.s)) {
        Badge(s(R.string.badge_top_rated), BadgeKind.Highlight, R.drawable.ic_star_filled)
        Badge(s(R.string.badge_best_price), BadgeKind.Highlight)
        Badge(s(R.string.badge_fastest), BadgeKind.Highlight)
        Badge(s(R.string.order_status), BadgeKind.Status)
    }
    ComponentTitle(R.string.gallery_component_provider_header)
    ProviderHeader(s(R.string.offer_provider), rating, services, AvatarSize.Small)
    ProviderHeader(s(R.string.offer_provider), rating, services, AvatarSize.Medium, s(R.string.provider_verified))
    ProviderHeader(s(R.string.offer_provider), rating, services, AvatarSize.Large, s(R.string.provider_verified))
    ComponentTitle(R.string.gallery_component_stats)
    StatRow(
        listOf(
            StatItem(R.drawable.ic_star, rating, s(R.string.stat_rating)),
            StatItem(R.drawable.ic_orders, BzrFormat.number(GalleryFixtures.offerServices), s(R.string.stat_services)),
            StatItem(R.drawable.ic_clock, BzrFormat.number(GalleryFixtures.providerYears), s(R.string.stat_years)),
        ),
    )
    RatingBars(
        listOf(s(R.string.rating_quality), s(R.string.rating_commitment), s(R.string.rating_communication)).zip(GalleryFixtures.ratingBars),
        GalleryFixtures.ratingMaxStars,
    )
    ReviewCard(s(R.string.review_name), s(R.string.review_body), s(R.string.review_verified), s(R.string.review_time), s(R.string.review_tag), GalleryFixtures.ratingMaxStars)
    ComponentTitle(R.string.gallery_component_sticky_action_bar)
    StickyActionBar(s(R.string.sticky_price_label), BzrFormat.fill(s(R.string.format_amount), GalleryFixtures.offerPrice), s(R.string.offer_select), s(R.string.button_chat))
}

@Composable
private fun TrackingPage() {
    ComponentTitle(R.string.gallery_component_tracking)
    EtaCard(BzrFormat.number(GalleryFixtures.etaMinutes), s(R.string.unit_minute), s(R.string.eta_title), s(R.string.eta_subtitle))
    MapCard(s(R.string.map_title))
    StatusStepper(
        listOf(
            s(R.string.status_confirmed) to StepState.Done,
            s(R.string.status_on_the_way) to StepState.Done,
            s(R.string.status_arrived) to StepState.OnHold,
            s(R.string.status_in_progress) to StepState.Pending,
            s(R.string.status_payment) to StepState.Pending,
            s(R.string.status_closed) to StepState.Pending,
        ),
    )
    WarningBox(s(R.string.status_reviewing))
    ComponentTitle(R.string.gallery_component_order_card)
    OrderCard(s(R.string.order_title), s(R.string.order_subtitle), s(R.string.order_status))
}

@Composable
private fun FeedbackPage() {
    ComponentTitle(R.string.gallery_component_banners)
    InfoBanner(s(R.string.banner_info))
    WarningBox(s(R.string.banner_warning))
    OfflineBanner(s(R.string.offline_message))
    ComponentTitle(R.string.gallery_component_countdown)
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { Countdown(GalleryFixtures.countdownNormalSeconds, s(R.string.countdown_label)) }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { Countdown(GalleryFixtures.countdownUrgentSeconds, s(R.string.countdown_label)) }
    }
    ComponentTitle(R.string.gallery_component_feedback)
    EmptyState(s(R.string.empty_title), s(R.string.empty_body), s(R.string.action_republish))
    LoadingSkeleton()
    ErrorState(s(R.string.error_title), s(R.string.error_body), s(R.string.common_retry))
}

@Composable
private fun ChatPage() {
    ComponentTitle(R.string.gallery_component_chat)
    ChatBubble(s(R.string.chat_sent), sent = true)
    ChatBubble(s(R.string.chat_received), sent = false)
    ChatBubble(s(R.string.media_photo), sent = false, kind = ChatKind.Image)
    ChatBubble(s(R.string.chat_blocked), sent = false, kind = ChatKind.Blocked)
    ChatInput(s(R.string.chat_placeholder))
}

@Composable
private fun MediaAndSheetPage() {
    ComponentTitle(R.string.gallery_component_media)
    MediaThumb(s(R.string.media_photo), s(R.string.media_uploading), MediaKind.Photo, MediaState.Uploading)
    MediaThumb(s(R.string.media_video), s(R.string.media_uploaded), MediaKind.Video, MediaState.Uploaded)
    MediaThumb(s(R.string.media_audio), s(R.string.media_failed), MediaKind.Audio, MediaState.Failed)
    ComponentTitle(R.string.gallery_component_bottom_sheet)
    Box(modifier = Modifier.fillMaxWidth().background(DesignColors.scrim).padding(top = DesignSpace.xxl)) {
        AppBottomSheet(s(R.string.sheet_title), s(R.string.sheet_body), s(R.string.button_primary))
    }
}
