package noox.bzr.customer

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import noox.bzr.design.BzrFormat
import noox.bzr.design.R
import noox.bzr.design.views.BremoLinearLayout
import noox.bzr.design.views.cardShadow
import noox.bzr.design.views.sizeCompoundIcons
import noox.bzr.gallery.databinding.ScreenC32NotificationsBinding

/**
 * SCR-C32 — Figma «الإشعارات» 3002:3608 (DEC-076): «اليوم» and «سابق» groups; each card has the type icon (from the
 * NTF code) with an unread dot, the title, body and relative time. Unread cards are white, read ones tinted. A settings
 * link shows when the device blocks notifications (DEC-058).
 */
class C32NotificationsView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC32NotificationsBinding.inflate(inflater, content)
    var onOpen: (Int) -> Unit = {}
    var onOpenSettings: () -> Unit = {}

    init {
        binding.permissionOff.text = string(R.string.notifications_permission_off)
        binding.openSettings.onClick = { onOpenSettings() }
    }

    override fun title(state: CustomerUiState) = string(R.string.notifications_title)

    override fun renderContent(state: CustomerUiState) {
        binding.permissionOff.isVisible = state.notificationsDenied
        binding.openSettings.isVisible = state.notificationsDenied
        binding.empty.isVisible = state.items.isEmpty()
        binding.notifications.isVisible = state.items.isNotEmpty()
        binding.notifications.removeAllViews()
        val (today, earlier) = state.items.indices.partition { state.stepStates.getOrNull(it) == "today" }
        listOf(R.string.notifications_today to today, R.string.notifications_earlier to earlier).forEach { (label, rows) ->
            if (rows.isEmpty()) return@forEach
            binding.notifications.addView(groupHeader(string(label), label == R.string.notifications_today))
            rows.forEach { binding.notifications.addView(card(state, it)) }
        }
    }

    private fun groupHeader(label: String, today: Boolean) = text(label, R.style.TextAppearance_Bremo_TileTitle).apply {
        setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.bremo_bg_status_dot, 0, 0, 0)
        TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(context.getColor(if (today) R.color.bremo_primary600 else R.color.bremo_slate300)))
        compoundDrawablePadding = px(R.dimen.bremo_space_s)
        sizeCompoundIcons(R.dimen.bremo_size_status_dot)
    }

    private fun card(state: CustomerUiState, index: Int): View {
        val unread = state.itemStates.getOrNull(index) == "unread"
        val title = state.items[index]
        val body = state.itemDetails.getOrElse(index) { "" }
        val time = age(state.orderAgeMinutes.getOrElse(index) { -1 }).ifEmpty { state.fieldValues.getOrElse(index) { "" } }
        return BremoLinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setBackgroundResource(if (unread) R.drawable.bremo_bg_card else R.drawable.bremo_bg_inset)
            if (unread) cardShadow()
            setPadding(px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l))
            contentDescription = listOfNotNull(title, body, time, string(R.string.notifications_unread).takeIf { unread })
                .joinToString(string(R.string.format_list_separator))
            isEnabled = !state.isBusy
            isClickable = !state.isBusy
            isFocusable = true
            setOnClickListener { onOpen(index) }
            addView(FrameLayout(context).apply {
                addView(FrameLayout(context).apply {
                    setBackgroundResource(if (unread) R.drawable.bremo_bg_round_tint else R.drawable.bremo_bg_well_round)
                    addView(ImageView(context).apply {
                        setImageResource(icon(state.optionCodes.getOrElse(index) { "" }))
                        ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(context.getColor(if (unread) R.color.bremo_primary600 else R.color.bremo_decorative_icon)))
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }, FrameLayout.LayoutParams(px(R.dimen.bremo_size_icon), px(R.dimen.bremo_size_icon), Gravity.CENTER))
                }, FrameLayout.LayoutParams(px(R.dimen.bremo_size_home_tile_well), px(R.dimen.bremo_size_home_tile_well)))
                if (unread) addView(View(context).apply { setBackgroundResource(R.drawable.bremo_bg_notification_dot) }, FrameLayout.LayoutParams(px(R.dimen.bremo_size_online_dot), px(R.dimen.bremo_size_online_dot), Gravity.TOP or Gravity.END))
            }, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
            addView(BremoLinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPaddingRelative(px(R.dimen.bremo_space_m), 0, 0, 0)
                addView(BremoLinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(text(title, R.style.TextAppearance_Bremo_TileTitle), LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                    if (time.isNotBlank()) addView(text(time, R.style.TextAppearance_Bremo_Hint).apply {
                        if (unread) setTextColor(context.getColor(R.color.bremo_primary600))
                    }, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { marginStart = px(R.dimen.bremo_space_s) })
                })
                if (body.isNotBlank()) addView(text(body, R.style.TextAppearance_Bremo_Secondary))
            }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    private fun age(minutes: Int): String = when {
        minutes < 0 -> ""
        minutes < 1 -> string(R.string.time_ago_now)
        minutes < MINUTES_PER_HOUR -> BzrFormat.quantity(context, R.plurals.plural_time_ago_minutes, minutes)
        minutes < MINUTES_PER_DAY -> BzrFormat.quantity(context, R.plurals.plural_time_ago_hours, minutes / MINUTES_PER_HOUR)
        else -> BzrFormat.quantity(context, R.plurals.plural_time_ago_days, minutes / MINUTES_PER_DAY)
    }

    private fun text(value: String, @StyleRes style: Int) = TextView(context).apply {
        text = value
        TextViewCompat.setTextAppearance(this, style)
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    }

    private fun px(id: Int) = resources.getDimensionPixelSize(id)

    private companion object {
        const val MINUTES_PER_HOUR = 60
        const val MINUTES_PER_DAY = 1440

        /** NotificationCode → the Figma icon of that event (tag for offers, navigation on the way, receipt for money…). */
        fun icon(code: String): Int = when (code) {
            "NTF-04", "NTF-21" -> R.drawable.ic_tag
            "NTF-05", "NTF-16", "NTF-28", "NTF-29" -> R.drawable.ic_check
            "NTF-07", "NTF-08" -> R.drawable.ic_navigation
            "NTF-09" -> R.drawable.ic_location
            "NTF-10", "NTF-12" -> R.drawable.ic_info
            "NTF-11" -> R.drawable.ic_wrench
            "NTF-13", "NTF-14", "NTF-15", "NTF-30" -> R.drawable.ic_receipt
            "NTF-18" -> R.drawable.ic_star
            "NTF-02", "NTF-03", "NTF-19", "NTF-20" -> R.drawable.ic_warning
            "NTF-25" -> R.drawable.ic_messages
            else -> R.drawable.ic_bell
        }
    }
}
