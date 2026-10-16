package noox.bzr.customer

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.InsetDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
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
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.FieldVisualState
import noox.bzr.design.R
import noox.bzr.design.views.BremoLinearLayout
import noox.bzr.design.views.cardShadow
import noox.bzr.design.views.sizeCompoundIcons
import noox.bzr.gallery.databinding.ScreenC24RatingBinding

/**
 * SCR-C24 — Figma «قيّم الخدمة» 3002:4475 (DEC-076): «تم إنهاء الزيارة بنجاح», the technician card and question, the
 * overall stars (they set the three API dimensions), «تقييم التفاصيل», the quick tags, «ملاحظات إضافية» and the dock.
 * `fieldValues`: comment, provider name and job title; `items`: chosen tag indexes.
 * Optional after-repair photos are uploaded and attached to the review.
 */
class C24RatingView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC24RatingBinding.inflate(inflater, content)
    var onRate: (Int, Int) -> Unit = { _, _ -> }
    var onOverall: (Int) -> Unit = {}
    var onTag: (Int, String) -> Unit = { _, _ -> }
    var onCommentChange: (String) -> Unit = {}
    var onSubmit: () -> Unit = {}
    var onAddPhoto: () -> Unit = {}
    var onRemovePhoto: () -> Unit = {}
    private val photoButton = noox.bzr.design.views.SecondaryButtonView(context).apply { text = string(R.string.rating_photos_add); onClick = { onAddPhoto() } }
    private val removePhoto = noox.bzr.design.views.DangerTextButtonView(context).apply { text = string(R.string.common_delete); onClick = { onRemovePhoto() } }
    private val photoCount = TextView(context)
    private val dimensions = listOf(R.string.rating_quality, R.string.rating_punctuality, R.string.rating_conduct)

    private val providerHolder = card(Gravity.CENTER_HORIZONTAL)
    private val overallStars = row()
    private val detailStars = List(dimensions.size) { row() }
    private val tagFlow = TagFlowLayout(context, px(R.dimen.bremo_space_s))

    init {
        binding.comment.onValueChange = { onCommentChange(it) }
        binding.submit.icon = R.drawable.ic_send
        binding.submit.onClick = { onSubmit() }
        pinAction(binding.dock)
        // Built once so typing in the comment keeps its focus; render only refills the stars, tags and provider card.
        binding.body.addView(donePill(), LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER_HORIZONTAL })
        binding.body.addView(providerHolder)
        binding.body.addView(card(Gravity.CENTER_HORIZONTAL).apply {
            addView(overallStars, wrap())
            addView(text(string(R.string.rating_overall_hint), R.style.TextAppearance_Bremo_Hint), wrap())
        })
        binding.body.addView(card().apply {
            addView(heading(string(R.string.rating_details), R.drawable.ic_tune))
            dimensions.forEachIndexed { dimension, label ->
                if (dimension > 0) addView(View(context).apply { setBackgroundResource(R.drawable.bremo_bg_divider) }, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, px(R.dimen.bremo_border_width)))
                addView(row().apply {
                    addView(text(string(label), R.style.TextAppearance_Bremo_Secondary), LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                    addView(detailStars[dimension])
                })
            }
        })
        binding.body.addView(card().apply {
            addView(row().apply {
                addView(text(string(R.string.rating_tags), R.style.TextAppearance_Bremo_HomeSection), LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                addView(text(string(R.string.rating_optional), R.style.TextAppearance_Bremo_Hint))
            })
            addView(tagFlow)
        })
        binding.body.addView(card().apply { addView(photoButton); addView(photoCount); addView(removePhoto) })
        binding.body.addView(card().apply {
            addView(text(string(R.string.rating_notes), R.style.TextAppearance_Bremo_HomeSection))
            (binding.comment.parent as ViewGroup).removeView(binding.comment)
            addView(binding.comment)
        })
    }

    override fun title(state: CustomerUiState) = string(R.string.rating_title)

    override fun renderContent(state: CustomerUiState) {
        photoButton.isVisible = "rate" in state.visibleActions
        photoButton.enabled = !state.isBusy && state.photoCount < 5
        photoCount.isVisible = state.photoCount > 0
        photoCount.text = String.format(string(R.string.rating_photos_count), state.photoCount.toString())
        removePhoto.isVisible = state.photoCount > 0 && "rate" in state.visibleActions
        removePhoto.isEnabled = !state.isBusy
        val name = state.fieldValues.getOrElse(1) { "" }
        val ratings = List(3) { state.ratings.getOrElse(it) { 0 } }
        providerHolder.removeAllViews()
        providerCard(providerHolder, name, state.fieldValues.getOrElse(2) { "" }, state.providerVerified)
        // Figma's overall row: the shared value of the three dimensions, or empty while they differ.
        fillStars(overallStars, ratings.distinct().singleOrNull() ?: 0, R.dimen.bremo_size_rating_star_large, string(R.string.rating_overall_hint), px(R.dimen.bremo_size_touch_target_min) + px(R.dimen.bremo_space_s)) { onOverall(it) }
        dimensions.forEachIndexed { dimension, label ->
            fillStars(detailStars[dimension], ratings[dimension], R.dimen.bremo_size_rating_star, string(label)) { onRate(dimension, it) }
        }
        tagFlow.removeAllViews()
        state.options.forEachIndexed { index, label ->
            val code = state.optionCodes.getOrElse(index) { "" }
            tagFlow.addView(tag(label, R.drawable.ic_star, code in state.items) { onTag(index, code) })
        }
        val error = state.fieldErrors.firstOrNull { it == "rating.validation.comment_max" }?.let { string(R.string.rating_validation_comment_max) }
        binding.comment.value = state.fieldValues.firstOrNull().orEmpty()
        binding.comment.state = if (error == null) FieldVisualState.Filled else FieldVisualState.Error
        binding.comment.error = error
        val key = state.messageKey
        binding.success.isVisible = key == "rating.success"
        binding.warning.isVisible = key != null && key != "rating.success"
        binding.warning.isError = key in setOf("media.upload_error", "error.body")
        if (key != null && key != "rating.success") {
            binding.warning.text = string(if (key == "media.upload_error") R.string.media_upload_error else if (key == "rating.already_exists") R.string.rating_already_exists else if (key == "error.body") R.string.error_body else R.string.rating_window_closed)
        }
        binding.submit.isVisible = "rate" in state.visibleActions
        binding.afterSubmit.isVisible = binding.submit.isVisible
        binding.submit.state = if (state.isBusy) ButtonVisualState.Loading else continueState(state)
        // Figma shows no validation text; screen readers hear why «إرسال التقييم» is disabled.
        binding.submit.contentDescription = listOfNotNull(
            string(R.string.rating_submit),
            string(R.string.rating_validation_required).takeIf { "rating.validation.required" in state.fieldErrors },
        ).joinToString(string(R.string.format_list_separator))
    }

    private fun donePill() = text(string(R.string.rating_visit_done), R.style.TextAppearance_Bremo_StatusPill).apply {
        setBackgroundResource(R.drawable.bremo_bg_badge_success)
        setTextColor(context.getColor(R.color.bremo_success))
        setPaddingRelative(px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_xs), px(R.dimen.bremo_space_m), px(R.dimen.bremo_space_xs))
        setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_verified, 0, 0, 0)
        TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(context.getColor(R.color.bremo_success)))
        compoundDrawablePadding = px(R.dimen.bremo_space_xs)
        sizeCompoundIcons(R.dimen.bremo_size_icon_small)
    }

    private fun providerCard(holder: LinearLayout, name: String, title: String, verified: Boolean) = holder.apply {
        // BACKEND-NOTES B-16: the app loads no remote photos, so the avatar shows the first letter.
        addView(FrameLayout(context).apply {
            addView(text(name.trim().take(1), R.style.TextAppearance_Bremo_ProfileName).apply {
                gravity = Gravity.CENTER
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setTextColor(context.getColor(R.color.bremo_primary600))
                setBackgroundResource(R.drawable.bremo_bg_avatar_large)
            }, FrameLayout.LayoutParams(px(R.dimen.bremo_size_avatar_large), px(R.dimen.bremo_size_avatar_large)))
            if (verified) addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_check)
                setBackgroundResource(R.drawable.bremo_bg_verified_badge)
                ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(context.getColor(R.color.bremo_on_primary)))
                val inset = px(R.dimen.bremo_space_xs)
                setPadding(inset, inset, inset, inset)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, FrameLayout.LayoutParams(px(R.dimen.bremo_size_verified_badge), px(R.dimen.bremo_size_verified_badge), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))
        }, wrap())
        if (name.isNotEmpty()) addView(text(name, R.style.TextAppearance_Bremo_OfferName), wrap())
        if (title.isNotEmpty()) addView(text(title, R.style.TextAppearance_Bremo_Hint).apply {
            setBackgroundResource(R.drawable.bremo_bg_info_pill)
            setPaddingRelative(px(R.dimen.bremo_space_s), px(R.dimen.bremo_space_xs), px(R.dimen.bremo_space_s), px(R.dimen.bremo_space_xs))
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_wrench, 0, 0, 0)
            TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(context.getColor(R.color.bremo_success)))
            compoundDrawablePadding = px(R.dimen.bremo_space_xs)
            sizeCompoundIcons(R.dimen.bremo_size_icon_xs)
        }, wrap())
        val question = if (name.isEmpty()) string(R.string.rating_heading)
        else BzrFormat.fill(string(R.string.rating_question), "name" to name.substringBefore(' '))
        addView(text(question, R.style.TextAppearance_Bremo_ScreenTitle).apply { textAlignment = View.TEXT_ALIGNMENT_CENTER }, wrap())
    }

    /** Five stars; each one is a 48-high touch target around the Figma glyph. */
    private fun fillStars(
        row: LinearLayout, selected: Int, size: Int, label: String,
        cell: Int = px(R.dimen.bremo_size_touch_target_min) * 3 / 4, onPick: (Int) -> Unit,
    ) = row.apply {
        removeAllViews()
        repeat(MAX_STARS) { index ->
            addView(FrameLayout(context).apply {
                isClickable = true
                isFocusable = true
                contentDescription = "$label ${index + 1}"
                setOnClickListener { onPick(index + 1) }
                addView(ImageView(context).apply {
                    setImageResource(if (index < selected) R.drawable.ic_star_filled else R.drawable.ic_star)
                    ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(context.getColor(if (index < selected) R.color.bremo_star else R.color.bremo_star_empty)))
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, FrameLayout.LayoutParams(px(size), px(size), Gravity.CENTER))
            }, LinearLayout.LayoutParams(maxOf(px(size), cell), px(R.dimen.bremo_size_touch_target_min)))
        }
    }

    private fun tag(label: String, @DrawableRes icon: Int, selected: Boolean, onClick: () -> Unit) = text(label, R.style.TextAppearance_Bremo_Caption).apply {
        setTextColor(context.getColor(if (selected) R.color.bremo_chip_selected_text else R.color.bremo_chip_text))
        val inset = (px(R.dimen.bremo_size_touch_target_min) - px(R.dimen.bremo_size_chip_height)) / 2
        background = InsetDrawable(ContextCompat.getDrawable(context, if (selected) R.drawable.bremo_bg_tag_selected else R.drawable.bremo_bg_tag_unselected), 0, inset, 0, inset)
        gravity = Gravity.CENTER_VERTICAL
        minHeight = px(R.dimen.bremo_size_touch_target_min)
        setPaddingRelative(px(R.dimen.bremo_space_m), 0, px(R.dimen.bremo_space_m), 0)
        setCompoundDrawablesRelativeWithIntrinsicBounds(if (selected) R.drawable.ic_check else icon, 0, 0, 0)
        TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(context.getColor(if (selected) R.color.bremo_chip_selected_text else R.color.bremo_chip_text)))
        compoundDrawablePadding = px(R.dimen.bremo_space_xs)
        sizeCompoundIcons(R.dimen.bremo_size_icon_small)
        isSelected = selected
        isClickable = true
        isFocusable = true
        setOnClickListener { onClick() }
    }

    private fun heading(value: String, @DrawableRes icon: Int) = text(value, R.style.TextAppearance_Bremo_HomeSection).apply {
        setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
        TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(context.getColor(R.color.bremo_success)))
        compoundDrawablePadding = px(R.dimen.bremo_space_s)
        sizeCompoundIcons(R.dimen.bremo_size_icon)
    }

    private fun card(gravity: Int = Gravity.NO_GRAVITY) = BremoLinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        this.gravity = gravity
        setBackgroundResource(R.drawable.bremo_bg_card)
        cardShadow()
        dividerDrawable = ContextCompat.getDrawable(context, R.drawable.bremo_gap_s)
        showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        setPadding(px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l), px(R.dimen.bremo_space_l))
    }

    private fun wrap() = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

    private fun row() = BremoLinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun text(value: String, @StyleRes style: Int) = TextView(context).apply {
        text = value
        TextViewCompat.setTextAppearance(this, style)
        textAlignment = View.TEXT_ALIGNMENT_VIEW_START
    }

    private fun px(id: Int) = resources.getDimensionPixelSize(id)

    private companion object {
        const val MAX_STARS = 5
    }
}

/** Wraps the quick tags onto new lines from the start edge, mirrored for RTL. */
private class TagFlowLayout(context: Context, private val gap: Int) : ViewGroup(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        var x = 0
        var y = 0
        var line = 0
        children().forEach { child ->
            child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
            if (x > 0 && x + child.measuredWidth > width) { x = 0; y += line + gap; line = 0 }
            x += child.measuredWidth + gap
            line = maxOf(line, child.measuredHeight)
        }
        setMeasuredDimension(width, y + line)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val rtl = layoutDirection == View.LAYOUT_DIRECTION_RTL
        var x = 0
        var y = 0
        var line = 0
        children().forEach { child ->
            if (x > 0 && x + child.measuredWidth > width) { x = 0; y += line + gap; line = 0 }
            val left = if (rtl) width - x - child.measuredWidth else x
            child.layout(left, y, left + child.measuredWidth, y + child.measuredHeight)
            x += child.measuredWidth + gap
            line = maxOf(line, child.measuredHeight)
        }
    }

    private fun children() = (0 until childCount).map(::getChildAt)
}
