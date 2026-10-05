package noox.bzr.auth

import noox.bzr.screens.ScreenSnapshots
import org.junit.Rule
import org.junit.Test

/** SCR-C10..C13 as XML Views (DEC-047) on the same shared fixtures as the approved Compose snapshots. */
class CustomerAuthViewsSnapshotTest {
    @get:Rule
    val paparazzi = ScreenSnapshots.paparazzi()

    @Test
    fun xml_screen_cases_match_shared_fixture() {
        ScreenSnapshots.prepare(paparazzi)
        listOf("SCR-C10", "SCR-C11", "SCR-C12", "SCR-C13").forEach { screen ->
            ScreenSnapshots.cases(screen).forEach { (id, input) ->
                val state = AuthLogic.reduce(screen, input)
                val view = authScreenView(paparazzi.context, screen).apply { render(state) }
                paparazzi.snapshot(view, name = "$screen-$id")
            }
        }
    }
}
