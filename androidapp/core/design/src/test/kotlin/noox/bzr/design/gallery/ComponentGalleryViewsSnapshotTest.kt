package noox.bzr.design.gallery

import android.content.pm.ApplicationInfo
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.detectEnvironment
import com.android.resources.Density
import com.android.resources.LayoutDirection
import noox.bzr.design.generated.GalleryFixtures
import noox.bzr.design.views.SnapshotMode
import noox.bzr.design.views.gallery.ComponentGalleryViews
import org.junit.Rule
import org.junit.Test

/** XML gallery (DEC-047) on the same fixture and viewport as the approved Compose gallery. */
class ComponentGalleryViewsSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
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

    @Test
    fun xml_gallery_cases_match_shared_fixture() {
        // layoutlib ignores the library manifest; the app manifest declares supportsRtl (43 §13).
        SnapshotMode.enabled = true
        paparazzi.context.applicationInfo.flags = paparazzi.context.applicationInfo.flags or ApplicationInfo.FLAG_SUPPORTS_RTL
        GalleryFixtures.snapshotCases.forEach { fixture ->
            paparazzi.snapshot(ComponentGalleryViews.page(paparazzi.context, fixture.page, fixture.anchorBottom), name = "xml-gallery-${fixture.id}")
        }
    }
}
