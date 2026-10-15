package noox.bzr.customer

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import noox.bzr.design.BzrFormat
import noox.bzr.design.R
import noox.bzr.design.views.BremoLinearLayout
import noox.bzr.design.views.cardShadow
import noox.bzr.design.views.sizeCompoundIcons
import noox.bzr.gallery.databinding.ScreenC18MessagesBinding

/**
 * SCR-C18 — Figma «الرسائل» 3002:2924 (DEC-076): the bell in the top bar, a local search, «الكل» and «طلبات نشطة»
 * chips, one card per conversation, the support entry, then «أمانك وحقوقك أولوية». `items`: provider names;
 * `itemDetails`: "category • status"; `itemStates`: conversation status; `options`: last messages; `fieldValues`: times;
 * `fieldErrors`: "verified"; `optionCodes`: "mine" when the last message is the customer's. Unread counts and availability come from the API; opening a chat marks only its fetched incoming messages read.
 */
class C18MessagesView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC18MessagesBinding.inflate(inflater, content)
    var onSelect: (Int) -> Unit = {}
    var onOpen: (String) -> Unit = {}
    private var query = ""
    private var activeOnly = false
    private var unreadOnly = false
    private var last: CustomerUiState? = null

    init {
        scaffold.topBar.actionIcon = R.drawable.ic_bell
        scaffold.topBar.actionLabel = string(R.string.notifications_title)
        scaffold.topBar.onAction = { onOpen("SCR-C32") }
        binding.search.placeholder = string(R.string.messages_search)
        binding.search.onValueChange = { value ->
            query = value.trim()
            last?.let(::renderContent)
        }
    }

    override fun title(state: CustomerUiState) = string(R.string.messages_title)

    override fun renderContent(state: CustomerUiState) {
        last = state
        val empty = state.phase == CustomerPhase.Empty
        val active = state.items.indices.filter { state.itemStates.getOrNull(it) == "OPEN" }
        binding.empty.isVisible = empty
        binding.search.isVisible = !empty
        binding.chips.isVisible = !empty
        binding.chips.removeAllViews()
        binding.chips.addView(chip(string(R.string.messages_filter_all), state.items.size.takeIf { it > 0 }, !activeOnly && !unreadOnly) { activeOnly = false; unreadOnly = false; renderContent(state) })
        binding.chips.addView(chip(string(R.string.messages_filter_active), null, activeOnly) { activeOnly = true; unreadOnly = false; renderContent(state) })
        binding.chips.addView(chip(string(R.string.messages_filter_unread), state.unreadCounts.sum().takeIf { it > 0 }, unreadOnly) { unreadOnly = true; activeOnly = false; renderContent(state) })
        val shown = (if (activeOnly) active else state.items.indices.toList()).filter { index ->
            if (unreadOnly && state.unreadCounts.getOrElse(index) { 0 } == 0) return@filter false
            query.isEmpty() || listOf(state.items, state.itemDetails, state.options).any { it.getOrElse(index) { "" }.contains(query, ignoreCase = true) }
        }
        binding.noResults.isVisible = !empty && shown.isEmpty()
        binding.conversations.removeAllViews()
        if (empty) return
        shown.forEach { index -> binding.conversations.addView(conversation(state, index)) }
        if (query.isEmpty() && !activeOnly && !unreadOnly) binding.conversations.addView(supportRow())
        binding.conversations.addView(safetyNote())
    }

    private fun conversation(state: CustomerUiState, index: Int): View {
        val name = state.items[index]
        val message = state.options.getOrElse(index) { "" }
        val mine = state.optionCodes.getOrNull(index) == "mine"
        return card(
            avatar(name.trim().take(1), online = state.providersAvailable.getOrElse(index) { false }),
            name, state.fieldErrors.getOrNull(index) == "verified", state.unreadCounts.getOrElse(index) { 0 }.takeIf { it > 0 }?.let { BzrFormat.number(it) },
            (state.itemDetails.getOrElse(index) { "" } + if (state.providersAvailable.getOrElse(index) { false }) " · ${string(R.string.provider_available_now)}" else ""), message.ifBlank { string(R.string.messages_no_messages) },
            state.fieldValues.getOrElse(index) { "" }, if (mine && message.isNotBlank()) R.drawable.ic_check else 0,
        ).apply { setOnClickListener { onSelect(index) } }
    }

    /** Figma's official support conversation opens the help screen (C29); it is not an API conversation. */
    private fun supportRow(): View = card(
        FrameLayout(context).apply {
            setBackgroundResource(R.drawable.bremo_bg_avatar_large)
            addView(icon(R.drawable.ic_shield, R.color.bremo_primary600, R.dimen.bremo_size_icon), FrameLayout.LayoutParams(px(R.dimen.bremo_size_icon), px(R.dimen.bremo_size_icon), Gravity.CENTER))
        },
        string(R.string.messages_support_name), false, string(R.string.messages_support_badge),
        string(R.string.messages_support_subtitle), string(R.string.messages_support_body), "", 0,
    ).apply { setOnClickListener { onOpen("SCR-C29") } }

    private fun card(
        avatar: View, name: String, verified: Boolean, badge: String?, meta: String, message: String, time: String, @DrawableRes tick: Int,
    ) = row(Gravity.TOP).apply {
        setBackgroundResource(R.drawable.bremo_bg_card)
        cardShadow()
        setPadding(px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l))
        isClickable = true
        isFocusable = true
        contentDescription = listOf(name, meta, message, time).filter(String::isNotBlank).joinToString(string(R.string.format_list_separator))
        addView(avatar, LinearLayout.LayoutParams(px(R.dimen.bremo_size_offer_avatar), px(R.dimen.bremo_size_offer_avatar)))
        addView(BremoLinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            dividerDrawable = ContextCompat.getDrawable(context, R.drawable.bremo_gap_xs)
            showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
            setPaddingRelative(px(R.dimen.bremo_space_m), 0, 0, 0)
            addView(row().apply {
                addView(text(name, R.style.TextAppearance_Bremo_OfferName).apply {
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    if (verified) {
                        setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_verified, 0)
                        TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(context.getColor(R.color.bremo_primary600)))
                        compoundDrawablePadding = px(R.dimen.bremo_space_xs)
                        sizeCompoundIcons(R.dimen.bremo_size_icon_small)
                    }
                }, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0f))
                if (badge != null) addView(text(badge, R.style.TextAppearance_Bremo_StatusPill).apply {
                    setBackgroundResource(R.drawable.bremo_bg_status_pill)
                    setPaddingRelative(px(R.dimen.bremo_space_s), 0, px(R.dimen.bremo_space_s), 0)
                }, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { marginStart = px(R.dimen.bremo_space_xs) })
                addView(View(context), LinearLayout.LayoutParams(0, 0, 1f))
                if (time.isNotBlank()) addView(text(time, R.style.TextAppearance_Bremo_Hint))
            })
            if (meta.isNotBlank()) addView(text(meta, R.style.TextAppearance_Bremo_Hint).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END })
            addView(row().apply {
                addView(text(message, R.style.TextAppearance_Bremo_Secondary).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                if (tick != 0) addView(icon(tick, R.color.bremo_slate300, R.dimen.bremo_size_icon_small))
            })
        }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
    }

    private fun avatar(initial: String, online: Boolean) = FrameLayout(context).apply {
        addView(text(initial, R.style.TextAppearance_Bremo_TileTitle).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(context.getColor(R.color.bremo_primary600))
            setBackgroundResource(R.drawable.bremo_bg_avatar_large)
        }, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        if (online) addView(View(context).apply { setBackgroundResource(R.drawable.bremo_bg_online_dot) }, FrameLayout.LayoutParams(px(R.dimen.bremo_size_online_dot), px(R.dimen.bremo_size_online_dot), Gravity.BOTTOM or Gravity.START))
    }

    private fun chip(label: String, count: Int?, selected: Boolean, onClick: () -> Unit) = row().apply {
        setBackgroundResource(if (selected) R.drawable.bremo_bg_button_primary else R.drawable.bremo_bg_info_pill)
        minimumHeight = px(R.dimen.bremo_size_chip_height)
        setPaddingRelative(px(R.dimen.bremo_space_m), 0, px(R.dimen.bremo_space_m), 0)
        isClickable = true
        isFocusable = true
        isSelected = selected
        contentDescription = listOfNotNull(label, count?.let(BzrFormat::number)).joinToString(" ")
        setOnClickListener { onClick() }
        addView(text(label, R.style.TextAppearance_Bremo_TileTitle).apply { setTextColor(context.getColor(if (selected) R.color.bremo_on_primary else R.color.bremo_navy900)) })
        if (count != null) addView(text(BzrFormat.number(count), R.style.TextAppearance_Bremo_Hint).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(context.getColor(if (selected) R.color.bremo_navy900 else R.color.bremo_on_primary))
            setBackgroundResource(if (selected) R.drawable.bremo_bg_avatar_large else R.drawable.bremo_bg_button_primary)
            minWidth = px(R.dimen.bremo_size_icon)
            minHeight = px(R.dimen.bremo_size_icon)
        }, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { marginStart = px(R.dimen.bremo_space_xs) })
    }

    private fun safetyNote() = row().apply {
        setBackgroundResource(R.drawable.bremo_bg_banner_info)
        setPadding(px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l))
        addView(FrameLayout(context).apply {
            setBackgroundResource(R.drawable.bremo_bg_button_primary)
            addView(icon(R.drawable.ic_shield, R.color.bremo_on_primary, R.dimen.bremo_size_icon), FrameLayout.LayoutParams(px(R.dimen.bremo_size_icon), px(R.dimen.bremo_size_icon), Gravity.CENTER))
        }, LinearLayout.LayoutParams(px(R.dimen.bremo_size_order_well), px(R.dimen.bremo_size_order_well)))
        addView(BremoLinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPaddingRelative(px(R.dimen.bremo_space_m), 0, 0, 0)
            addView(text(string(R.string.messages_safety_title), R.style.TextAppearance_Bremo_TileTitle))
            addView(text(string(R.string.messages_safety_body), R.style.TextAppearance_Bremo_Hint))
        }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
    }

    private fun row(gravity: Int = Gravity.CENTER_VERTICAL) = BremoLinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        this.gravity = gravity
    }

    private fun icon(@DrawableRes icon: Int, tint: Int, size: Int) = ImageView(context).apply {
        setImageResource(icon)
        ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(context.getColor(tint)))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        layoutParams = LinearLayout.LayoutParams(px(size), px(size))
    }

    private fun text(value: String, @StyleRes style: Int) = TextView(context).apply {
        text = value
        TextViewCompat.setTextAppearance(this, style)
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    }

    private fun px(id: Int) = resources.getDimensionPixelSize(id)
}
