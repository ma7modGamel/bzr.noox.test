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
import noox.bzr.gallery.R
import noox.bzr.provider.ProviderLogic
import noox.bzr.provider.ProviderPhase
import noox.bzr.provider.providerScreenView
import noox.bzr.screens.ScreenSnapshots
import org.junit.Assert.assertEquals
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
    fun legal_html_renders_formatted_text_in_the_selected_direction() {
        ScreenSnapshots.prepare(paparazzi)
        val heading = if (locale == "ar") "الشروط" else "Terms"
        val paragraph = if (locale == "ar") "نص الوثيقة" else "Document text"
        val state = CustomerLogic.reduce("SCR-C34", mapOf(
            "event" to "loaded", "version" to "1", "effective_at" to "2026-10-11",
            "body" to "<h2>$heading</h2><p><strong>$paragraph</strong></p><ul><li>$paragraph</li></ul><ol><li>$paragraph</li><li>$paragraph</li></ol>",
        ))
        val view = C34TermsView(paparazzi.context).apply { render(state) }
        paparazzi.snapshot(view, name = "legal-html-$width-$locale-$fontScale")
        val texts = mutableListOf<TextView>()
        fun collect(node: View) {
            if (node is TextView) texts += node
            if (node is ViewGroup) (0 until node.childCount).forEach { collect(node.getChildAt(it)) }
        }
        collect(view)
        val document = texts.first { it.text.contains(paragraph) }
        assertTrue(document.text.contains(heading))
        assertTrue(!document.text.contains("<h2>") && !document.text.contains("<p>"))
        assertTrue(document.text is android.text.Spanned)
        assertTrue(document.text.contains("1. $paragraph") && document.text.contains("2. $paragraph"))
        assertEquals(if (locale == "ar") View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR, document.layoutDirection)
        assertTextFits(document, "SCR-C34-html")
        assertTrue(clipping.joinToString("\n"), clipping.isEmpty())
    }

    @Test
    fun native_plurals_select_singular_dual_and_other_categories() {
        val context = paparazzi.context
        val quantity = noox.bzr.design.R.plurals.plural_offers_available
        val one = noox.bzr.design.BzrFormat.quantity(context, quantity, 1)
        val two = noox.bzr.design.BzrFormat.quantity(context, quantity, 2)
        val three = noox.bzr.design.BzrFormat.quantity(context, quantity, 3)
        assertEquals(if (locale == "ar") "عرض واحد متاح" else "1 offer available", one)
        assertEquals(if (locale == "ar") "عرضان متاحان" else "2 offers available", two)
        assertEquals(if (locale == "ar") "3 عروض متاحة" else "3 offers available", three)
    }

    @Test
    fun screens_accommodate_localization_and_text_scaling() {
        SnapshotMode.enabled = true
        paparazzi.context.applicationInfo.flags = paparazzi.context.applicationInfo.flags or ApplicationInfo.FLAG_SUPPORTS_RTL
        val screens = ((1..9) + (14..38)).map { "SCR-C%02d".format(it) }
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
    fun home_hero_photo_covers_content_without_current_order() {
        ScreenSnapshots.prepare(paparazzi)
        val input = ScreenSnapshots.cases("SCR-C01").first { it.first == "empty" }.second
        val view = C01HomeView(paparazzi.context).apply { render(CustomerLogic.reduce("SCR-C01", input)) }

        paparazzi.snapshot(view, name = "home-empty-$width-$locale-$fontScale")

        val hero = view.findViewById<View>(R.id.hero)
        val photo = view.findViewById<View>(R.id.heroImage)
        val content = view.findViewById<View>(R.id.heroContent)
        assertEquals("The photo must cover the full banner height", hero.height, photo.height)
        assertEquals("The photo must start at the top of the banner", 0, photo.top)
        assertEquals("The content must determine the banner height", content.height, hero.height)
        assertTrue("Services must retain their height below the banner", view.findViewById<View>(R.id.services).height > 0)
        assertTrue("The emergency row must retain its height below the banner", view.findViewById<View>(R.id.urgent).height > 0)
        assertTextFits(hero, "SCR-C01-empty")
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
