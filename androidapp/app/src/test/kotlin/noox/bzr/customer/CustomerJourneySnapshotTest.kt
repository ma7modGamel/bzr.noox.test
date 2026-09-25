package noox.bzr.customer

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.detectEnvironment
import com.android.resources.Density
import java.io.File
import noox.bzr.design.BzrTheme
import noox.bzr.design.generated.GalleryFixtures
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test

class CustomerJourneySnapshotTest {
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
    fun allSharedVisualCasesRender() {
        ((1..9) + (14..35)).forEach { number ->
            val screen = "SCR-C%02d".format(number)
            val fixture = JSONObject(repositoryFile("design/fixtures/$screen/cases.json").readText())
            val cases = fixture.getJSONArray("cases")
            repeat(cases.length()) { index ->
                val case = cases.getJSONObject(index)
                if (!case.optBoolean("snapshot", true)) return@repeat
                val state = CustomerLogic.reduce(screen, case.getJSONObject("input").map())
                paparazzi.snapshot(name = "$screen-${case.getString("id")}") {
                    BzrTheme { render(screen, state) }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun render(screen: String, state: CustomerUiState) {
        when (screen) {
            "SCR-C01" -> C01HomeScreen(state)
            "SCR-C02" -> C02AccountScreen(state)
            "SCR-C03" -> C03ProblemScreen(state)
            "SCR-C04" -> C04TimingScreen(state)
            "SCR-C05" -> C05ReviewScreen(state)
            "SCR-C06" -> C06OffersScreen(state)
            "SCR-C07" -> C07ProviderScreen(state)
            "SCR-C08" -> C08OfferDetailsScreen(state)
            "SCR-C09" -> C09TrackingScreen(state)
            "SCR-C14" -> C14AddressesScreen(state)
            "SCR-C15" -> C15AddressFormScreen(state)
            "SCR-C16" -> C16SlotPickerScreen(state)
            "SCR-C17" -> C17MediaScreen(state)
            "SCR-C18" -> C18MessagesScreen(state)
            "SCR-C19" -> C19ChatScreen(state)
            "SCR-C20" -> C20CancellationScreen(state)
            "SCR-C21" -> C21PaymentSummaryScreen(state)
            "SCR-C22" -> C22ElectronicPaymentScreen(state)
            "SCR-C23" -> C23CompletionScreen(state)
            "SCR-C24" -> C24RatingScreen(state)
            "SCR-C25" -> C25OrdersScreen(state)
            "SCR-C26" -> C26OrderHistoryScreen(state)
            "SCR-C27" -> C27DisputeScreen(state)
            "SCR-C28" -> C28ProviderReportScreen(state)
            "SCR-C29" -> C29HelpScreen(state)
            "SCR-C30" -> C30ExecutionQuoteScreen(state)
            "SCR-C31" -> C31AdditionalCostScreen(state)
            "SCR-C32" -> C32NotificationsScreen(state)
            "SCR-C33" -> C33AccountSettingsScreen(state)
            "SCR-C34" -> C34TermsScreen(state)
            "SCR-C35" -> C35NoOffersScreen(state)
            else -> error("Unknown screen: $screen")
        }
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
