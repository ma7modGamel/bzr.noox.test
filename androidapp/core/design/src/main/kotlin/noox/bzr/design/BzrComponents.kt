@file:Suppress("LongMethod", "LongParameterList", "TooManyFunctions")

package noox.bzr.design

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import noox.bzr.design.generated.BzrTextStyle
import noox.bzr.design.generated.DesignBorder
import noox.bzr.design.generated.DesignColors
import noox.bzr.design.generated.DesignOpacity
import noox.bzr.design.generated.DesignRadius
import noox.bzr.design.generated.DesignRatio
import noox.bzr.design.generated.DesignSize
import noox.bzr.design.generated.DesignSpace
import noox.bzr.design.generated.DesignThreshold
import noox.bzr.design.generated.DesignType

// Mirror of BzrComponents.swift: same names, same parameters, same states (43 §3).

val AvatarSize.dp: Dp
    get() = when (this) {
        AvatarSize.Small -> DesignSize.avatarSmall
        AvatarSize.Medium -> DesignSize.avatarMedium
        AvatarSize.Large -> DesignSize.avatarLarge
    }

// ── Foundation ───────────────────────────────────────────────────────────

@Composable
fun BzrText(
    text: String,
    style: BzrTextStyle = DesignType.body,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        style = style.toTextStyle(),
        modifier = modifier,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun BzrIcon(
    @DrawableRes icon: Int,
    description: String?,
    tint: Color = DesignColors.navy800,
    size: Dp = DesignSize.icon,
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = description,
        tint = tint,
        modifier = Modifier.size(size),
    )
}

@Composable
fun BzrCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignRadius.card))
            .background(DesignColors.surface)
            .border(DesignBorder.width, DesignColors.border, RoundedCornerShape(DesignRadius.card))
            .padding(DesignSpace.cardPadding),
        verticalArrangement = Arrangement.spacedBy(DesignSpace.m),
        content = content,
    )
}

@Composable
private fun HorizontalLine() {
    Box(modifier = Modifier.fillMaxWidth().height(DesignBorder.width).background(DesignColors.border))
}

@Composable
private fun VerticalLine(height: Dp) {
    Box(modifier = Modifier.width(DesignBorder.width).height(height).background(DesignColors.border))
}

/** Deterministic arc instead of an animated spinner, so snapshots always show the loading state. */
@Composable
private fun ProgressArc(color: Color, label: String) {
    Canvas(modifier = Modifier.size(DesignSize.icon).semantics { contentDescription = label }) {
        val stroke = DesignSize.progressStroke.toPx()
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * DesignRatio.progressArc,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
            size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

private fun Modifier.selectionFrame(state: SelectionState, radius: Dp): Modifier {
    val shape = RoundedCornerShape(radius)
    val selected = state == SelectionState.Selected

    return this
        .alpha(if (state == SelectionState.Disabled) DesignOpacity.disabled else 1f)
        .clip(shape)
        .background(if (selected) DesignBorder.selectedFill else DesignColors.surface)
        .border(
            if (selected) DesignBorder.selectedWidth else DesignBorder.width,
            if (selected) DesignBorder.selectedColor else DesignColors.border,
            shape,
        )
}

// ── Actions & navigation ─────────────────────────────────────────────────

@Composable
fun AppTopBar(title: String, @DrawableRes actionIcon: Int? = null, actionLabel: String? = null, onBack: () -> Unit = {}, onAction: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(DesignSize.topBarHeight)
            .background(DesignColors.surface)
            .padding(horizontal = DesignSpace.screenHorizontal),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.align(Alignment.CenterEnd).size(DesignSize.menuIconBox).clickable(onClick = onBack),
        ) {
            BzrIcon(R.drawable.ic_arrow_back, stringResource(R.string.a11y_back))
        }
        BzrText(title, DesignType.screenTitle, Modifier.align(Alignment.Center), maxLines = 1)
        actionIcon?.let {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.align(Alignment.CenterStart).size(DesignSize.menuIconBox).clickable(onClick = onAction),
            ) {
                BzrIcon(it, actionLabel)
            }
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    state: ButtonVisualState = ButtonVisualState.Normal,
    modifier: Modifier = Modifier,
    height: Dp = DesignSize.primaryButtonHeight,
    onClick: () -> Unit = {},
) {
    val interactive = state == ButtonVisualState.Normal || state == ButtonVisualState.Pressed
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .alpha(if (state == ButtonVisualState.Disabled) DesignOpacity.disabled else 1f)
            .clip(RoundedCornerShape(DesignRadius.button))
            .background(if (state == ButtonVisualState.Pressed) DesignColors.primary700 else DesignColors.primary600)
            .clickable(enabled = interactive, onClick = onClick),
    ) {
        if (state == ButtonVisualState.Loading) {
            ProgressArc(DesignColors.onPrimary, stringResource(R.string.a11y_loading))
        } else {
            BzrText(text, DesignType.button, maxLines = 1)
        }
    }
}

