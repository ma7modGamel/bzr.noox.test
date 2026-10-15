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
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.TextViewCompat
import noox.bzr.design.BzrFormat
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.R
import noox.bzr.design.views.BremoLinearLayout
import noox.bzr.design.views.DangerTextButtonView
import noox.bzr.design.views.PrimaryButtonView
import noox.bzr.design.views.SecondaryButtonView
import noox.bzr.design.views.cardShadow
import noox.bzr.design.views.sizeCompoundIcons
import noox.bzr.gallery.databinding.ScreenC30ProposalBinding

/**
 * SCR-C30 execution quote — Figma «نتيجة الفحص» 3002:2264 — and SCR-C31 additional cost — Figma 3002:4029 (DEC-076).
 * `items`: type, amount, reason, projected total; `fieldValues`: provider name, rating, category, agreed price.
 * The inspection photo exists in the API but the app loads no remote images yet, so a framed placeholder stands in.
 */
open class ProposalDecisionView(context: Context, private val screen: String) : CustomerScreenView(context) {
    private val binding = ScreenC30ProposalBinding.inflate(inflater, content)
    var loadImage: (String, (android.graphics.Bitmap?) -> Unit) -> Unit = { _, ready -> ready(null) }
    var onAction: (String) -> Unit = {}
    private var clockState: CustomerUiState? = null
    private val remainingText = text("", R.style.TextAppearance_Bremo_DetailStrong)
    private val clock = object : Runnable {
        override fun run() {
            val state = clockState ?: return
            if (state.proposalDeadline.isNotEmpty() && content.visibility == View.VISIBLE) {
                val remaining = CustomerLogic.proposalRemaining(state, (android.os.SystemClock.elapsedRealtime() / 1000).toInt())
                remainingText.text = "${string(R.string.proposal_remaining)}: ${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')}"
                if (remaining == 0 && state.canContinue) {
                    render(state.copy(canContinue = false, messageKey = "proposal.expired"))
                }
            }
            removeCallbacks(this)
            if (isAttachedToWindow) postDelayed(this, 1000)
        }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); clock.run() }
    override fun onDetachedFromWindow() { removeCallbacks(clock); super.onDetachedFromWindow() }

    private fun mayDecide(state: CustomerUiState): Boolean = state.canContinue &&
        (state.proposalDeadline.isEmpty() || CustomerLogic.proposalRemaining(state, (android.os.SystemClock.elapsedRealtime() / 1000).toInt()) > 0)

    init {
        pinAction(binding.dock)
    }

    override fun title(state: CustomerUiState) =
        string(if (screen == "SCR-C30") R.string.proposal_execution_title else R.string.proposal_additional_title)

    override fun renderContent(inputState: CustomerUiState) {
        val state = if (inputState.proposalDeadline.isNotEmpty() && CustomerLogic.proposalRemaining(inputState, (android.os.SystemClock.elapsedRealtime() / 1000).toInt()) == 0)
            inputState.copy(canContinue = false, messageKey = "proposal.expired") else inputState
        clockState = state
        binding.body.removeAllViews()
        binding.dock.removeAllViews()
        if (state.proposalDeadline.isNotEmpty()) binding.body.addView(card().apply {
            addView(text("${string(R.string.proposal_deadline)}: ${state.proposalDeadline}", R.style.TextAppearance_Bremo_Caption))
            (remainingText.parent as? android.view.ViewGroup)?.removeView(remainingText)
            addView(remainingText)
            if (state.proposalExpiryMessage.isNotEmpty()) addView(text(state.proposalExpiryMessage, R.style.TextAppearance_Bremo_Caption))
        })
        val remaining = CustomerLogic.proposalRemaining(state, (android.os.SystemClock.elapsedRealtime() / 1000).toInt())
        remainingText.text = "${string(R.string.proposal_remaining)}: ${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')}"
        removeCallbacks(clock)
        if (isAttachedToWindow) postDelayed(clock, 1000)
        val type = state.items.getOrElse(0) { "" }
        val amount = state.items.getOrElse(1) { "" }
        val reason = state.items.getOrElse(2) { "" }
        val total = state.items.getOrElse(3) { "" }
        val original = value(state, 3)
        if (screen == "SCR-C30") {
            binding.body.addView(banner(R.drawable.ic_check, string(R.string.proposal_inspection_done), R.drawable.bremo_bg_status_pill))
            binding.body.addView(providerRow(state))
            binding.body.addView(card().apply {
                addView(heading(string(R.string.proposal_diagnosis), R.drawable.ic_search, string(R.string.proposal_field)))
                if (reason.isNotEmpty()) addView(text(reason, R.style.TextAppearance_Bremo_Secondary))
                if (state.proposalPhotoUrl.isNotEmpty()) addView(photoSection(context, string(R.string.proposal_fault_photo), listOf(state.proposalPhotoUrl), loadImage))
            })
            if (original.isNotEmpty()) binding.body.addView(card().apply {
                addView(row().apply {
                    addView(text(string(R.string.proposal_agreement), R.style.TextAppearance_Bremo_Caption), LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                    addView(text(string(R.string.proposal_confirmed), R.style.TextAppearance_Bremo_StatusPill))
                })
                addView(row().apply {
                    addView(text(string(R.string.proposal_agreed_price), R.style.TextAppearance_Bremo_TileTitle), LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                    addView(text(amount(original), R.style.TextAppearance_Bremo_OfferPrice).apply { setTextColor(context.getColor(R.color.bremo_navy900)) })
                })
            }.apply { setBackgroundResource(R.drawable.bremo_bg_inset) })
            binding.body.addView(card().apply {
                addView(row().apply {
                    addView(text(type, R.style.TextAppearance_Bremo_Caption), LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                    addView(pendingPill())
                })
                addView(row().apply {
                    addView(text(reason, R.style.TextAppearance_Bremo_TileTitle), LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                    addView(text(amount(amount), R.style.TextAppearance_Bremo_StatValue).apply {
                        setBackgroundResource(R.drawable.bremo_bg_inset)
                        setPaddingRelative(px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_xs), px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_xs))
                    })
                })
                addView(banner(R.drawable.ic_info, string(R.string.proposal_not_added), R.drawable.bremo_bg_inset))
            })
            val secondary = row()
            if ("chat" in state.visibleActions) secondary.addView(SecondaryButtonView(context).apply {
                text = BzrFormat.fill(string(R.string.proposal_message), "name" to value(state, 0).substringBefore(' '))
                icon = R.drawable.ic_messages
                onClick = { onAction("chat") }
            }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            if ("open_dispute" in state.visibleActions) secondary.addView(SecondaryButtonView(context).apply {
                text = string(R.string.tracking_report)
                icon = R.drawable.ic_warning
                onClick = { onAction("open_dispute") }
            }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = px(R.dimen.bremo_space_s) })
            if (secondary.childCount > 0) binding.body.addView(secondary)
        } else {
            binding.body.addView(row().apply {
                setBackgroundResource(R.drawable.bremo_bg_banner_warning)
                setPadding(px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_m))
                addView(icon(R.drawable.ic_clock, R.color.bremo_amber500, R.dimen.bremo_size_icon_small))
                addView(text(string(R.string.proposal_status), R.style.TextAppearance_Bremo_TileTitle).apply {
                    setPaddingRelative(px(R.dimen.bremo_space_s), 0, 0, 0)
                }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                addView(pendingPill())
            })
            binding.body.addView(providerRow(state))
            if (reason.isNotEmpty()) binding.body.addView(banner(R.drawable.ic_info, reason, R.drawable.bremo_bg_inset))
            binding.body.addView(card().apply {
                addView(heading(string(R.string.proposal_cost_details), R.drawable.ic_receipt, null))
                if (original.isNotEmpty()) addView(line(string(R.string.proposal_current_total), amount(original), R.style.TextAppearance_Bremo_StatValue))
                addView(row().apply {
                    setBackgroundResource(R.drawable.bremo_bg_inset)
                    setPadding(px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_s), px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_s))
                    addView(icon(R.drawable.ic_wrench, R.color.bremo_primary600, R.dimen.bremo_size_icon_small))
                    addView(text(type, R.style.TextAppearance_Bremo_Caption).apply { setPaddingRelative(px(R.dimen.bremo_space_s), 0, 0, 0) }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                    addView(text(amount(amount), R.style.TextAppearance_Bremo_StatValue).apply { setTextColor(context.getColor(R.color.bremo_primary600)) })
                })
                addView(View(context).apply { setBackgroundResource(R.drawable.bremo_bg_divider) }, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, px(R.dimen.bremo_border_width)))
                addView(line(string(R.string.proposal_new_total), amount(total), R.style.TextAppearance_Bremo_AmountDisplay, R.color.bremo_primary600))
            })
            if (state.proposalPhotoUrl.isNotEmpty()) binding.body.addView(card().apply {
                addView(heading(string(R.string.proposal_photo), R.drawable.ic_image, null))
                addView(photoSection(context, string(R.string.proposal_fault_photo), listOf(state.proposalPhotoUrl), loadImage))
                addView(text(string(R.string.proposal_no_charge), R.style.TextAppearance_Bremo_Hint).apply { gravity = Gravity.CENTER })
            })
        }
        state.messageKey?.let { key ->
            binding.body.addView(banner(R.drawable.ic_warning, string(if (key == "proposal.expired") R.string.proposal_expired else R.string.proposal_deciding), R.drawable.bremo_bg_banner_warning))
        }
        dock(state, total)
    }

    private fun dock(state: CustomerUiState, total: String) {
        if (screen == "SCR-C30" && total.isNotEmpty()) {
            binding.dock.addView(line(string(R.string.proposal_total_after), amount(total), R.style.TextAppearance_Bremo_OfferPrice))
        }
        if ("approve_proposal" in state.visibleActions) binding.dock.addView(PrimaryButtonView(context).apply {
            text = BzrFormat.fill(string(R.string.proposal_approve), "amount" to total)
            icon = R.drawable.ic_check
            this.state = when {
                state.isBusy -> ButtonVisualState.Loading
                mayDecide(state) -> ButtonVisualState.Normal
                else -> ButtonVisualState.Disabled
            }
            onClick = { if (mayDecide(state)) onAction("approve_proposal") }
        })
        val secondary = row()
        if (screen == "SCR-C31" && "chat" in state.visibleActions) secondary.addView(SecondaryButtonView(context).apply {
            text = string(R.string.proposal_ask)
            icon = R.drawable.ic_messages
            onClick = { onAction("chat") }
        }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        if (secondary.childCount > 0) binding.dock.addView(secondary)
        if ("reject_proposal" in state.visibleActions) binding.dock.addView(DangerTextButtonView(context).apply {
            text = string(R.string.proposal_reject)
            isEnabled = mayDecide(state)
            getChildAt(0)?.isEnabled = mayDecide(state)
            alpha = if (mayDecide(state)) 1f else 0.5f
            onClick = { if (mayDecide(state)) onAction("reject_proposal") }
        })
    }

    private fun providerRow(state: CustomerUiState) = card().apply {
        addView(row().apply {
            addView(BremoLinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                addView(text(value(state, 0), R.style.TextAppearance_Bremo_OfferName))
                val category = value(state, 2)
                if (category.isNotEmpty()) addView(text(
                    if (state.providerVerified) BzrFormat.fill(string(R.string.provider_verified_title), "category" to category) else category,
                    R.style.TextAppearance_Bremo_Hint,
                ))
            }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            value(state, 1).takeIf(String::isNotEmpty)?.let { rating ->
                addView(text(rating, R.style.TextAppearance_Bremo_DetailStrong).apply {
                    setBackgroundResource(R.drawable.bremo_bg_info_pill)
                    setPaddingRelative(px(R.dimen.bremo_space_s), 0, px(R.dimen.bremo_space_s), 0)
                    setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_star_filled, 0, 0, 0)
                    TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(context.getColor(R.color.bremo_star)))
                    compoundDrawablePadding = px(R.dimen.bremo_space_xs)
                    sizeCompoundIcons(R.dimen.bremo_size_icon_small)
                })
            }
        })
    }

    private fun heading(title: String, @DrawableRes icon: Int, badge: String?) = row().apply {
        addView(icon(icon, R.color.bremo_primary600, R.dimen.bremo_size_icon_small))
        addView(text(title, R.style.TextAppearance_Bremo_HomeSection).apply { setPaddingRelative(px(R.dimen.bremo_space_s), 0, 0, 0) }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        badge?.let { addView(text(it, R.style.TextAppearance_Bremo_StatusPill).apply {
            setBackgroundResource(R.drawable.bremo_bg_pill)
            setPaddingRelative(px(R.dimen.bremo_space_s), 0, px(R.dimen.bremo_space_s), 0)
        }) }
    }

    private fun pendingPill() = text(string(R.string.proposal_pending), R.style.TextAppearance_Bremo_StatusPill).apply {
        setTextColor(context.getColor(R.color.bremo_warning_text))
        setBackgroundResource(R.drawable.bremo_bg_badge_highlight)
        setPaddingRelative(px(R.dimen.bremo_space_s), 0, px(R.dimen.bremo_space_s), 0)
    }

    private fun banner(@DrawableRes icon: Int, value: String, background: Int) = row().apply {
        setBackgroundResource(background)
        setPadding(px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_s), px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_s))
        addView(icon(icon, R.color.bremo_primary600, R.dimen.bremo_size_icon_small))
        addView(text(value, R.style.TextAppearance_Bremo_Caption).apply { setPaddingRelative(px(R.dimen.bremo_space_s), 0, 0, 0) }, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
    }


    private fun line(label: String, value: String, @StyleRes style: Int, colour: Int = R.color.bremo_navy900) = row().apply {
        addView(text(label, R.style.TextAppearance_Bremo_Caption), LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        addView(text(value, style).apply { setTextColor(context.getColor(colour)) })
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
    private fun px(id: Int) = resources.getDimensionPixelSize(id)
}

/** SCR-C30 (C30ExecutionQuoteView in SwiftUI). */
class C30ExecutionQuoteView(context: Context) : ProposalDecisionView(context, "SCR-C30")

/** SCR-C31 (C31AdditionalCostView in SwiftUI). */
class C31AdditionalCostView(context: Context) : ProposalDecisionView(context, "SCR-C31")
