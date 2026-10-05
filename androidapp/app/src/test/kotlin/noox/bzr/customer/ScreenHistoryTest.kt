package noox.bzr.customer

import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenHistoryTest {
    @Test
    fun historyMatchesSharedFixture() {
        val fixture = JSONObject(repositoryFile("design/fixtures/navigation/history.json").readText())
        val root = fixture.getString("root")
        val cases = fixture.getJSONArray("cases")
        repeat(cases.length()) { caseIndex ->
            val case = cases.getJSONObject(caseIndex)
            val history = ScreenHistory<String>(root) { it }
            val steps = case.getJSONArray("steps")
            repeat(steps.length()) { stepIndex ->
                val step = steps.getJSONObject(stepIndex)
                val label = "${case.getString("id")}#$stepIndex"
                when (step.getString("op")) {
                    "show" -> history.show(step.getString("screen"), step.optBoolean("restorable", true))
                    "commit" -> history.commit()
                    "back" -> assertEquals(label, if (step.isNull("expect")) null else step.getString("expect"), history.back { root })
                }
            }
        }
        assertEquals(8, cases.length())
    }

    @Test
    fun slotDayLabelsMatchSharedFixture() {
        val fixture = JSONObject(repositoryFile("design/fixtures/navigation/slot-days.json").readText())
        val names = fixture.getJSONArray("names").let { array -> List(array.length()) { array.getString(it) } }
        val cases = fixture.getJSONArray("cases")
        repeat(cases.length()) { index ->
            val case = cases.getJSONObject(index)
            val dates = case.getJSONArray("dates").let { array -> List(array.length()) { array.getString(it) } }
            val expected = case.getJSONArray("expected").let { array -> List(array.length()) { array.getString(it) } }
            assertEquals(case.getString("id"), expected, CustomerLogic.slotDayLabels(dates, names))
        }
    }

    private fun repositoryFile(path: String): File {
        var directory = File(checkNotNull(System.getProperty("user.dir")))
        while (!File(directory, "design").isDirectory) directory = directory.parentFile ?: error("Repository root not found")
        return File(directory, path)
    }
}
