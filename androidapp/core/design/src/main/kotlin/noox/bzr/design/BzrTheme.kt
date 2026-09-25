package noox.bzr.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import noox.bzr.design.generated.BzrTextStyle
import noox.bzr.design.generated.DesignA11y
import noox.bzr.design.generated.DesignColors
import noox.bzr.design.generated.DesignType

/** One static file per weight (43 §2), so every weight renders exactly as on iOS. */
val Cairo = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_medium, FontWeight.Medium),
    Font(R.font.cairo_semibold, FontWeight.SemiBold),
    Font(R.font.cairo_bold, FontWeight.Bold),
)

fun BzrTextStyle.toTextStyle(): TextStyle = TextStyle(
    color = color,
    fontFamily = Cairo,
    fontWeight = weight,
    fontSize = size,
)

fun BzrTextStyle.colored(newColor: androidx.compose.ui.graphics.Color): BzrTextStyle = copy(color = newColor)

/** Every Material slot maps to a token so nothing falls back to the system font. */
private val BzrTypography = Typography(
    displayLarge = DesignType.displayNumber.toTextStyle(),
    displayMedium = DesignType.displayNumber.toTextStyle(),
    displaySmall = DesignType.displayNumber.toTextStyle(),
    headlineLarge = DesignType.screenTitle.toTextStyle(),
    headlineMedium = DesignType.screenTitle.toTextStyle(),
    headlineSmall = DesignType.screenTitle.toTextStyle(),
    titleLarge = DesignType.sectionTitle.toTextStyle(),
    titleMedium = DesignType.cardTitle.toTextStyle(),
    titleSmall = DesignType.body.toTextStyle(),
    bodyLarge = DesignType.body.toTextStyle(),
    bodyMedium = DesignType.secondary.toTextStyle(),
    bodySmall = DesignType.caption.toTextStyle(),
    labelLarge = DesignType.button.toTextStyle(),
    labelMedium = DesignType.caption.toTextStyle(),
    labelSmall = DesignType.badge.toTextStyle(),
)

private val BzrColorScheme = lightColorScheme(
    primary = DesignColors.primary600,
    onPrimary = DesignColors.onPrimary,
    primaryContainer = DesignColors.primary50,
    onPrimaryContainer = DesignColors.navy900,
    background = DesignColors.surface,
    onBackground = DesignColors.navy800,
    surface = DesignColors.surface,
    onSurface = DesignColors.navy800,
    surfaceVariant = DesignColors.surfaceAlt,
    onSurfaceVariant = DesignColors.slate500,
    outline = DesignColors.border,
    error = DesignColors.danger,
    onError = DesignColors.onPrimary,
)

/** RTL always, light always (43 §2), and font scale capped at DesignA11y.maxFontScale. */
@Composable
fun BzrTheme(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val cappedDensity = Density(
        density = density.density,
        fontScale = minOf(density.fontScale, DesignA11y.maxFontScale),
    )

    CompositionLocalProvider(
        LocalDensity provides cappedDensity,
        LocalLayoutDirection provides LayoutDirection.Rtl,
    ) {
        MaterialTheme(
            colorScheme = BzrColorScheme,
            typography = BzrTypography,
            content = content,
        )
    }
}
