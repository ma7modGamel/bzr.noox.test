package noox.bzr.customer

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import noox.bzr.design.BzrFormat
import noox.bzr.design.R
import noox.bzr.design.views.TileGridLayout
import noox.bzr.design.views.cardShadow
import noox.bzr.design.views.sizeCompoundIcons
import noox.bzr.gallery.databinding.ItemSectionHeaderBinding
import noox.bzr.gallery.databinding.ScreenC07ProviderBinding

/**
 * SCR-C07 — Figma «Primo App» technician profile 3002:873 (DEC-073): profile card with three real stats, about, specialties,
 * portfolio, verification and reviews, then a dock with the offer price, chat and «اختر …». `fieldValues`: name,
 * rating, completed, experience, about, offer amount, review total, category; `itemStates`: portfolio captions;
 * `items`: name|stars|comment|date.
 */
class C07ProviderView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC07ProviderBinding.inflate(inflater, content)
    var onAction: (String) -> Unit = {}
    private var aboutExpanded = false
    var loadImage: (String, (android.graphics.Bitmap?) -> Unit) -> Unit = { _, _ -> }
    private val avatarPhoto = android.widget.ImageView(context).apply {
        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
        clipToOutline = true
        setBackgroundResource(R.drawable.bremo_bg_avatar_large)
    }


    init {
        (binding.avatar.parent as android.widget.FrameLayout).addView(avatarPhoto, 1, android.widget.FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        pinAction(binding.dock)
        binding.profileCard.cardShadow()
        binding.actions.onAction = { onAction(it) }
        binding.chat.label = string(R.string.button_chat)
        binding.chat.onClick = { onAction("chat") }
        binding.choose.onClick = { onAction("accept_offer") }
        binding.title.sizeCompoundIcons(R.dimen.bremo_size_badge_icon)
        binding.availableNow.sizeCompoundIcons(R.dimen.bremo_size_status_dot)
    }

    override fun title(state: CustomerUiState) = string(R.string.provider_profile_title)

    override fun renderContent(state: CustomerUiState) {
        val values = state.fieldValues
        fun value(index: Int) = values.getOrElse(index) { "" }
        val name = value(0)
        val firstName = name.substringBefore(' ')
        binding.avatar.text = name.trim().take(1)
        avatarPhoto.isVisible = false
        avatarPhoto.contentDescription = name
        bindImage(avatarPhoto, state.providerAvatarUrl)
        binding.name.text = name
        binding.verifiedBadge.isVisible = state.providerVerified
        binding.title.text = BzrFormat.fill(string(R.string.provider_verified_title), "category" to value(7))
        binding.title.isVisible = state.providerVerified && value(7).isNotEmpty()
        val available = state.phase != CustomerPhase.Empty
        binding.availableNow.isVisible = available
        binding.unavailable.isVisible = !available
        renderStats(value(1), value(2), value(3))
        binding.sections.removeAllViews()
        if (value(4).isNotBlank()) about(BzrFormat.fill(string(R.string.provider_about_named), "name" to firstName), value(4))
        if (state.options.isNotEmpty()) specialties(state.options)
        if (state.itemStates.isNotEmpty()) portfolio(state.itemStates, state.portfolioUrls)
        if (state.providerVerified) verification()
        reviews(state.items, value(6), state.imageGroups)
        if (state.reviewTagSummary.isNotEmpty()) {
            val tags = section(string(R.string.provider_review_tags))
            state.reviewTagSummary.forEach { tags.addView(text(it, R.style.TextAppearance_Bremo_Secondary)) }
        }
        binding.price.text = BzrFormat.fill(string(R.string.customer_home_offers_amount), "amount" to value(5))
        binding.priceBlock.isVisible = value(5).isNotEmpty()
        binding.chat.isVisible = "chat" in state.visibleActions
        binding.choose.text = BzrFormat.fill(string(R.string.provider_choose), "name" to firstName)
        binding.choose.isVisible = "accept_offer" in state.visibleActions
        binding.choose.state = continueState(state)
        binding.actions.actions = state.visibleActions.filter { it != "accept_offer" && it != "chat" }
    }

    private fun renderStats(rating: String, completed: String, experience: String) {
        binding.stats.removeAllViews()
        listOf(
            rating to R.string.provider_stat_rating,
            completed to R.string.provider_stat_completed,
            BzrFormat.quantity(context, R.plurals.plural_provider_stat_experience_value, (experience).toIntOrNull() ?: 0).takeIf { experience.isNotEmpty() }.orEmpty() to R.string.provider_stat_experience,
        ).forEachIndexed { index, (stat, label) ->
            val column = noox.bzr.design.views.BremoLinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                addView(text(stat, R.style.TextAppearance_Bremo_StatValue).apply {
                    if (index == 0) {
                        setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_star_filled, 0, 0, 0)
                        TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(context.getColor(R.color.bremo_star)))
                        compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.bremo_space_xs)
                        sizeCompoundIcons(R.dimen.bremo_size_icon_small)
                    }
                })
                addView(text(string(label), R.style.TextAppearance_Bremo_Hint))
            }
            binding.stats.addView(column, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    private fun about(title: String, body: String) {
        val section = section(title)
        val text = text(body, R.style.TextAppearance_Bremo_Secondary).apply {
            maxLines = if (aboutExpanded) Int.MAX_VALUE else ABOUT_LINES
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val more = text(string(R.string.provider_read_more), R.style.TextAppearance_Bremo_LinkSmall).apply {
            isVisible = !aboutExpanded
            setOnClickListener {
                aboutExpanded = true
                text.maxLines = Int.MAX_VALUE
                isVisible = false
            }
        }
        section.addView(card(text, more))
    }

    private fun specialties(labels: List<String>) {
        val grid = TileGridLayout(context).apply { maxColumns = 2 }
        labels.forEach { label ->
            grid.addView(card(noox.bzr.design.views.BremoLinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(iconWell(R.drawable.ic_wrench, R.drawable.bremo_bg_order_well))
                addView(text(label, R.style.TextAppearance_Bremo_TileLabel).apply {
                    setPaddingRelative(resources.getDimensionPixelSize(R.dimen.bremo_space_s), 0, 0, 0)
                }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            }))
        }
        section(string(R.string.provider_specialties)).addView(grid)
    }

    private fun portfolio(captions: List<String>, urls: List<String>) {
        val row = noox.bzr.design.views.BremoLinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        captions.take(PORTFOLIO_TILES).forEachIndexed { index, caption ->
            val photo = android.widget.ImageView(context).apply {
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                setBackgroundResource(R.drawable.bremo_bg_stat_strip)
                setImageResource(R.drawable.ic_image)
                contentDescription = caption.ifBlank { string(R.string.provider_portfolio) }
            }
            bindImage(photo, urls.getOrElse(index) { "" })
            val tile = noox.bzr.design.views.BremoLinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                addView(photo, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, resources.getDimensionPixelSize(R.dimen.bremo_size_portfolio_tile)))
                addView(text(caption, R.style.TextAppearance_Bremo_Hint).apply { gravity = Gravity.CENTER; maxLines = 2 })
            }
            row.addView(tile, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                if (index > 0) marginStart = resources.getDimensionPixelSize(R.dimen.bremo_space_s)
            })
        }
        section(string(R.string.provider_portfolio)).addView(row)
    }

    private fun bindImage(view: android.widget.ImageView, url: String) {
        view.tag = url
        if (url.isBlank()) return
        loadImage(url) { bitmap ->
            if (view.tag == url && bitmap != null) {
                view.setImageBitmap(bitmap)
                view.isVisible = true
            }
        }
    }

    private fun verification() {
        val row = noox.bzr.design.views.BremoLinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(R.drawable.ic_shield to R.string.provider_verification_identity).forEach { (icon, label) ->
            row.addView(noox.bzr.design.views.BremoLinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                addView(iconWell(icon, R.drawable.bremo_bg_round_tint))
                addView(text(string(label), R.style.TextAppearance_Bremo_TileLabel).apply { gravity = Gravity.CENTER })
            }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        }
        section(string(R.string.provider_verification)).addView(card(row))
    }

    private fun reviews(rows: List<String>, total: String, images: List<List<String>> = emptyList()) {
        if (rows.isEmpty()) return
        val section = section(
            string(R.string.provider_reviews),
            total.takeIf(String::isNotEmpty)?.let { BzrFormat.quantity(context, R.plurals.plural_provider_reviews_count, (it).toIntOrNull() ?: 0) },
        )
        rows.forEachIndexed { index, row ->
            val fields = row.split('|')
            if (fields.size < REVIEW_FIELDS) return@forEachIndexed
            val stars = fields[1].toIntOrNull() ?: 0
            val header = noox.bzr.design.views.BremoLinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(text(fields[0].trim().take(1), R.style.TextAppearance_Bremo_TileTitle).apply {
                    gravity = Gravity.CENTER
                    setTextColor(context.getColor(R.color.bremo_primary600))
                    setBackgroundResource(R.drawable.bremo_bg_avatar_large)
                    val size = resources.getDimensionPixelSize(R.dimen.bremo_size_review_avatar)
                    minWidth = size
                    minHeight = size
                })
                addView(noox.bzr.design.views.BremoLinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPaddingRelative(resources.getDimensionPixelSize(R.dimen.bremo_space_s), 0, 0, 0)
                    addView(text(fields[0], R.style.TextAppearance_Bremo_TileTitle))
                    // Date and service area are supplied by the API.
                    addView(text(fields[3], R.style.TextAppearance_Bremo_Hint))
                }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                repeat(MAX_STARS) { star ->
                    addView(iconView(if (star < stars) R.drawable.ic_star_filled else R.drawable.ic_star, R.color.bremo_star, R.dimen.bremo_size_icon_small))
                }
                contentDescription = listOf(fields[0], "$stars/$MAX_STARS").joinToString(string(R.string.format_list_separator))
            }
            section.addView(card(header, text(fields[2], R.style.TextAppearance_Bremo_Secondary).apply { isVisible = fields[2].isNotBlank() }, photoSection(context, "", images.getOrElse(index) { emptyList() }, loadImage)))
        }
    }

    private fun section(title: String, trailing: String? = null): LinearLayout {
        val section = noox.bzr.design.views.BremoLinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            dividerDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bremo_gap_m)
            showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        }
        val header = ItemSectionHeaderBinding.inflate(inflater, section, true)
        header.title.text = title
        header.trailing.text = trailing.orEmpty()
        header.trailing.isVisible = trailing != null
        binding.sections.addView(section)
        return section
    }

    private fun card(vararg children: View) = noox.bzr.design.views.BremoLinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.bremo_bg_card)
        cardShadow()
        dividerDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bremo_gap_s)
        showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        val padding = resources.getDimensionPixelSize(R.dimen.bremo_space_l)
        setPadding(padding, padding, padding, padding)
        children.forEach { addView(it) }
    }

    private fun iconWell(@DrawableRes icon: Int, @DrawableRes background: Int) = android.widget.FrameLayout(context).apply {
        setBackgroundResource(background)
        val size = resources.getDimensionPixelSize(R.dimen.bremo_size_order_well)
        layoutParams = LinearLayout.LayoutParams(size, size)
        addView(iconView(icon, R.color.bremo_primary600), android.widget.FrameLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.bremo_size_icon_small), resources.getDimensionPixelSize(R.dimen.bremo_size_icon_small), Gravity.CENTER,
        ))
    }

    private fun iconView(@DrawableRes icon: Int, tint: Int, size: Int = R.dimen.bremo_size_icon) = ImageView(context).apply {
        setImageResource(icon)
        ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(context.getColor(tint)))
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        val px = resources.getDimensionPixelSize(size)
        layoutParams = LinearLayout.LayoutParams(px, px)
    }

    private fun text(value: String, @StyleRes style: Int) = TextView(context).apply {
        text = value
        TextViewCompat.setTextAppearance(this, style)
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    }

    private companion object {
        const val REVIEW_FIELDS = 4
        const val ABOUT_LINES = 4
        const val PORTFOLIO_TILES = 3
        const val MAX_STARS = 5
    }
}