@Composable
fun SecondaryButton(text: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(DesignSize.secondaryButtonHeight)
            .alpha(if (enabled) 1f else DesignOpacity.disabled)
            .clip(RoundedCornerShape(DesignRadius.button))
            .background(DesignColors.surface)
            .border(DesignBorder.width, DesignColors.primary600, RoundedCornerShape(DesignRadius.button))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        BzrText(text, DesignType.button.colored(DesignColors.primary700), maxLines = 1)
    }
}

@Composable
fun DangerTextButton(text: String, onClick: () -> Unit = {}) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(DesignSize.secondaryButtonHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = DesignSpace.m),
    ) {
        BzrText(text, DesignType.body.colored(DesignColors.danger))
    }
}

@Composable
fun LinkButton(text: String, onClick: () -> Unit = {}) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(DesignSize.secondaryButtonHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = DesignSpace.m),
    ) {
        BzrText(text, DesignType.body.colored(DesignColors.primary700))
    }
}

@Composable
fun IconSquareButton(label: String, @DrawableRes icon: Int = R.drawable.ic_chat, onClick: () -> Unit = {}) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(DesignSize.chatSquareButtonW)
            .height(DesignSize.chatSquareButtonH)
            .clip(RoundedCornerShape(DesignRadius.button))
            .background(DesignColors.surface)
            .border(DesignBorder.width, DesignColors.primary600, RoundedCornerShape(DesignRadius.button))
            .clickable(onClick = onClick),
    ) {
        BzrIcon(icon, label, DesignColors.primary600)
    }
}

@Composable
fun BottomNav(items: List<Pair<Int, String>>, selectedIndex: Int, onSelect: (Int) -> Unit = {}) {
    Column(modifier = Modifier.fillMaxWidth().background(DesignColors.surface)) {
        HorizontalLine()
        Row(modifier = Modifier.fillMaxWidth().height(DesignSize.bottomNavHeight), verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { index, item ->
                val color = if (index == selectedIndex) DesignColors.primary500 else DesignColors.slate400
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(DesignSpace.xs),
                    modifier = Modifier.weight(1f).clickable { onSelect(index) },
                ) {
                    BzrIcon(item.first, null, color)
                    BzrText(item.second, DesignType.caption.colored(color), maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun MenuRow(@DrawableRes icon: Int, text: String, onClick: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignSpace.m),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = DesignSpace.s),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(DesignSize.menuIconBox).clip(RoundedCornerShape(DesignRadius.menuIconBox)).background(DesignColors.surfaceAlt),
        ) {
            BzrIcon(icon, null)
        }
        BzrText(text, DesignType.body, Modifier.weight(1f))
        BzrIcon(R.drawable.ic_chevron, null, DesignColors.slate400)
    }
}

@Composable
fun DrawerMenu(name: String, rating: String, verifiedLabel: String, rows: List<Pair<Int, String>>, action: String, onAction: () -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignSpace.s)) {
        ProviderHeader(name, rating, null, AvatarSize.Large, verifiedLabel)
        Spacer(modifier = Modifier.height(DesignSpace.s))
        rows.forEach { MenuRow(it.first, it.second) }
        Spacer(modifier = Modifier.height(DesignSpace.s))
        PrimaryButton(action, onClick = onAction)
    }
}

// ── Selection & fields ───────────────────────────────────────────────────

@Composable
fun StepIndicator(current: Int, total: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(DesignSpace.s), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(DesignSize.stepIndicatorWidth)) {
            for (step in 1..total) {
                val state = when {
                    step < current -> StepState.Done
                    step == current -> StepState.Active
                    else -> StepState.Pending
                }
                StepCircle(step, state)
                if (step < total) {
                    Box(modifier = Modifier.weight(1f).height(DesignSize.stepperLine).background(if (step < current) DesignColors.primary600 else DesignColors.border))
                }
            }
        }
        BzrText(label, DesignType.secondary)
    }
}

@Composable
private fun StepCircle(step: Int, state: StepState) {
    val filled = state != StepState.Pending
    val filledColor = if (state == StepState.OnHold) DesignColors.star else DesignColors.primary600
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(DesignSize.stepCircle)
            .clip(CircleShape)
            .background(if (filled) filledColor else DesignColors.surface)
            .border(DesignSize.controlStroke, if (filled) filledColor else DesignColors.border, CircleShape),
    ) {
        when (state) {
            StepState.Done -> BzrIcon(R.drawable.ic_check, null, DesignColors.onPrimary, DesignSize.iconSmall)
            StepState.Active -> BzrText(BzrFormat.number(step), DesignType.caption.colored(DesignColors.onPrimary))
            StepState.OnHold -> BzrText(BzrFormat.number(step), DesignType.caption.colored(DesignColors.onPrimary))
            StepState.Pending -> BzrText(BzrFormat.number(step), DesignType.caption)
        }
    }
}

