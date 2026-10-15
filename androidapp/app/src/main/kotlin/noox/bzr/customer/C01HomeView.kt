package noox.bzr.customer

import android.content.Context
import android.content.res.ColorStateList
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import noox.bzr.design.BzrFormat
import noox.bzr.design.R
import noox.bzr.design.generated.CategoryIcons
import noox.bzr.design.views.cardShadow
import noox.bzr.design.views.sizeCompoundIcons
import noox.bzr.gallery.databinding.ItemHomeTileBinding
import noox.bzr.gallery.databinding.ScreenC01HomeBinding

/**
 * SCR-C01 — Figma «Primo App» home 3002:270, component for component (DEC-073): pinned header (avatar, location,
 * notifications), greeting, search, hero banner, four services, the current request and the emergency row. Every
 * figure comes from the API; the hero photo is a design asset. The bottom navigation lives in MainActivity.
 */
class C01HomeView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC01HomeBinding.inflate(inflater, content)
    var onOpen: (String) -> Unit = {}
    var onCategorySelect: (Int) -> Unit = {}
    var onOrderSelect: (Int) -> Unit = {}
    var onUrgent: () -> Unit = {}

    override val showsTopBar = false

    init {
        pinHeader(binding.header)
        binding.hero.clipToOutline = true
        binding.hero.cardShadow()
        binding.currentCard.cardShadow()
        binding.location.setOnClickListener { onOpen("SCR-C14") }
        binding.bell.setOnClickListener { onOpen("SCR-C32") }
        binding.bell.contentDescription = string(R.string.notifications_title)
        binding.search.setOnClickListener { onOpen("SCR-C03") }
        binding.search.contentDescription = string(R.string.customer_home_search)
        binding.newRequest.onClick = { onOpen("SCR-C03") }
        binding.allServices.setOnClickListener { onOpen("SCR-C03") }
        binding.urgent.setOnClickListener { onUrgent() }
        binding.urgentAction.sizeCompoundIcons(R.dimen.bremo_size_icon_small)
        binding.orderAction.onClick = { onOrderSelect(0) }
        binding.currentCard.setOnClickListener { onOrderSelect(0) }
    }

    override fun title(state: CustomerUiState) = string(R.string.nav_home)

    override fun renderContent(state: CustomerUiState) {
        val name = state.fieldValues.firstOrNull().orEmpty()
        binding.greeting.text = if (name.isBlank()) string(R.string.customer_greeting_plain) else context.getString(R.string.customer_greeting, name)
        // The app loads no remote images yet, so the avatar shows the name's initial (REPORT figma-b §ثابت).
        binding.avatar.text = name.trim().take(1)
        binding.avatar.contentDescription = name
        binding.locationLabel.text = state.fieldValues.getOrNull(1).orEmpty().ifBlank { string(R.string.customer_home_location_empty) }
        binding.bellDot.isVisible = state.unreadNotifications > 0
        binding.heroBody.text = string(if (state.showPricing) R.string.customer_home_hero_body_marketplace else R.string.customer_home_hero_body_staff)
        renderServices(state)
        renderCurrent(state)
    }

    private fun renderServices(state: CustomerUiState) {
        binding.serviceTiles.removeAllViews()
        state.options.take(HOME_TILES).forEachIndexed { index, label ->
            val tile = ItemHomeTileBinding.inflate(inflater, binding.serviceTiles, false)
            tile.label.text = label
            tile.root.contentDescription = label
            val icon = state.optionIcons.getOrNull(index).orEmpty()
            tile.icon.setImageResource(if (icon.isEmpty()) R.drawable.ic_wrench else CategoryIcons.drawable(icon))
            ImageViewCompat.setImageTintList(tile.icon, ColorStateList.valueOf(context.getColor(R.color.bremo_primary600)))
            tile.well.cardShadow()
            tile.root.setOnClickListener { onCategorySelect(index) }
            binding.serviceTiles.addView(tile.root)
        }
    }

    /** The first order is the current request; offers show while it is open (DEC-073). */
    private fun renderCurrent(state: CustomerUiState) {
        val title = state.items.firstOrNull()
        binding.current.isVisible = title != null
        if (title == null) return
        binding.orderTitle.text = title
        binding.orderAge.text = age(state.orderAgeMinutes.firstOrNull() ?: -1)
        binding.orderAge.isVisible = binding.orderAge.text.isNotEmpty()
        binding.orderStatus.text = state.itemStates.firstOrNull().orEmpty()
        binding.orderStatus.isVisible = binding.orderStatus.text.isNotEmpty()
        val icon = state.orderIcons.firstOrNull().orEmpty()
        binding.orderIcon.setImageResource(if (icon.isEmpty()) R.drawable.ic_wrench else CategoryIcons.drawable(icon))
        ImageViewCompat.setImageTintList(binding.orderIcon, ColorStateList.valueOf(context.getColor(R.color.bremo_primary600)))
        val open = state.optionCodes.firstOrNull() == "OPEN"
        val offers = state.orderOfferCounts.firstOrNull() ?: 0
        val lowest = state.orderLowestOffers.firstOrNull().orEmpty()
        binding.offersRow.isVisible = open && offers > 0
        binding.offersCount.text = BzrFormat.quantity(context, R.plurals.plural_customer_home_offers_new, offers)
        binding.lowestLabel.isVisible = lowest.isNotEmpty()
        binding.lowestAmount.isVisible = lowest.isNotEmpty()
        binding.lowestAmount.text = BzrFormat.fill(string(R.string.customer_home_offers_amount), "amount" to lowest)
        binding.orderAction.text = string(if (open) R.string.customer_home_offers_action else R.string.customer_home_order_action)
    }

    private fun age(minutes: Int): String = when {
        minutes < 0 -> ""
        minutes < 1 -> string(R.string.customer_home_published_now)
        minutes < MINUTES_PER_HOUR -> BzrFormat.quantity(context, R.plurals.plural_customer_home_published_minutes, minutes)
        minutes < MINUTES_PER_DAY -> BzrFormat.quantity(context, R.plurals.plural_customer_home_published_hours, minutes / MINUTES_PER_HOUR)
        else -> BzrFormat.quantity(context, R.plurals.plural_customer_home_published_days, minutes / MINUTES_PER_DAY)
    }

    private companion object {
        const val HOME_TILES = 4
        const val MINUTES_PER_HOUR = 60
        const val MINUTES_PER_DAY = 1440
    }
}
