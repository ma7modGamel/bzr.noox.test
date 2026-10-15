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
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import noox.bzr.design.BzrFormat
import noox.bzr.design.R
import noox.bzr.design.StepState
import noox.bzr.design.views.BremoLinearLayout
import noox.bzr.design.views.PrimaryButtonView
import noox.bzr.design.views.SecondaryButtonView
import noox.bzr.design.views.StatusStepperView
import noox.bzr.design.views.cardShadow
import noox.bzr.design.views.sizeCompoundIcons
import noox.bzr.gallery.databinding.ScreenC09TrackingBinding

/**
 * SCR-C09 — Figma «Primo App» tracking (DEC-076), one layout per status: CONFIRMED 3002:1631, ON_THE_WAY 3002:1903,
 * ARRIVED 3002:2055, IN_PROGRESS 3002:2540; other statuses keep the status card, stepper and actions.
 * `fieldValues`: number, address, problem, agreed price, materials, provider name, rating, completed, category,
 * approved total, slot; `optionCodes`: status, timing type.
 */
class C09TrackingView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC09TrackingBinding.inflate(inflater, content)
    var onAction: (String) -> Unit = {}

    /** The last ETA and distance from `GET /tracking`; renders keep them instead of a sample value (CFG-081). */
    private var etaMinutes: Int? = null
    private var distanceKm: Float? = null

    /** Actions this render already shows in a card or the dock, so the action list does not repeat them. */
    private val shown = mutableSetOf<String>()

    fun attachNativeMap(view: View, onMyLocation: () -> Unit) {
        binding.map.setNativeContent(view)
        binding.map.onMyLocation = onMyLocation
    }

    fun updateEta(minutes: Int?, approximate: Boolean, km: Float? = null) {
        if (minutes != null) etaMinutes = minutes
        if (km != null) distanceKm = km
        renderEtaCard()
    }

    private val shareBanner = noox.bzr.design.views.InfoBannerView(context)
    private val warrantyBanner = noox.bzr.design.views.InfoBannerView(context)

    init {
        content.addView(warrantyBanner, 0)
        content.addView(shareBanner, 1)
        pinAction(binding.dock)
        binding.etaCard.cardShadow()
        binding.actions.onAction = { onAction(it) }
    }

    override fun title(state: CustomerUiState) = string(
        when (state.optionCodes.firstOrNull()) {
            "CONFIRMED" -> R.string.tracking_title_confirmed
            "ON_THE_WAY" -> R.string.tracking_step_on_the_way
            "ARRIVED" -> R.string.tracking_title_arrived
            "IN_PROGRESS" -> R.string.tracking_step_in_progress
            else -> R.string.tracking_title
        },
    )

    override fun renderContent(state: CustomerUiState) {
        shareBanner.isVisible = state.isBusy || state.messageKey == "share.error"
        shareBanner.text = string(if (state.isBusy) R.string.share_loading else R.string.share_error)
        warrantyBanner.isVisible = state.warrantyCoverage.isNotBlank()
        warrantyBanner.text = state.warrantyCoverage
        val status = state.optionCodes.firstOrNull().orEmpty()
        binding.offline.isVisible = state.phase == CustomerPhase.Offline
        binding.mapBlock.isVisible = state.showMap
        binding.sections.removeAllViews()
        binding.links.removeAllViews()
        binding.primary.isVisible = false
        shown.clear()
        val actions = state.visibleActions
        when (status) {
            "CONFIRMED" -> confirmed(state)
            "ON_THE_WAY" -> onTheWay(state)
            "ARRIVED" -> arrived(state)
            "IN_PROGRESS" -> inProgress(state)
            else -> generic(state)
        }
        binding.actions.actions = actions.filter { it !in shown }
        binding.dock.isVisible = binding.primary.isVisible || binding.links.childCount > 0
        if (state.arrivalCodeRequired) {
            binding.sections.addView(card().apply {
                addView(text(string(R.string.arrival_code_title), R.style.TextAppearance_Bremo_HomeSection))
                addView(text(string(R.string.arrival_code_body), R.style.TextAppearance_Bremo_Secondary))
                if (state.arrivalCode.isNotEmpty()) {
                    addView(text(state.arrivalCode, R.style.TextAppearance_Bremo_AmountDisplay).apply { textDirection = android.view.View.TEXT_DIRECTION_LTR })
                    addView(text(BzrFormat.fill(string(R.string.arrival_code_until), "time" to state.arrivalCodeUntil), R.style.TextAppearance_Bremo_Caption))
                }
                if (state.arrivalCodeFailed) addView(text(string(R.string.arrival_code_failed), R.style.TextAppearance_Bremo_Secondary))
                addView(noox.bzr.design.views.SecondaryButtonView(context).apply { text = string(R.string.arrival_code_refresh); onClick = { onAction("refresh_arrival_code") } })
                if (state.arrivalExceptionPending) addView(text(string(R.string.arrival_exception_pending), R.style.TextAppearance_Bremo_Secondary))
            })
        }
        renderEtaCard()
    }

    // ── CONFIRMED (3002:1631) ──────────────────────────────────────────────────────────────────────────────────────
    private fun confirmed(state: CustomerUiState) {
        hero(R.drawable.ic_check, null, BzrFormat.fill(string(R.string.tracking_confirmed_heading), "name" to firstName(state)), string(R.string.tracking_confirmed_body))
        val card = providerCard(state)
        val eta = if (state.optionCodes.getOrNull(1) == "NOW") "" else value(state, SLOT)
        if (eta.isNotEmpty()) card.addView(inset(R.drawable.ic_clock, BzrFormat.fill(string(R.string.tracking_expected), "eta" to eta)))
        card.addView(contactRow(state, callFirst = false))
        serviceCard(state)
        val steps = listOf(R.string.tracking_step_confirmed, R.string.tracking_step_on_the_way, R.string.tracking_step_arrived, R.string.tracking_step_in_progress)
        stepperCard(
            string(R.string.tracking_steps), null, steps.map(::string),
            listOf(R.string.tracking_step_confirmed_body, R.string.tracking_step_next, R.string.tracking_step_arrived_body, R.string.tracking_step_in_progress_body).map(::string),
            listOf(StepState.Done, StepState.Pending, StepState.Pending, StepState.Pending),
        )
        primary(string(R.string.tracking_follow), R.drawable.ic_arrow_forward, "refresh")
    }

    // ── ON_THE_WAY (3002:1903) ─────────────────────────────────────────────────────────────────────────────────────
    private fun onTheWay(state: CustomerUiState) {
        val card = providerCard(state)
        card.addView(contactRow(state, callFirst = true))
        card.addView(inset(R.drawable.ic_navigation, BzrFormat.fill(string(R.string.tracking_moving_title), "name" to firstName(state)), string(R.string.tracking_moving_body)))
        card.addView(labeledRow(R.drawable.ic_location, R.string.tracking_visit_location, value(state, ADDRESS)))
        card.addView(labeledRow(R.drawable.ic_wrench, R.string.tracking_requested_service, value(state, TITLE)))
        primary(string(R.string.tracking_follow_trip), R.drawable.ic_navigation, "refresh")
        if ("share_visit" in state.visibleActions) link(R.drawable.ic_share, string(R.string.action_share_visit), "share_visit")
        link(R.drawable.ic_info, string(R.string.tracking_help), "help")
    }

    // ── ARRIVED (3002:2055) ────────────────────────────────────────────────────────────────────────────────────────
    private fun arrived(state: CustomerUiState) {
        hero(R.drawable.ic_account, string(R.string.tracking_arrived_pill), BzrFormat.fill(string(R.string.tracking_arrived_heading), "name" to value(state, NAME)), string(R.string.tracking_arrived_body))
        // The Figma arrival code and «تأكيد وصول الفني» have no API counterpart (BACKEND-NOTES B-22); the card keeps the identity.
        providerCard(state)
        val details = card()
        val head = row()
        head.addView(text(string(R.string.tracking_order_details), R.style.TextAppearance_Bremo_Caption), LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        head.addView(pill(BzrFormat.fill(string(if (state.optionCodes.getOrNull(1) == "NOW") R.string.tracking_kind_now else R.string.tracking_kind_scheduled), "number" to value(state, NUMBER))))
        details.addView(head)
        details.addView(iconTitle(R.drawable.ic_wrench, value(state, TITLE)))
        details.addView(priceInset(state))
        binding.sections.addView(details)
        val buttons = row()
        if ("chat" in state.visibleActions) buttons.addView(secondary(string(R.string.tracking_quick_chat), "chat").apply { icon = R.drawable.ic_messages }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        if ("call" in state.visibleActions) buttons.addView(secondary(string(R.string.tracking_call_provider), "call").apply { icon = R.drawable.ic_phone }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = px(R.dimen.bremo_space_s) })
        if (buttons.childCount > 0) binding.sections.addView(buttons)
        reportLink(state, string(R.string.tracking_report_mismatch))
    }

    // ── IN_PROGRESS (3002:2540) ────────────────────────────────────────────────────────────────────────────────────
    private fun inProgress(state: CustomerUiState) {
        binding.sections.addView(text(BzrFormat.fill(string(R.string.tracking_working_pill), "name" to firstName(state)), R.style.TextAppearance_Bremo_StatusPill).apply {
            setBackgroundResource(R.drawable.bremo_bg_status_pill)
            setPaddingRelative(px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_s), px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_s))
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.bremo_bg_online_dot, 0, 0, 0)
            compoundDrawablePadding = px(R.dimen.bremo_space_s)
            sizeCompoundIcons(R.dimen.bremo_size_status_dot)
        })
        val card = providerCard(state, compact = true)
        card.addView(contactRow(state, callFirst = false))
        val estimatedAt = state.fieldValues.getOrNull(11).orEmpty()
        if (estimatedAt.isNotEmpty()) {
            val overdue = state.fieldValues.getOrNull(12) == "overdue"
            binding.sections.addView(inset(R.drawable.ic_clock,
                BzrFormat.fill(string(R.string.tracking_finish_estimated_at), "time" to estimatedAt),
                string(if (overdue) R.string.tracking_finish_overdue else R.string.tracking_finish_from_technician)))
        }
        if (state.itemDetails.isNotEmpty()) {
            val active = state.itemStates.indexOf("active").takeIf { it >= 0 }?.plus(1)
            stepperCard(string(R.string.tracking_exec_steps),
                active?.let { BzrFormat.fill(string(R.string.tracking_stage), "current" to BzrFormat.number(it), "total" to BzrFormat.number(state.itemDetails.size)) }.orEmpty(),
                state.itemDetails, state.options, state.itemStates.map { when (it) { "done" -> StepState.Done; "active" -> StepState.Active; else -> StepState.Pending } })
        }
        val details = card()
        details.addView(text(string(R.string.tracking_current_details), R.style.TextAppearance_Bremo_HomeSection))
        details.addView(inset(null, string(R.string.tracking_requested_service), value(state, TITLE)))
        val total = value(state, TOTAL).ifEmpty { value(state, PRICE) }
        if (total.isNotEmpty()) {
            val line = row()
            line.addView(BremoLinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(text(string(R.string.tracking_total), R.style.TextAppearance_Bremo_TileTitle))
                addView(text(string(R.string.tracking_total_note), R.style.TextAppearance_Bremo_Hint))
            }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            line.addView(text(amount(total), R.style.TextAppearance_Bremo_OfferPrice))
            details.addView(line)
        }
        binding.sections.addView(details)
        primary(string(R.string.tracking_follow_service), R.drawable.ic_arrow_forward, "refresh")
        reportLinkInDock(state)
    }

    // ── Other statuses: status, stepper, actions ───────────────────────────────────────────────────────────────────
    private fun generic(state: CustomerUiState) {
        if (state.displayStatus.isNotEmpty()) binding.sections.addView(pill(displayStatus(state.displayStatus)))
        if (state.showStatusCard) {
            binding.sections.addView(inset(R.drawable.ic_info, string(
                when (state.messageKey) {
                    "tracking.status.open" -> R.string.tracking_status_open
                    "tracking.status.expired" -> R.string.tracking_status_expired
                    else -> R.string.tracking_status_cancelled
                },
            )))
        }
        if (state.messageKey == "status.reviewing") binding.sections.addView(inset(R.drawable.ic_warning, string(R.string.status_reviewing)))
        if (state.showStepper) {
            val labels = listOf(
                R.string.status_confirmed, R.string.status_on_the_way, R.string.status_arrived,
                R.string.status_in_progress, R.string.status_payment, R.string.status_closed,
            ).map(::string)
            val states = state.stepStates.ifEmpty { listOf("done", "active", "pending", "pending", "pending", "pending") }.map(::stepState)
            stepperCard(string(R.string.tracking_steps), null, labels, emptyList(), labels.indices.map { states.getOrElse(it) { StepState.Pending } })
        }
        if (value(state, TITLE).isNotEmpty()) serviceCard(state)
    }

    // ── Building blocks ────────────────────────────────────────────────────────────────────────────────────────────
    private fun hero(@DrawableRes icon: Int, pillText: String?, heading: String, body: String) {
        val block = BremoLinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            dividerDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bremo_gap_s)
            showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        }
        block.addView(FrameLayout(context).apply {
            setBackgroundResource(R.drawable.bremo_bg_success_ring)
            addView(FrameLayout(context).apply {
                setBackgroundResource(R.drawable.bremo_bg_success_core)
                addView(icon(icon, R.color.bremo_on_primary), FrameLayout.LayoutParams(px(R.dimen.bremo_size_icon), px(R.dimen.bremo_size_icon), Gravity.CENTER))
            }, FrameLayout.LayoutParams(px(R.dimen.bremo_size_success_core), px(R.dimen.bremo_size_success_core), Gravity.CENTER))
        }, LinearLayout.LayoutParams(px(R.dimen.bremo_size_success_ring), px(R.dimen.bremo_size_success_ring)))
        pillText?.let { block.addView(pill(it)) }
        block.addView(text(heading, R.style.TextAppearance_Bremo_ScreenTitle).apply { gravity = Gravity.CENTER })
        block.addView(text(body, R.style.TextAppearance_Bremo_Subtitle).apply { gravity = Gravity.CENTER })
        binding.sections.addView(block)
    }

    private fun providerCard(state: CustomerUiState, compact: Boolean = false): LinearLayout {
        val card = card()
        val top = row()
        top.addView(BremoLinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val nameRow = row()
            nameRow.addView(text(value(state, NAME), R.style.TextAppearance_Bremo_OfferName))
            if (state.providerVerified) nameRow.addView(icon(R.drawable.ic_verified, R.color.bremo_primary600, R.dimen.bremo_size_icon_small).apply {
                layoutParams = LinearLayout.LayoutParams(px(R.dimen.bremo_size_icon_small), px(R.dimen.bremo_size_icon_small)).apply { marginStart = px(R.dimen.bremo_space_xs) }
            })
            addView(nameRow)
            val meta = listOfNotNull(
                BzrFormat.fill(string(R.string.provider_verified_title), "category" to value(state, CATEGORY)).takeIf { state.providerVerified && value(state, CATEGORY).isNotEmpty() },
                value(state, COMPLETED).takeIf(String::isNotEmpty)?.let { BzrFormat.quantity(context, R.plurals.plural_confirm_completed, (it).toIntOrNull() ?: 0) }.takeUnless { compact },
            ).joinToString(" • ")
            if (meta.isNotEmpty()) addView(text(meta, R.style.TextAppearance_Bremo_Hint).apply { setTextColor(context.getColor(R.color.bremo_primary600)) })
        }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        value(state, RATING).takeIf(String::isNotEmpty)?.let { rating ->
            top.addView(text(rating, R.style.TextAppearance_Bremo_DetailStrong).apply {
                setBackgroundResource(R.drawable.bremo_bg_info_pill)
                setPaddingRelative(px(R.dimen.bremo_space_s), 0, px(R.dimen.bremo_space_s), 0)
                setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_star_filled, 0, 0, 0)
                TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(context.getColor(R.color.bremo_star)))
                compoundDrawablePadding = px(R.dimen.bremo_space_xs)
                sizeCompoundIcons(R.dimen.bremo_size_icon_small)
            })
        }
        card.addView(top)
        binding.sections.addView(card)
        return card
    }

    private fun contactRow(state: CustomerUiState, callFirst: Boolean): View {
        val buttons = row()
        val call = "call" in state.visibleActions
        val chat = "chat" in state.visibleActions
        val order = if (callFirst) listOf("call", "chat") else listOf("chat", "call")
        order.filter { (it == "call" && call) || (it == "chat" && chat) }.forEachIndexed { index, action ->
            val label = string(if (action == "call") R.string.tracking_call else R.string.tracking_message)
            val view = if (callFirst && action == "call") {
                PrimaryButtonView(context).also { shown += action }.apply {
                    text = label
                    compact = true
                    height = px(R.dimen.bremo_size_secondary_button_height)
                    icon = R.drawable.ic_phone
                    onClick = { onAction(action) }
                }
            } else {
                secondary(label, action).apply { icon = if (action == "call") R.drawable.ic_phone else R.drawable.ic_chat }
            }
            buttons.addView(view, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { if (index > 0) marginStart = px(R.dimen.bremo_space_s) })
        }
        return buttons
    }

    private fun serviceCard(state: CustomerUiState) {
        val card = card()
        val line = row()
        line.addView(BremoLinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(text(string(R.string.tracking_service_details), R.style.TextAppearance_Bremo_Hint))
            addView(text(value(state, TITLE), R.style.TextAppearance_Bremo_TileTitle))
        }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        if (value(state, PRICE).isNotEmpty()) {
            line.addView(BremoLinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.END
                addView(text(amount(value(state, PRICE)), R.style.TextAppearance_Bremo_OfferPrice))
                if (value(state, MATERIALS).isNotEmpty()) addView(text(value(state, MATERIALS), R.style.TextAppearance_Bremo_Hint))
            })
        }
        card.addView(line)
        if (value(state, ADDRESS).isNotEmpty()) card.addView(text(value(state, ADDRESS), R.style.TextAppearance_Bremo_Caption).apply {
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_location, 0, 0, 0)
            TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(context.getColor(R.color.bremo_primary600)))
            compoundDrawablePadding = px(R.dimen.bremo_space_xs)
            sizeCompoundIcons(R.dimen.bremo_size_icon_small)
        })
        binding.sections.addView(card)
    }

    private fun stepperCard(title: String, badge: String?, labels: List<String>, subtitles: List<String>, states: List<StepState>) {
        val card = card()
        val head = row()
        head.addView(text(title, R.style.TextAppearance_Bremo_HomeSection), LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        badge?.let { head.addView(pill(it)) }
        card.addView(head)
        card.addView(StatusStepperView(context).apply {
            vertical = true
            this.subtitles = subtitles
            steps = labels.zip(states)
        })
        binding.sections.addView(card)
    }

    private fun priceInset(state: CustomerUiState): View {
        val line = row().apply {
            setBackgroundResource(R.drawable.bremo_bg_inset)
            setPadding(px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_s), px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_s))
        }
        line.addView(text(string(R.string.tracking_agreed), R.style.TextAppearance_Bremo_Caption), LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        line.addView(text(amount(value(state, PRICE)), R.style.TextAppearance_Bremo_AmountSmall).apply { setTextColor(context.getColor(R.color.bremo_primary600)) })
        if (value(state, MATERIALS).isNotEmpty()) line.addView(text(value(state, MATERIALS), R.style.TextAppearance_Bremo_Hint).apply {
            setPaddingRelative(px(R.dimen.bremo_space_s), 0, 0, 0)
        })
        return line
    }

    private fun renderEtaCard() {
        binding.etaValue.text = etaMinutes?.let { BzrFormat.quantity(context, R.plurals.plural_offer_eta, it) } ?: "—"
        binding.etaDistance.text = distanceKm?.let { BzrFormat.fill(string(R.string.tracking_distance), "km" to BzrFormat.number((it * 10).toInt() / 10.0)) }.orEmpty()
        binding.etaDistance.isVisible = distanceKm != null
    }

    private fun reportLink(state: CustomerUiState, label: String) {
        val action = listOf("report_provider", "open_dispute").firstOrNull { it in state.visibleActions } ?: return
        link(R.drawable.ic_warning, label, action, danger = true)
    }

    private fun reportLinkInDock(state: CustomerUiState) = reportLink(state, string(R.string.tracking_report))

    private fun primary(label: String, @DrawableRes icon: Int, action: String) {
        binding.primary.text = label
        binding.primary.icon = icon
        binding.primary.onClick = { onAction(action) }
        binding.primary.isVisible = true
        shown += action
    }

    private fun link(@DrawableRes icon: Int, label: String, action: String, danger: Boolean = false) {
        shown += action
        binding.links.addView(text(label, R.style.TextAppearance_Bremo_Caption).apply {
            gravity = Gravity.CENTER
            minHeight = px(R.dimen.bremo_size_touch_target_min)
            setPaddingRelative(px(R.dimen.bremo_space_m), 0, px(R.dimen.bremo_space_m), 0)
            if (danger) setTextColor(context.getColor(R.color.bremo_danger))
            setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
            TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(context.getColor(if (danger) R.color.bremo_danger else R.color.bremo_slate500)))
            compoundDrawablePadding = px(R.dimen.bremo_space_xs)
            sizeCompoundIcons(R.dimen.bremo_size_icon_small)
            setOnClickListener { onAction(action) }
        })
    }

    private fun secondary(label: String, action: String) = SecondaryButtonView(context).also { shown += action }.apply {
        text = label
        onClick = { onAction(action) }
    }

    private fun inset(@DrawableRes icon: Int?, title: String, body: String = ""): View {
        val line = row().apply {
            setBackgroundResource(R.drawable.bremo_bg_inset)
            setPadding(px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_m))
        }
        line.addView(BremoLinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(text(title, if (body.isEmpty() && icon != null) R.style.TextAppearance_Bremo_DetailStrong else R.style.TextAppearance_Bremo_Hint))
            if (body.isNotEmpty()) addView(text(body, R.style.TextAppearance_Bremo_TileTitle))
        }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        icon?.let { line.addView(icon(it, R.color.bremo_primary600)) }
        return line
    }

    private fun labeledRow(@DrawableRes icon: Int, label: Int, value: String): View {
        val line = row()
        line.addView(BremoLinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(text(string(label), R.style.TextAppearance_Bremo_Hint))
            addView(text(value, R.style.TextAppearance_Bremo_TileTitle))
        }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        line.addView(FrameLayout(context).apply {
            setBackgroundResource(R.drawable.bremo_bg_order_well)
            addView(icon(icon, R.color.bremo_primary600, R.dimen.bremo_size_icon_small), FrameLayout.LayoutParams(px(R.dimen.bremo_size_icon_small), px(R.dimen.bremo_size_icon_small), Gravity.CENTER))
        }, LinearLayout.LayoutParams(px(R.dimen.bremo_size_order_well), px(R.dimen.bremo_size_order_well)))
        return line
    }

    private fun iconTitle(@DrawableRes icon: Int, title: String): View {
        val line = row()
        line.addView(text(title, R.style.TextAppearance_Bremo_HomeSection), LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        line.addView(FrameLayout(context).apply {
            setBackgroundResource(R.drawable.bremo_bg_order_well)
            addView(icon(icon, R.color.bremo_primary600, R.dimen.bremo_size_icon_small), FrameLayout.LayoutParams(px(R.dimen.bremo_size_icon_small), px(R.dimen.bremo_size_icon_small), Gravity.CENTER))
        }, LinearLayout.LayoutParams(px(R.dimen.bremo_size_order_well), px(R.dimen.bremo_size_order_well)))
        return line
    }

    private fun pill(value: String) = text(value, R.style.TextAppearance_Bremo_StatusPill).apply {
        setBackgroundResource(R.drawable.bremo_bg_status_pill)
        setPaddingRelative(px(R.dimen.bremo_space_s), 0, px(R.dimen.bremo_space_s), 0)
        layoutParams = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    private fun card() = BremoLinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.bremo_bg_card)
        cardShadow()
        dividerDrawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bremo_gap_m)
        showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        setPadding(px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l))
    }

    private fun row() = BremoLinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun icon(@DrawableRes icon: Int, tint: Int, size: Int = R.dimen.bremo_size_icon) = ImageView(context).apply {
        setImageResource(icon)
        ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(context.getColor(tint)))
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        layoutParams = LinearLayout.LayoutParams(px(size), px(size))
    }

    private fun text(value: String, @StyleRes style: Int) = TextView(context).apply {
        text = value
        TextViewCompat.setTextAppearance(this, style)
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    }

    private fun amount(value: String) = BzrFormat.fill(string(R.string.customer_home_offers_amount), "amount" to value)
    private fun value(state: CustomerUiState, index: Int) = state.fieldValues.getOrElse(index) { "" }
    private fun firstName(state: CustomerUiState) = value(state, NAME).substringBefore(' ')
    private fun px(id: Int) = resources.getDimensionPixelSize(id)

    private fun stepState(value: String): StepState = when (value) {
        "done" -> StepState.Done
        "active" -> StepState.Active
        "on_hold" -> StepState.OnHold
        else -> StepState.Pending
    }

    private companion object {
        const val NUMBER = 0
        const val ADDRESS = 1
        const val TITLE = 2
        const val PRICE = 3
        const val MATERIALS = 4
        const val NAME = 5
        const val RATING = 6
        const val COMPLETED = 7
        const val CATEGORY = 8
        const val TOTAL = 9
        const val SLOT = 10
    }
}
