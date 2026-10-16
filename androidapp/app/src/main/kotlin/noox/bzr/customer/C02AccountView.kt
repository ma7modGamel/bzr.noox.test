package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.views.AppBottomSheetView
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import noox.bzr.design.BzrFormat
import noox.bzr.design.R
import noox.bzr.design.views.BremoLinearLayout
import noox.bzr.design.views.InfoBannerView
import noox.bzr.design.views.PrimaryButtonView
import noox.bzr.design.views.cardShadow
import noox.bzr.design.views.sizeCompoundIcons
import noox.bzr.gallery.databinding.ScreenC02AccountBinding

/**
 * SCR-C02 — Figma «حسابي» 3002:4654 (DEC-076): the profile card (avatar, name, masked phone, «تعديل الملف الشخصي»),
 * three stat tiles, the provider-mode card, the menu and «تسجيل الخروج». `fieldValues`: name, phone, completed
 * orders, saved addresses, active orders. «طرق الدفع» is left out (ASM-05: no saved cards) and «تقييماتي» too (B-10: no endpoint).
 */
class C02AccountView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC02AccountBinding.inflate(inflater, content)
    var loadImage: (String, (android.graphics.Bitmap?) -> Unit) -> Unit = { _, ready -> ready(null) }
    var onOpen: (String) -> Unit = {}
    var onConfirmLogout: () -> Unit = {}
    var onDismissLogout: () -> Unit = {}
    private val logoutSheet = AppBottomSheetView(context).apply {
        title = string(R.string.account_logout_confirm_title)
        body = string(R.string.account_logout_confirm_body)
        action = string(R.string.customer_account_logout)
        secondaryAction = string(R.string.common_cancel)
        onAction = { onConfirmLogout() }
        onSecondary = { onDismissLogout() }
    }

    init { overlay.addView(logoutSheet) }

    override fun renderOverlay(state: CustomerUiState) {
        logoutSheet.isVisible = state.showLogoutConfirmation
    }

    override fun title(state: CustomerUiState) = string(R.string.customer_account_title)

    override fun renderContent(state: CustomerUiState) {
        val name = state.fieldValues.getOrElse(0) { "" }
        val addresses = state.fieldValues.getOrElse(3) { "" }
        binding.body.removeAllViews()
        binding.body.addView(profileCard(name, state.fieldValues.getOrElse(1) { "" }, state.accountAvatarUrl, state.messageKey == "account.verified"))
        if (state.messageKey == "account.unverified") binding.body.addView(InfoBannerView(context).apply { text = string(R.string.account_unverified) })
        binding.body.addView(row().apply {
            dividerDrawable = ContextCompat.getDrawable(context, R.drawable.bremo_gap_m)
            showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
            gravity = Gravity.TOP
            addView(stat(R.drawable.ic_check, R.color.bremo_success, R.color.bremo_success_tint, state.fieldValues.getOrElse(2) { "" }, string(R.string.account_stats_completed)), weight())
            addView(stat(R.drawable.ic_orders, R.color.bremo_decorative_icon, R.color.bremo_accent_tint, state.fieldValues.getOrElse(4) { "" }, string(R.string.account_stats_active)), weight())
            addView(stat(R.drawable.ic_location, R.color.bremo_slate400, R.color.bremo_surface_container, addresses, string(R.string.account_stats_addresses)), weight())
        })
        if ("open_provider_mode" in state.visibleActions) binding.body.addView(providerCard())
        binding.body.addView(menuRow(R.drawable.ic_info, string(R.string.account_language), null) {
            val language = noox.bzr.network.BremoHttp.language()
            AlertDialog.Builder(context)
                .setTitle(string(R.string.account_language))
                .setSingleChoiceItems(arrayOf(string(R.string.language_ar), string(R.string.language_en)), if (language == "ar") 0 else 1) { dialog, index ->
                    dialog.dismiss()
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(if (index == 0) "ar" else "en"))
                }
                .show()
        })
        binding.body.addView(column(0).apply {
            setBackgroundResource(R.drawable.bremo_bg_card)
            cardShadow()
            listOf(
                Triple(R.drawable.ic_account, string(R.string.customer_account_personal), "SCR-C33"),
                Triple(R.drawable.ic_location, string(R.string.customer_account_addresses), "SCR-C14"),
                Triple(R.drawable.ic_bell, string(R.string.notifications_title), "SCR-C32"),
                Triple(R.drawable.ic_info, string(R.string.customer_account_help), "SCR-C29"),
                Triple(R.drawable.ic_shield, string(R.string.account_terms), "SCR-C34"),
            ).forEachIndexed { index, (icon, label, route) ->
                if (index > 0) addView(View(context).apply { setBackgroundResource(R.drawable.bremo_bg_divider) }, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, px(R.dimen.bremo_border_width)))
                val chip = if (route == "SCR-C14" && addresses.isNotBlank()) BzrFormat.quantity(context, R.plurals.plural_account_addresses_count, (addresses).toIntOrNull() ?: 0) else null
                addView(menuRow(icon, label, chip) { onOpen(route) })
            }
        })
        binding.body.addView(row().apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = px(R.dimen.bremo_radius_card).toFloat()
                setColor(context.getColor(R.color.bremo_danger_tint))
            }
            minimumHeight = px(R.dimen.bremo_size_primary_button_height)
            isClickable = true
            isFocusable = true
            contentDescription = string(R.string.customer_account_logout)
            setOnClickListener { onOpen("SCR-C10") }
            addView(icon(R.drawable.ic_logout, R.color.bremo_danger, R.dimen.bremo_size_icon_small))
            addView(text(string(R.string.customer_account_logout), R.style.TextAppearance_Bremo_TileTitle).apply {
                setTextColor(context.getColor(R.color.bremo_danger))
                setPaddingRelative(px(R.dimen.bremo_space_xs), 0, 0, 0)
            })
        })
    }

    private fun profileCard(name: String, phone: String, avatarUrl: String, verified: Boolean) = column(R.drawable.bremo_gap_s).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        setBackgroundResource(R.drawable.bremo_bg_card)
        cardShadow()
        setPadding(px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l))
        if (avatarUrl.isNotBlank()) {
            addView(AccountAvatarView(context).apply {
                loadImage = this@C02AccountView.loadImage
                contentDescription = name
                bind(avatarUrl)
                setOnClickListener { onOpen("SCR-C33") }
            }, LinearLayout.LayoutParams(px(R.dimen.bremo_size_profile_avatar), px(R.dimen.bremo_size_profile_avatar)))
        } else {
        addView(text(name.trim().take(1), R.style.TextAppearance_Bremo_ProfileName).apply {
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(context.getColor(R.color.bremo_decorative_icon))
            setBackgroundResource(R.drawable.bremo_bg_avatar_ring)
        }, LinearLayout.LayoutParams(px(R.dimen.bremo_size_profile_avatar), px(R.dimen.bremo_size_profile_avatar)))
        }
        addView(text(name, R.style.TextAppearance_Bremo_ProfileName).apply {
            if (verified) {
                setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_verified, 0)
                TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(context.getColor(R.color.bremo_verified_text)))
                compoundDrawablePadding = px(R.dimen.bremo_space_xs)
                sizeCompoundIcons(R.dimen.bremo_size_icon_small)
                contentDescription = listOf(name, string(R.string.account_verified)).joinToString(string(R.string.format_list_separator))
            }
        }, wrap())
        if (phone.isNotBlank()) addView(text(maskPhone(phone), R.style.TextAppearance_Bremo_Secondary).apply { textDirection = View.TEXT_DIRECTION_LTR }, wrap())
        addView(text(string(R.string.account_edit_profile), R.style.TextAppearance_Bremo_Caption).apply {
            setTextColor(context.getColor(R.color.bremo_navy900))
            setBackgroundResource(R.drawable.bremo_bg_info_pill)
            gravity = Gravity.CENTER
            minHeight = px(R.dimen.bremo_size_chip_height)
            setPaddingRelative(px(R.dimen.bremo_space_l), 0, px(R.dimen.bremo_space_l), 0)
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_edit, 0, 0, 0)
            TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(context.getColor(R.color.bremo_decorative_icon)))
            compoundDrawablePadding = px(R.dimen.bremo_space_xs)
            sizeCompoundIcons(R.dimen.bremo_size_icon_small)
            isClickable = true
            isFocusable = true
            setOnClickListener { onOpen("SCR-C33") }
        }, wrap())
    }

    private fun stat(@DrawableRes icon: Int, tint: Int, well: Int, value: String, label: String) = column(R.drawable.bremo_gap_xs).apply {
        gravity = Gravity.CENTER_HORIZONTAL
        setBackgroundResource(R.drawable.bremo_bg_card)
        cardShadow()
        setPadding(px(R.dimen.bremo_space_s), px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_s), px(R.dimen.bremo_space_m))
        contentDescription = listOf(value, label).filter(String::isNotBlank).joinToString(" ")
        addView(FrameLayout(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(context.getColor(well))
            }
            addView(icon(icon, tint, R.dimen.bremo_size_icon_small), FrameLayout.LayoutParams(px(R.dimen.bremo_size_icon_small), px(R.dimen.bremo_size_icon_small), Gravity.CENTER))
        }, LinearLayout.LayoutParams(px(R.dimen.bremo_size_order_well), px(R.dimen.bremo_size_order_well)))
        addView(text(value.ifBlank { "—" }, R.style.TextAppearance_Bremo_StatValue).apply { textAlignment = View.TEXT_ALIGNMENT_CENTER }, wrap())
        addView(text(label, R.style.TextAppearance_Bremo_Hint).apply { textAlignment = View.TEXT_ALIGNMENT_CENTER }, wrap())
    }

    private fun providerCard() = column(R.drawable.bremo_gap_m).apply {
        setBackgroundResource(R.drawable.bremo_bg_banner_info)
        setPadding(px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l))
        addView(row().apply {
            addView(text(string(R.string.account_provider_title), R.style.TextAppearance_Bremo_HomeSection).apply {
                setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_wrench, 0, 0, 0)
                TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(context.getColor(R.color.bremo_decorative_icon)))
                compoundDrawablePadding = px(R.dimen.bremo_space_s)
                sizeCompoundIcons(R.dimen.bremo_size_icon)
            }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            addView(text(string(R.string.menu_provider_mode), R.style.TextAppearance_Bremo_Hint).apply {
                setTextColor(context.getColor(R.color.bremo_on_primary))
                setBackgroundResource(R.drawable.bremo_bg_button_primary)
                setPaddingRelative(px(R.dimen.bremo_space_s), 0, px(R.dimen.bremo_space_s), 0)
            })
        })
        addView(text(string(R.string.account_provider_body), R.style.TextAppearance_Bremo_Secondary))
        addView(PrimaryButtonView(context).apply {
            text = string(R.string.account_provider_action)
            icon = R.drawable.ic_arrow_forward
            onClick = { onOpen("SCR-P01") }
        })
    }

    private fun menuRow(@DrawableRes icon: Int, label: String, chip: String?, onClick: () -> Unit) = row().apply {
        minimumHeight = px(R.dimen.bremo_size_touch_target_min)
        setPadding(px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_m))
        isClickable = true
        isFocusable = true
        contentDescription = listOfNotNull(label, chip).joinToString(string(R.string.format_list_separator))
        setOnClickListener { onClick() }
        addView(FrameLayout(context).apply {
            setBackgroundResource(R.drawable.bremo_bg_menu_icon_box)
            addView(icon(icon, R.color.bremo_decorative_icon, R.dimen.bremo_size_icon), FrameLayout.LayoutParams(px(R.dimen.bremo_size_icon), px(R.dimen.bremo_size_icon), Gravity.CENTER))
        }, LinearLayout.LayoutParams(px(R.dimen.bremo_size_order_well), px(R.dimen.bremo_size_order_well)))
        addView(text(label, R.style.TextAppearance_Bremo_TileTitle).apply { setPaddingRelative(px(R.dimen.bremo_space_m), 0, 0, 0) }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        if (chip != null) addView(text(chip, R.style.TextAppearance_Bremo_Hint).apply {
            setBackgroundResource(R.drawable.bremo_bg_info_pill)
            setPaddingRelative(px(R.dimen.bremo_space_s), 0, px(R.dimen.bremo_space_s), 0)
        })
        addView(icon(R.drawable.ic_chevron, R.color.bremo_slate400, R.dimen.bremo_size_icon_small))
    }

    /** "010 •••• 5678": the start and the last four digits only. */
    private fun maskPhone(phone: String): String =
        if (phone.length < PHONE_VISIBLE + PHONE_PREFIX) phone else "${phone.take(PHONE_PREFIX)} •••• ${phone.takeLast(PHONE_VISIBLE)}"

    private fun weight() = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)

    private fun wrap() = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

    private fun column(@DrawableRes gap: Int) = BremoLinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        if (gap != 0) {
            dividerDrawable = ContextCompat.getDrawable(context, gap)
            showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        }
    }

    private fun row() = BremoLinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
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

    private companion object {
        const val PHONE_PREFIX = 3
        const val PHONE_VISIBLE = 4
        // STATIC (DEC-074, BACKEND-NOTES B-09).
    }
}
