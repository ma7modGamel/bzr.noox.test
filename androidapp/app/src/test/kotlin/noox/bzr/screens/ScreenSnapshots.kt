package noox.bzr.screens

import android.content.pm.ApplicationInfo
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.detectEnvironment
import com.android.resources.Density
import com.android.resources.LayoutDirection
import java.io.File
import noox.bzr.design.generated.GalleryFixtures
import noox.bzr.design.views.SnapshotMode
import org.json.JSONArray
import org.json.JSONObject

/** Paparazzi setup shared by the XML screen snapshots: 400×800, RTL, Arabic, Theme.Bremo (43 §8, §13). */
object ScreenSnapshots {
    fun paparazzi() = Paparazzi(
        environment = detectEnvironment().copy(compileSdkVersion = 34),
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = GalleryFixtures.viewportWidth,
            screenHeight = GalleryFixtures.viewportHeight,
            density = Density.MEDIUM,
            fontScale = GalleryFixtures.fontScale,
            layoutDirection = LayoutDirection.RTL,
            locale = "ar",
        ),
        theme = "Theme.Bremo",
    )

    fun prepare(paparazzi: Paparazzi) {
        // layoutlib ignores the manifest flag and scrolls RTL single-line fields (views/LayoutSupport.kt).
        SnapshotMode.enabled = true
        paparazzi.context.applicationInfo.flags = paparazzi.context.applicationInfo.flags or ApplicationInfo.FLAG_SUPPORTS_RTL
    }

    /** Snapshot cases of design/fixtures/<screen>/cases.json as (id, input). */
    fun cases(screen: String): List<Pair<String, Map<String, Any?>>> {
        val cases = JSONObject(repositoryFile("design/fixtures/$screen/cases.json").readText()).getJSONArray("cases")
        return (0 until cases.length()).map(cases::getJSONObject)
            .filter { it.optBoolean("snapshot", true) }
            .map { it.getString("id") to it.getJSONObject("input").map() }
    }

    private fun JSONObject.map(): Map<String, Any?> = keys().asSequence().associateWith { key ->
        when (val value = if (isNull(key)) null else get(key)) {
            is JSONArray -> (0 until value.length()).map(value::getString)
            else -> value
        }
    }

    private fun repositoryFile(path: String): File {
        var directory = File(checkNotNull(System.getProperty("user.dir")))
        while (!File(directory, "design").isDirectory) directory = directory.parentFile ?: error("Repository root not found")
        return File(directory, path)
    }
}