@Composable
fun SelectableTile(text: String, @DrawableRes icon: Int, state: SelectionState, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignSpace.s, Alignment.CenterVertically),
        modifier = modifier.height(DesignSize.categoryTileH).selectionFrame(state, DesignRadius.card).padding(DesignSpace.s),
    ) {
        BzrIcon(icon, null, if (state == SelectionState.Selected) DesignColors.primary600 else DesignColors.navy800, DesignSize.tileIcon)
        BzrText(text, DesignType.body, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
fun SelectableChip(text: String, state: SelectionState, modifier: Modifier = Modifier, height: Dp = DesignSize.chipHeight) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.height(height).selectionFrame(state, DesignRadius.chip).padding(horizontal = DesignSpace.m),
    ) {
        BzrText(text, DesignType.body, maxLines = 1)
    }
}

@Composable
fun RadioCard(title: String, body: String, selected: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignSpace.m),
        modifier = Modifier
            .fillMaxWidth()
            .selectionFrame(if (selected) SelectionState.Selected else SelectionState.Unselected, DesignRadius.card)
            .padding(DesignSpace.l),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(DesignSize.radio)
                .clip(CircleShape)
                .border(DesignSize.controlStroke, if (selected) DesignColors.primary600 else DesignColors.slate300, CircleShape),
        ) {
            if (selected) Box(modifier = Modifier.size(DesignSize.radioDot).clip(CircleShape).background(DesignColors.primary600))
        }
        Column(verticalArrangement = Arrangement.spacedBy(DesignSpace.xs)) {
            BzrText(title, DesignType.cardTitle)
            BzrText(body, DesignType.secondary)
        }
    }
}

@Composable
fun CheckRow(text: String, checked: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignSpace.m),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignRadius.field))
            .background(if (checked) DesignColors.primary50 else DesignColors.surface)
            .border(DesignBorder.width, DesignColors.border, RoundedCornerShape(DesignRadius.field))
            .padding(DesignSpace.m),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(DesignSize.checkbox)
                .clip(RoundedCornerShape(DesignRadius.checkbox))
                .background(if (checked) DesignColors.primary600 else DesignColors.surface)
                .border(DesignSize.controlStroke, if (checked) DesignColors.primary600 else DesignColors.slate300, RoundedCornerShape(DesignRadius.checkbox)),
        ) {
            if (checked) BzrIcon(R.drawable.ic_check, null, DesignColors.onPrimary, DesignSize.iconSmall)
        }
        BzrText(text, DesignType.body, Modifier.weight(1f))
    }
}

@Composable
private fun FieldShell(
    label: String,
    state: FieldVisualState,
    focused: Boolean,
    error: String?,
    optionalLabel: String?,
    height: Dp,
    alignment: Alignment.Vertical,
    padding: PaddingValues,
    content: @Composable RowScope.() -> Unit,
) {
    val effective = if (focused && state != FieldVisualState.Error && state != FieldVisualState.Disabled) FieldVisualState.Focused else state
    val borderWidth = if (effective == FieldVisualState.Error || effective == FieldVisualState.Focused) DesignBorder.selectedWidth else DesignBorder.width
    val borderColor = when (effective) {
        FieldVisualState.Error -> DesignColors.danger
        FieldVisualState.Focused -> DesignColors.primary600
        else -> DesignColors.border
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(DesignSpace.xs),
        modifier = Modifier.fillMaxWidth().alpha(if (state == FieldVisualState.Disabled) DesignOpacity.disabled else 1f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BzrText(label, DesignType.secondary, Modifier.weight(1f))
            optionalLabel?.let { BzrText(it, DesignType.caption) }
        }
        Row(
            verticalAlignment = alignment,
            horizontalArrangement = Arrangement.spacedBy(DesignSpace.s),
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(DesignRadius.field))
                .background(DesignColors.surface)
                .border(borderWidth, borderColor, RoundedCornerShape(DesignRadius.field))
                .padding(padding),
            content = content,
        )
        error?.let { BzrText(it, DesignType.caption.colored(DesignColors.danger)) }
    }
}

/** Static text when no handler is given (gallery / snapshots); a real input otherwise. */
@Composable
private fun FieldValue(
    value: String,
    placeholder: String,
    enabled: Boolean,
    singleLine: Boolean,
    interaction: MutableInteractionSource,
    onValueChange: ((String) -> Unit)?,
    modifier: Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    if (onValueChange == null) {
        val displayValue = visualTransformation.filter(androidx.compose.ui.text.AnnotatedString(value)).text.text
        BzrText(
            displayValue.ifEmpty { placeholder },
            if (value.isEmpty()) DesignType.placeholder else DesignType.body,
            modifier,
            maxLines = if (singleLine) 1 else Int.MAX_VALUE,
        )

        return
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = singleLine,
        textStyle = DesignType.body.toTextStyle(),
        interactionSource = interaction,
        visualTransformation = visualTransformation,
        modifier = modifier,
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) BzrText(placeholder, DesignType.placeholder)
                inner()
            }
        },
    )
}

