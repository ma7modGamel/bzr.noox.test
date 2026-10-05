package noox.bzr.customer

import android.content.pm.ApplicationInfo
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.detectEnvironment
import com.android.resources.Density
import com.android.resources.LayoutDirection
import noox.bzr.auth.AuthLogic
import noox.bzr.auth.AuthPhase
import noox.bzr.auth.authScreenView
import noox.bzr.design.views.SnapshotMode
import noox.bzr.provider.ProviderLogic
import noox.bzr.provider.ProviderPhase
import noox.bzr.provider.providerScreenView
import noox.bzr.screens.ScreenSnapshots
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Every customer and technician screen is laid out in both directions, with narrow and large-text profiles. */
@RunWith(Parameterized::class)
class MobileLayoutQualityTest(
    private val width: Int,
    private val height: Int,
    private val locale: String,
    private val fontScale: Float,
) {
    @get:Rule
    val paparazzi = Paparazzi(
        environment = detectEnvironment().copy(compileSdkVersion = 34),
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = width,
            screenHeight = height,
            density = Density.MEDIUM,
            fontScale = fontScale,
            layoutDirection = if (locale == "ar") LayoutDirection.RTL else LayoutDirection.LTR,
            locale = locale,
        ),
        theme = "Theme.Bremo",
    )

    private val clipping = mutableListOf<String>()

    @Test
    fun screens_accommodate_localization_and_text_scaling() {
        SnapshotMode.enabled = true
        paparazzi.context.applicationInfo.flags = paparazzi.context.applicationInfo.flags or ApplicationInfo.FLAG_SUPPORTS_RTL
        val screens = ((1..9) + (14..36)).map { "SCR-C%02d".format(it) }
        screens.forEach { screen ->
            val state = ScreenSnapshots.cases(screen).map { CustomerLogic.reduce(screen, it.second) }
                .filter { it.phase != CustomerPhase.Loading && it.phase != CustomerPhase.Error }
                .maxByOrNull { (it.items + it.itemDetails + it.options + it.fieldValues).sumOf(String::length) } ?: return@forEach
            val view = customerScreenView(paparazzi.context, screen).apply { render(state) }
            paparazzi.snapshot(view, name = "$screen-$width-$locale-$fontScale")
            assertTextFits(view, screen)
        }
        (10..13).map { "SCR-C%02d".format(it) }.forEach { screen ->
            val state = ScreenSnapshots.cases(screen).map { AuthLogic.reduce(screen, it.second) }
                .firstOrNull { it.phase != AuthPhase.Loading && it.phase != AuthPhase.Error } ?: return@forEach
            val view = authScreenView(paparazzi.context, screen).apply { render(state) }
            paparazzi.snapshot(view, name = "$screen-$width-$locale-$fontScale")
            assertTextFits(view, screen)
        }
        (1..21).map { "SCR-P%02d".format(it) }.forEach { screen ->
            val state = ScreenSnapshots.cases(screen).map { ProviderLogic.reduce(screen, it.second) }
                .filter { it.phase != ProviderPhase.Loading && it.phase != ProviderPhase.Error }
                .maxByOrNull { (it.items + it.itemDetails + it.options + it.secondaryOptions + it.fieldValues).sumOf(String::length) } ?: return@forEach
            val view = providerScreenView(paparazzi.context, screen).apply { render(state) }
            paparazzi.snapshot(view, name = "$screen-$width-$locale-$fontScale")
            assertTextFits(view, screen)
        }
        assertTrue(clipping.joinToString("\n"), clipping.isEmpty())
    }

    @Test
    fun brand_loading_accommodates_localization_and_text_scaling() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C01HomeView(paparazzi.context).apply {
            render(CustomerUiState("SCR-C01", CustomerPhase.Loading))
        }
        paparazzi.snapshot(view, name = "brand-loading-$width-$locale-$fontScale")
        assertTextFits(view, "SCR-C01-loading")
        assertTrue(clipping.joinToString("\n"), clipping.isEmpty())
    }

    private fun assertTextFits(view: View, screen: String) {
        if (view.visibility != View.VISIBLE) return
        if (view is TextView && view !is EditText && view.text.isNotBlank() && view.layout != null) {
            val layout = view.layout
            if (layout.height + view.compoundPaddingTop + view.compoundPaddingBottom > view.height) {
                clipping += "$screen ${view.id}: clips ${view.text} (${view.height} < ${layout.height + view.compoundPaddingTop + view.compoundPaddingBottom})"
            }
        }
        if (view is ViewGroup) (0 until view.childCount).forEach { assertTextFits(view.getChildAt(it), screen) }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}x{1}-{2}-scale{3}")
        fun profiles(): List<Array<Any>> = listOf(
            arrayOf(320, 568, "ar", 1f), arrayOf(360, 800, "ar", 1f), arrayOf(412, 915, "ar", 1f),
            arrayOf(375, 667, "en", 1f), arrayOf(390, 844, "en", 1f), arrayOf(430, 932, "en", 1f),
            arrayOf(320, 568, "ar", 2f), arrayOf(320, 568, "en", 2f),
        )
    }
}
