package noox.bzr.provider

import noox.bzr.screens.ScreenSnapshots
import org.junit.Rule
import org.junit.Test

class ProviderJourneyViewsSnapshotTest {
    @get:Rule
    val paparazzi = ScreenSnapshots.paparazzi()

    @Test
    fun xmlScreenCasesMatchSharedFixture() {
        ScreenSnapshots.prepare(paparazzi)
        val only = System.getProperty("bzr.screens")?.split(",")?.toSet()
        ((1..11).map { "SCR-P%02d".format(it) } + listOf(
            "SCR-P12", "SCR-P13", "SCR-P14", "SCR-P15", "SCR-P16", "SCR-P17", "SCR-P18", "SCR-P19",
            "SCR-P20", "SCR-P21",
        )).filter { only == null || it in only }.forEach { screen ->
            ScreenSnapshots.cases(screen).forEach { (id, input) ->
                val state = ProviderLogic.reduce(screen, input)
                val view = providerScreenView(paparazzi.context, screen).apply { render(state) }
                paparazzi.snapshot(view, name = "$screen-$id")
            }
        }
    }
}