@Composable
fun SecureTextField(
    label: String,
    placeholder: String,
    value: String,
    state: FieldVisualState,
    error: String? = null,
    optionalLabel: String? = null,
    onValueChange: ((String) -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    FieldShell(label, state, focused, error, optionalLabel, DesignSize.fieldHeight, Alignment.CenterVertically, PaddingValues(horizontal = DesignSpace.m)) {
        FieldValue(
            value,
            placeholder,
            state != FieldVisualState.Disabled,
            true,
            interaction,
            onValueChange,
            Modifier.weight(1f),
            PasswordVisualTransformation(),
        )
    }
}

@Composable
fun AppTextField(
    label: String,
    placeholder: String,
    value: String,
    state: FieldVisualState,
    error: String? = null,
    optionalLabel: String? = null,
    onValueChange: ((String) -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    FieldShell(label, state, focused, error, optionalLabel, DesignSize.fieldHeight, Alignment.CenterVertically, PaddingValues(horizontal = DesignSpace.m)) {
        FieldValue(value, placeholder, state != FieldVisualState.Disabled, true, interaction, onValueChange, Modifier.weight(1f))
    }
}

@Composable
fun AmountField(
    label: String,
    placeholder: String,
    value: String,
    currency: String,
    state: FieldVisualState,
    error: String? = null,
    optionalLabel: String? = null,
    onValueChange: ((String) -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    FieldShell(
        label, state, focused, error, optionalLabel, DesignSize.fieldHeight, Alignment.CenterVertically,
        PaddingValues(start = DesignSpace.m, end = DesignSpace.xs, top = DesignSpace.xs, bottom = DesignSpace.xs),
    ) {
        FieldValue(value, placeholder, state != FieldVisualState.Disabled, true, interaction, onValueChange, Modifier.weight(1f))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(DesignSize.currencyBoxWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(DesignRadius.badge))
                .background(DesignColors.surfaceAlt2),
        ) {
            BzrText(currency, DesignType.body)
        }
    }
}

@Composable
fun TextAreaField(
    label: String,
    placeholder: String,
    value: String,
    state: FieldVisualState,
    error: String? = null,
    optionalLabel: String? = null,
    onValueChange: ((String) -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    FieldShell(label, state, focused, error, optionalLabel, DesignSize.textAreaHeight, Alignment.Top, PaddingValues(DesignSpace.m)) {
        FieldValue(value, placeholder, state != FieldVisualState.Disabled, false, interaction, onValueChange, Modifier.weight(1f))
    }
}

// ── Cards & data ─────────────────────────────────────────────────────────

@Composable
fun SummaryCard(title: String, rows: List<Pair<Int, String>>, editText: String?, onEdit: () -> Unit = {}) {
    BzrCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BzrText(title, DesignType.cardTitle, Modifier.weight(1f))
            editText?.let {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(DesignRadius.badge))
                        .background(DesignColors.primary50)
                        .clickable(onClick = onEdit)
                        .padding(horizontal = DesignSpace.m, vertical = DesignSpace.xs),
                ) {
                    BzrText(it, DesignType.caption.colored(DesignColors.primary700))
                }
            }
        }
        rows.forEachIndexed { index, row ->
            if (index > 0) HorizontalLine()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignSpace.m)) {
                BzrIcon(row.first, null, DesignColors.primary600)
                BzrText(row.second, DesignType.body)
            }
        }
    }
}

@Composable
fun Badge(text: String, kind: BadgeKind = BadgeKind.Highlight, @DrawableRes icon: Int? = null) {
    val background = if (kind == BadgeKind.Highlight) DesignColors.badgeBg else DesignColors.primary50Available
    val foreground = if (kind == BadgeKind.Highlight) DesignColors.badgeText else DesignColors.primary700
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignSpace.xs),
        modifier = Modifier
            .clip(RoundedCornerShape(DesignRadius.badge))
            .background(background)
            .padding(horizontal = DesignSpace.s, vertical = DesignSpace.xs),
    ) {
        icon?.let { BzrIcon(it, null, DesignColors.star, DesignSize.iconSmall) }
        BzrText(text, DesignType.badge.colored(foreground), maxLines = 1)
    }
}

@Composable
private fun Avatar(size: Dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size).clip(CircleShape).background(DesignColors.surfaceAlt),
    ) {
        BzrIcon(R.drawable.ic_account, stringResource(R.string.a11y_avatar), DesignColors.slate400)
    }
}

