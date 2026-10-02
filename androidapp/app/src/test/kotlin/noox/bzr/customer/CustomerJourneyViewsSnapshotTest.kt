package noox.bzr.customer

import noox.bzr.screens.ScreenSnapshots
import org.junit.Rule
import org.junit.Test

/** SCR-C01..C09, C14..C35 as XML Views (DEC-047) on the shared fixtures of the approved Compose snapshots. */
class CustomerJourneyViewsSnapshotTest {
    @get:Rule
    val paparazzi = ScreenSnapshots.paparazzi()

    @Test
    fun xml_screen_cases_match_shared_fixture() {
        ScreenSnapshots.prepare(paparazzi)
        val only = System.getProperty("bzr.screens")?.split(",")?.toSet()
        ((1..9) + (14..36)).map { "SCR-C%02d".format(it) }.filter { only == null || it in only }.forEach { screen ->
            ScreenSnapshots.cases(screen).forEach { (id, input) ->
                val state = CustomerLogic.reduce(screen, input)
                val view = customerScreenView(paparazzi.context, screen).apply { render(state) }
                paparazzi.snapshot(view, name = "$screen-$id")
            }
        }
    }
}
