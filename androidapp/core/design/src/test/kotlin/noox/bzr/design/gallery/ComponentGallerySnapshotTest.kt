package noox.bzr.design.gallery

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.detectEnvironment
import com.android.resources.Density
import noox.bzr.design.generated.GalleryFixtures
import org.junit.Rule
import org.junit.Test

class ComponentGallerySnapshotTest {
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
    fun gallery_cases_match_shared_fixture() {
        GalleryFixtures.snapshotCases.forEach { fixture ->
            paparazzi.snapshot(name = "gallery-${fixture.id}") {
                GalleryPage(page = fixture.page, anchorBottom = fixture.anchorBottom)
            }
        }
    }
}