@Composable
private fun VerifiedAvatar(size: Dp) {
    Box(modifier = Modifier.size(size)) {
        Avatar(size)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(DesignSize.verifiedShield)
                .clip(CircleShape)
                .background(DesignColors.primary500)
                .border(DesignSize.controlStroke, DesignColors.surface, CircleShape),
        ) {
            BzrIcon(R.drawable.ic_shield, stringResource(R.string.a11y_verified), DesignColors.onPrimary, DesignSize.iconSmall)
        }
    }
}

@Composable
private fun RatingLine(rating: String, services: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignSpace.s)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignSpace.xs)) {
            BzrIcon(R.drawable.ic_star_filled, null, DesignColors.star, DesignSize.iconSmall)
            BzrText(rating, DesignType.body, maxLines = 1)
        }
        services?.let { BzrText(it, DesignType.secondary, maxLines = 1) }
    }
}

@Composable
fun ProviderHeader(name: String, rating: String, services: String?, size: AvatarSize, verifiedLabel: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignSpace.m)) {
        VerifiedAvatar(size.dp)
        Column(verticalArrangement = Arrangement.spacedBy(DesignSpace.xs)) {
            BzrText(name, DesignType.cardTitle)
            verifiedLabel?.let {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignSpace.xs)) {
                    Box(modifier = Modifier.size(DesignSize.statusDot).clip(CircleShape).background(DesignColors.primary500))
                    BzrText(it, DesignType.secondary.colored(DesignColors.primary700))
                }
            }
            RatingLine(rating, services)
        }
    }
}

@Composable
fun OfferCard(
    provider: String,
    rating: String,
    services: String,
    price: String,
    detail: String,
    badge: String,
    button: String,
    chatLabel: String,
    variant: OfferVariant,
    priceCaption: String? = null,
    onSelect: () -> Unit = {},
    onChat: () -> Unit = {},
) {
    BzrCard {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(DesignSpace.m)) {
            VerifiedAvatar(DesignSize.avatarSmall)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DesignSpace.xs)) {
                BzrText(provider, DesignType.cardTitle)
                RatingLine(rating, services)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(DesignSpace.xs)) {
                Badge(badge, BadgeKind.Highlight, R.drawable.ic_star_filled)
                if (variant == OfferVariant.Inspection && priceCaption != null) BzrText(priceCaption, DesignType.caption)
                BzrText(price, DesignType.price)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignSpace.xs)) {
                    BzrIcon(if (variant == OfferVariant.Scheduled) R.drawable.ic_calendar else R.drawable.ic_clock, null, DesignColors.slate500, DesignSize.iconSmall)
                    BzrText(detail, DesignType.secondary)
                }
            }
        }
        HorizontalLine()
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignSpace.s)) {
            PrimaryButton(button, modifier = Modifier.weight(1f), height = DesignSize.compactButtonHeight, onClick = onSelect)
            IconSquareButton(chatLabel, onClick = onChat)
        }
    }
}

/** Scrolls horizontally when the chips do not fit the screen width (C06 at 400dp is already at the limit). */
@Composable
fun SortChips(title: String?, labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignSpace.s),
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    ) {
        title?.let { BzrText(it, DesignType.body, maxLines = 1) }
        labels.forEachIndexed { index, label ->
            SelectableChip(
                label,
                if (index == selectedIndex) SelectionState.Selected else SelectionState.Unselected,
                Modifier.clickable { onSelect(index) },
                DesignSize.sortChipHeight,
            )
        }
    }
}

@Composable
fun StatRow(items: List<StatItem>) {
    BzrCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { index, item ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(DesignSpace.xs),
                ) {
                    BzrIcon(item.icon, null, DesignColors.primary500)
                    BzrText(item.value, DesignType.cardTitle)
                    BzrText(item.label, DesignType.secondary, textAlign = TextAlign.Center, maxLines = 1)
                }
                if (index != items.lastIndex) VerticalLine(DesignSize.statDivider)
            }
        }
    }
}

@Composable
fun RatingBars(items: List<Pair<String, Double>>, max: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(DesignSpace.s)) {
        items.forEach { (label, value) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignSpace.m)) {
                BzrText(label, DesignType.secondary, Modifier.width(DesignSize.ratingLabelWidth), maxLines = 1)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(DesignSize.ratingBarHeight)
                        .clip(RoundedCornerShape(DesignRadius.full))
                        .background(DesignColors.surfaceAlt),
                ) {
                    Box(modifier = Modifier.fillMaxWidth((value / max).toFloat()).fillMaxHeight().clip(RoundedCornerShape(DesignRadius.full)).background(DesignColors.primary500))
                }
                BzrText(BzrFormat.number(value), DesignType.caption.colored(DesignColors.navy800))
            }
        }
    }
}

@Composable
private fun Stars(count: Int) {
    Row {
        repeat(count) { BzrIcon(R.drawable.ic_star_filled, null, DesignColors.star, DesignSize.iconSmall) }
    }
}

