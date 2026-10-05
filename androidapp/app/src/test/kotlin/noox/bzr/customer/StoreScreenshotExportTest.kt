package noox.bzr.customer

import android.view.View
import android.widget.LinearLayout
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.detectEnvironment
import com.android.resources.Density
import com.android.resources.LayoutDirection
import noox.bzr.auth.AuthPhase
import noox.bzr.auth.AuthUiState
import noox.bzr.auth.authScreenView
import noox.bzr.design.R as DesignR
import noox.bzr.design.views.BottomNavView
import noox.bzr.design.views.BremoBrandMarkView
import noox.bzr.gallery.R
import noox.bzr.screens.ScreenSnapshots
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/** Native Android captures for store artwork, using the shared demonstration fixtures. */
@RunWith(Parameterized::class)
class StoreScreenshotExportTest(private val locale: String) {
    @get:Rule
    val paparazzi = Paparazzi(
        environment = detectEnvironment().copy(compileSdkVersion = 34),
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = 1080,
            screenHeight = 1920,
            density = Density.create(400),
            fontScale = 1f,
            layoutDirection = if (locale == "ar") LayoutDirection.RTL else LayoutDirection.LTR,
            locale = locale,
        ),
        theme = "Theme.Bremo",
        supportsRtl = true,
        showSystemUi = true,
        useDeviceResolution = true,
    )

    @Test
    fun export_native_store_screens() {
        ScreenSnapshots.prepare(paparazzi)
        val captures = listOf(
            Triple("SCR-C01", "marketplace", "01-home"),
            Triple("SCR-C03", "with_media", "02-request"),
            Triple("SCR-C06", "execution_now", "03-offers"),
            Triple("SCR-C19", "messages", "04-chat"),
        )
        captures.forEach { (screen, case, filename) ->
            val input = ScreenSnapshots.cases(screen).first { it.first == case }.second + localizedDemo(screen)
            val view = customerScreenView(paparazzi.context, screen).apply {
                render(CustomerLogic.reduce(screen, input))
            }
            val root = LinearLayout(paparazzi.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutDirection = paparazzi.context.resources.configuration.layoutDirection
                // Layoutlib overlays its navigation decor; use the platform-provided system inset.
                val navigation = resources.getIdentifier("navigation_bar_height", "dimen", "android")
                setPadding(0, 0, 0, if (navigation == 0) 0 else resources.getDimensionPixelSize(navigation))
                addView(view, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
                if (screen == "SCR-C01") {
                    addView(BottomNavView(context).apply {
                        items = listOf(
                            DesignR.drawable.ic_home to context.getString(DesignR.string.nav_home),
                            DesignR.drawable.ic_orders to context.getString(DesignR.string.nav_orders),
                            DesignR.drawable.ic_chat to context.getString(DesignR.string.nav_messages),
                            DesignR.drawable.ic_account to context.getString(DesignR.string.nav_account),
                        )
                    })
                }
            }
            paparazzi.snapshot(root, name = "store-$locale-$filename")
        }
    }

    @Test
    fun brand_loading_exits_when_content_or_error_arrives() {
        ScreenSnapshots.prepare(paparazzi)
        val screen = C01HomeView(paparazzi.context)
        screen.render(CustomerUiState("SCR-C01", CustomerPhase.Loading))
        val loading = screen.findViewById<View>(R.id.loading)
        assertEquals(View.VISIBLE, loading.visibility)
        paparazzi.snapshot(screen, name = "brand-loading-$locale")
        screen.render(CustomerUiState("SCR-C01", CustomerPhase.Error))
        assertEquals(View.GONE, loading.visibility)
        screen.render(CustomerUiState("SCR-C01", CustomerPhase.Content))
        assertEquals(View.GONE, loading.visibility)
    }

    @Test
    fun authentication_screens_display_the_original_mark() {
        ScreenSnapshots.prepare(paparazzi)
        listOf("SCR-C10", "SCR-C11").forEach { screen ->
            val view = authScreenView(paparazzi.context, screen).apply {
                render(AuthUiState(screen, AuthPhase.Editing, canSubmit = false))
            }
            val mark = view.findViewById<BremoBrandMarkView>(R.id.brandLogo)
            assertNotNull(mark.drawable)
            assertEquals(paparazzi.context.getString(DesignR.string.app_name), mark.contentDescription)
            paparazzi.snapshot(view, name = "brand-$locale-$screen")
        }
    }

    private fun localizedDemo(screen: String): Map<String, Any?> {
        if (locale != "en") return emptyMap()
        var directory = File(checkNotNull(System.getProperty("user.dir")))
        while (!File(directory, "design").isDirectory) directory = directory.parentFile ?: error("Repository root not found")
        val data = JSONObject(File(directory, "design/brand/store-demo.en.json").readText()).getJSONObject(screen)
        return data.keys().asSequence().associateWith { key ->
            when (val value = data.get(key)) {
                is JSONArray -> (0 until value.length()).map(value::getString)
                else -> value
            }
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun locales(): List<Array<String>> = listOf(arrayOf("ar"), arrayOf("en"))
    }
}
