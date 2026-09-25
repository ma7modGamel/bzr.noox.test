package noox.bzr.auth

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.detectEnvironment
import com.android.resources.Density
import java.io.File
import noox.bzr.design.BzrTheme
import noox.bzr.design.generated.GalleryFixtures
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test

class CustomerAuthSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        environment = detectEnvironment().copy(compileSdkVersion = 34),
        deviceConfig = DeviceConfig.PIXEL_5.copy(
            screenWidth = GalleryFixtures.viewportWidth,
            screenHeight = GalleryFixtures.viewportHeight,
            density = Density.MEDIUM,
            fontScale = GalleryFixtures.fontScale,
        ),
        theme = "android:style/Theme.Material.Light.NoActionBar",
    )

    @Test
    fun sharedSnapshotCasesRender() {
        listOf(
            "design/fixtures/SCR-C10/cases.json",
            "design/fixtures/SCR-C11/cases.json",
            "design/fixtures/SCR-C12/cases.json",
            "design/fixtures/SCR-C13/cases.json",
        ).forEach { path ->
            val fixture = JSONObject(repositoryFile(path).readText())
            val screen = fixture.getString("screen")
            val cases = fixture.getJSONArray("cases")
            repeat(cases.length()) { index ->
                val case = cases.getJSONObject(index)
                if (!case.getBoolean("snapshot")) return@repeat
                val state = AuthLogic.reduce(screen, case.getJSONObject("input").map())
                paparazzi.snapshot(name = "$screen-${case.getString("id")}") {
                    BzrTheme {
                        when (screen) {
                            "SCR-C10" -> CustomerLoginScreen(state)
                            "SCR-C11" -> CustomerRegisterScreen(state)
                            "SCR-C12" -> CustomerEmailVerificationScreen(state)
                            else -> CustomerPasswordRecoveryScreen(state)
                        }
                    }
                }
            }
        }
    }

    private fun JSONObject.map(): Map<String, Any?> = keys().asSequence().associateWith { key -> if (isNull(key)) null else get(key) }

    private fun repositoryFile(path: String): File {
        var directory = File(checkNotNull(System.getProperty("user.dir")))
        while (!File(directory, "design").isDirectory) directory = directory.parentFile ?: error("Repository root not found")
        return File(directory, path)
    }
}