@Composable
fun ReviewCard(name: String, body: String, verified: String, time: String, tag: String?, rating: Int) {
    BzrCard {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(DesignSpace.m)) {
            Avatar(DesignSize.avatarXs)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DesignSpace.xs)) {
                BzrText(name, DesignType.body.let { BzrTextStyle(it.size, FontWeight.Bold, DesignColors.navy900) })
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignSpace.s)) {
                    Stars(rating)
                    BzrIcon(R.drawable.ic_check, null, DesignColors.primary600, DesignSize.iconSmall)
                    BzrText(verified, DesignType.caption.colored(DesignColors.primary700))
                }
            }
            BzrText(time, DesignType.caption)
        }
        BzrText(body, DesignType.body)
        tag?.let {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(DesignRadius.badge))
                    .background(DesignColors.surfaceAlt)
                    .padding(horizontal = DesignSpace.s, vertical = DesignSpace.xs),
            ) {
                BzrText(it, DesignType.caption)
            }
        }
    }
}

@Composable
fun StickyActionBar(priceLabel: String, price: String, action: String, chatLabel: String, onAction: () -> Unit = {}, onChat: () -> Unit = {}) {
    Column(modifier = Modifier.fillMaxWidth().background(DesignColors.surface)) {
        HorizontalLine()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignSpace.m),
            modifier = Modifier.fillMaxWidth().padding(vertical = DesignSpace.m),
        ) {
            Column {
                BzrText(priceLabel, DesignType.caption)
                BzrText(price, DesignType.price, maxLines = 1)
            }
            PrimaryButton(action, modifier = Modifier.weight(1f), onClick = onAction)
            IconSquareButton(chatLabel, onClick = onChat)
        }
    }
}

// ── Status & feedback ────────────────────────────────────────────────────

@Composable
private fun Banner(text: String, @DrawableRes icon: Int, iconTint: Color, background: Color, foreground: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignSpace.m),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(DesignRadius.card)).background(background).padding(DesignSpace.l),
    ) {
        BzrIcon(icon, null, iconTint)
        BzrText(text, DesignType.body.colored(foreground), Modifier.weight(1f))
    }
}

@Composable
fun InfoBanner(text: String) = Banner(text, R.drawable.ic_shield, DesignColors.primary600, DesignColors.primary50Info, DesignColors.primary700)

@Composable
fun WarningBox(text: String) = Banner(text, R.drawable.ic_warning_filled, DesignColors.star, DesignColors.warningBg, DesignColors.warningText)

@Composable
fun OfflineBanner(text: String) = Banner(text, R.drawable.ic_offline, DesignColors.navy800, DesignColors.surfaceAlt, DesignColors.navy800)

@Composable
fun EtaCard(value: String, unit: String, title: String, subtitle: String) {
    BzrCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignSpace.m)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(DesignSpace.xs)) {
                BzrText(value, DesignType.displayNumber, maxLines = 1)
                BzrText(unit, DesignType.cardTitle.colored(DesignColors.primary700), maxLines = 1)
            }
            VerticalLine(DesignSize.statDivider)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DesignSpace.xs)) {
                BzrText(title, DesignType.cardTitle, maxLines = 1)
                BzrText(subtitle, DesignType.secondary, maxLines = 1)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(DesignSize.etaIconCircle).clip(CircleShape).background(DesignColors.primary50Info),
            ) {
                BzrIcon(R.drawable.ic_car, null, DesignColors.primary600)
            }
        }
    }
}

/** Placeholder frame for the map SDK view (Google Maps / MapKit are platform views, 43 §7). */
@Composable
fun MapCard(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(DesignSize.mapCardHeight)
            .clip(RoundedCornerShape(DesignRadius.card))
            .background(DesignColors.surfaceAlt2)
            .border(DesignBorder.width, DesignColors.border, RoundedCornerShape(DesignRadius.card)),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DesignSpace.s),
            modifier = Modifier.align(Alignment.Center),
        ) {
            BzrIcon(R.drawable.ic_location, null, DesignColors.primary600)
            BzrText(title, DesignType.secondary)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(DesignSpace.m)
                .size(DesignSize.myLocationButton)
                .clip(RoundedCornerShape(DesignRadius.button))
                .background(DesignColors.surface)
                .border(DesignBorder.width, DesignColors.border, RoundedCornerShape(DesignRadius.button)),
        ) {
            BzrIcon(R.drawable.ic_navigation, stringResource(R.string.a11y_my_location))
        }
    }
}

@Composable
private fun StepperDot(state: StepState) {
    when (state) {
        StepState.Done -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(DesignSize.stepperDot).clip(CircleShape).background(DesignColors.primary600),
        ) {
            BzrIcon(R.drawable.ic_check, null, DesignColors.onPrimary, DesignSize.iconXs)
        }
        StepState.Active -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(DesignSize.stepperDotActive)
                .clip(CircleShape)
                .background(DesignColors.surface)
                .border(DesignSize.controlStroke, DesignColors.primary600, CircleShape),
        ) {
            Box(modifier = Modifier.size(DesignSize.radioDot).clip(CircleShape).background(DesignColors.primary600))
        }
        StepState.OnHold -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(DesignSize.stepperDotActive)
                .clip(CircleShape)
                .background(DesignColors.surface)
                .border(DesignSize.controlStroke, DesignColors.star, CircleShape),
        ) {
            Box(modifier = Modifier.size(DesignSize.radioDot).clip(CircleShape).background(DesignColors.star))
        }
        StepState.Pending -> Box(
            modifier = Modifier
                .size(DesignSize.stepperDot)
                .clip(CircleShape)
                .background(DesignColors.surface)
                .border(DesignSize.controlStroke, DesignColors.border, CircleShape),
        )
    }
}

/** Horizontal stepper as in C09: the line toward the previous step is teal once this step is reached. */
@Composable
fun StatusStepper(steps: List<Pair<String, StepState>>) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        steps.forEachIndexed { index, (label, state) ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DesignSpace.s),
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(DesignSize.stepperDotActive), contentAlignment = Alignment.Center) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.weight(1f).height(DesignSize.stepperLine)
                                .background(if (index == 0) Color.Transparent else if (state != StepState.Pending) DesignColors.primary600 else DesignColors.border),
                        )
                        Box(
                            modifier = Modifier.weight(1f).height(DesignSize.stepperLine)
                                .background(if (index == steps.lastIndex) Color.Transparent else if (steps[index + 1].second != StepState.Pending) DesignColors.primary600 else DesignColors.border),
                        )
                    }
                    StepperDot(state)
                }
                val style = when (state) {
                    StepState.Active -> BzrTextStyle(DesignType.caption.size, FontWeight.Bold, DesignColors.navy900)
                    StepState.OnHold -> BzrTextStyle(DesignType.caption.size, FontWeight.Bold, DesignColors.star)
                    StepState.Done -> DesignType.caption.colored(DesignColors.navy800)
                    StepState.Pending -> DesignType.caption
                }
                BzrText(label, style, textAlign = TextAlign.Center, maxLines = 2)
            }
        }
    }
}

@Composable
fun OrderCard(title: String, subtitle: String, status: String, onClick: () -> Unit = {}) {
    BzrCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignSpace.m)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DesignSpace.xs)) {
                BzrText(title, DesignType.cardTitle)
                BzrText(subtitle, DesignType.secondary)
                Badge(status, BadgeKind.Status)
            }
            BzrIcon(R.drawable.ic_chevron, null, DesignColors.slate400)
        }
    }
}

@Composable
fun AppBottomSheet(
    title: String,
    body: String,
    action: String,
    actionState: ButtonVisualState = ButtonVisualState.Normal,
    secondaryAction: String? = null,
    onAction: () -> Unit = {},
    onSecondary: () -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(DesignSpace.m),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = DesignRadius.sheetTop, topEnd = DesignRadius.sheetTop))
            .background(DesignColors.surface)
            .padding(horizontal = DesignSpace.screenHorizontal, vertical = DesignSpace.l),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(DesignSize.sheetHandleW)
                .height(DesignSize.sheetHandleH)
                .clip(RoundedCornerShape(DesignRadius.full))
                .background(DesignColors.border),
        )
        BzrText(title, DesignType.sectionTitle)
        BzrText(body, DesignType.body)
        PrimaryButton(action, actionState, onClick = onAction)
        secondaryAction?.let { SecondaryButton(it, onClick = onSecondary) }
    }
}

@Composable
private fun FeedbackState(
    @DrawableRes icon: Int,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    body: String,
    action: (@Composable () -> Unit)?,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DesignSpace.s),
        modifier = Modifier.fillMaxWidth().padding(DesignSpace.l),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(DesignSize.avatarSmall).clip(CircleShape).background(iconBackground),
        ) {
            BzrIcon(icon, null, iconTint)
        }
        BzrText(title, DesignType.cardTitle, textAlign = TextAlign.Center)
        BzrText(body, DesignType.secondary, textAlign = TextAlign.Center)
        action?.invoke()
    }
}

@Composable
fun EmptyState(title: String, body: String, action: String? = null, onAction: () -> Unit = {}) =
    FeedbackState(
        R.drawable.ic_empty, DesignColors.primary600, DesignColors.primary50, title, body,
        action?.let { { PrimaryButton(it, onClick = onAction) } },
    )

@Composable
fun ErrorState(title: String, body: String, retry: String? = null, onRetry: () -> Unit = {}) =
    FeedbackState(
        R.drawable.ic_error, DesignColors.danger, DesignColors.surfaceAlt, title, body,
        retry?.let { { SecondaryButton(it, onClick = onRetry) } },
    )

@Composable
fun LoadingSkeleton() {
    val label = stringResource(R.string.a11y_loading)
    BzrCard(modifier = Modifier.semantics { contentDescription = label }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignSpace.m)) {
            Box(modifier = Modifier.size(DesignSize.avatarSmall).clip(CircleShape).background(DesignColors.surfaceAlt))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DesignSpace.s)) {
                SkeletonLine(1f)
                SkeletonLine(DesignRatio.skeletonShortLine)
            }
        }
    }
}

@Composable
private fun SkeletonLine(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .height(DesignSize.skeletonLine)
            .clip(RoundedCornerShape(DesignRadius.badge))
            .background(DesignColors.surfaceAlt),
    )
}

/** Turns `danger` below DesignThreshold.countdownUrgentSeconds (43 §3: "أقل من 5 دقائق"). */
@Composable
fun Countdown(seconds: Int, label: String) {
    val urgent = seconds < DesignThreshold.countdownUrgentSeconds
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BzrText(BzrFormat.countdown(seconds), DesignType.displayNumber.colored(if (urgent) DesignColors.danger else DesignColors.primary700))
        BzrText(label, DesignType.secondary)
    }
}

// ── Content ──────────────────────────────────────────────────────────────

@Composable
fun ChatBubble(text: String, sent: Boolean, kind: ChatKind = ChatKind.Text) {
    val blocked = kind == ChatKind.Blocked
    val background = if (sent && !blocked) DesignColors.primary600 else DesignColors.surface
    val borderColor = when {
        blocked -> DesignColors.danger
        sent -> DesignColors.primary600
        else -> DesignColors.border
    }
    val foreground = when {
        blocked -> DesignColors.danger
        sent -> DesignColors.onPrimary
        else -> DesignColors.navy800
    }
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (sent) Alignment.CenterStart else Alignment.CenterEnd) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignSpace.s),
            modifier = Modifier
                .widthIn(max = DesignSize.chatBubbleMaxWidth)
                .clip(RoundedCornerShape(DesignRadius.card))
                .background(background)
                .border(DesignBorder.width, borderColor, RoundedCornerShape(DesignRadius.card))
                .padding(DesignSpace.m),
        ) {
            when (kind) {
                ChatKind.Image -> BzrIcon(R.drawable.ic_image, null, foreground)
                ChatKind.Blocked -> BzrIcon(R.drawable.ic_warning, null, foreground)
                ChatKind.Text -> Unit
            }
            BzrText(text, DesignType.body.colored(foreground))
        }
    }
}

@Composable
fun ChatInput(placeholder: String, value: String = "", onValueChange: ((String) -> Unit)? = null, onSend: () -> Unit = {}) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DesignSpace.s),
        modifier = Modifier
            .fillMaxWidth()
            .height(DesignSize.fieldHeight)
            .clip(RoundedCornerShape(DesignRadius.field))
            .background(DesignColors.surface)
            .border(DesignBorder.width, DesignColors.border, RoundedCornerShape(DesignRadius.field))
            .padding(horizontal = DesignSpace.m),
    ) {
        FieldValue(value, placeholder, true, true, interaction, onValueChange, Modifier.weight(1f))
        Box(modifier = Modifier.clickable(onClick = onSend)) {
            BzrIcon(R.drawable.ic_send, stringResource(R.string.a11y_send), DesignColors.primary600)
        }
    }
}

@Composable
fun MediaThumb(title: String, stateLabel: String, kind: MediaKind, state: MediaState, onDelete: () -> Unit = {}) {
    BzrCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignSpace.m)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(DesignSize.menuIconBox).clip(RoundedCornerShape(DesignRadius.menuIconBox)).background(DesignColors.surfaceAlt),
            ) {
                BzrIcon(
                    when (kind) {
                        MediaKind.Photo -> R.drawable.ic_image
                        MediaKind.Video -> R.drawable.ic_video
                        MediaKind.Audio -> R.drawable.ic_mic
                    },
                    null,
                    DesignColors.primary600,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DesignSpace.xs)) {
                BzrText(title, DesignType.body)
                BzrText(stateLabel, DesignType.caption.colored(if (state == MediaState.Failed) DesignColors.danger else DesignColors.slate500))
            }
            when (state) {
                MediaState.Uploading -> ProgressArc(DesignColors.primary600, stateLabel)
                MediaState.Uploaded -> BzrIcon(R.drawable.ic_check, stateLabel, DesignColors.primary600)
                MediaState.Failed -> BzrIcon(R.drawable.ic_error, stateLabel, DesignColors.danger)
            }
            Box(modifier = Modifier.clickable(onClick = onDelete)) {
                BzrIcon(R.drawable.ic_trash, stringResource(R.string.a11y_delete), DesignColors.danger)
            }
        }
    }
}
