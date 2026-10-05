package noox.bzr.provider

import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderSharedFixtureTest {
    @Test
    fun allProviderCasesMatch() {
        var count = 0
        listOf(
            "design/fixtures/SCR-P01/cases.json", "design/fixtures/SCR-P02/cases.json",
            "design/fixtures/SCR-P03/cases.json", "design/fixtures/SCR-P04/cases.json",
            "design/fixtures/SCR-P05/cases.json", "design/fixtures/SCR-P06/cases.json",
            "design/fixtures/SCR-P07/cases.json", "design/fixtures/SCR-P08/cases.json",
            "design/fixtures/SCR-P09/cases.json", "design/fixtures/SCR-P10/cases.json",
            "design/fixtures/SCR-P11/cases.json",
            "design/fixtures/SCR-P12/cases.json", "design/fixtures/SCR-P13/cases.json",
            "design/fixtures/SCR-P14/cases.json", "design/fixtures/SCR-P15/cases.json",
            "design/fixtures/SCR-P16/cases.json", "design/fixtures/SCR-P20/cases.json",
            "design/fixtures/SCR-P21/cases.json",
            "design/fixtures/SCR-P17/cases.json", "design/fixtures/SCR-P18/cases.json",
            "design/fixtures/SCR-P19/cases.json", "design/fixtures/SCR-C19/cases.json",
        ).forEach { path ->
            val fixture = JSONObject(repositoryFile(path).readText())
            val screen = fixture.getString("screen")
            val cases = fixture.getJSONArray("cases")
            repeat(cases.length()) { index ->
                val case = cases.getJSONObject(index)
                val actual = ProviderLogic.reduce(screen, case.getJSONObject("input").map())
                val expected = case.getJSONObject("expected")
                val label = "$screen/${case.getString("id")}"
                assertEquals(label, expected.getString("phase"), actual.phase.name.lowercase())
                assertEquals(label, expected.getBoolean("can_continue"), actual.canContinue)
                assertOptional(label, expected, "message_key", actual.messageKey)
                assertOptional(label, expected, "is_busy", actual.isBusy)
                assertOptional(label, expected, "has_profile_photo", actual.hasProfilePhoto)
                assertOptional(label, expected, "has_id_front", actual.hasIdFront)
                assertOptional(label, expected, "has_id_back", actual.hasIdBack)
                assertOptional(label, expected, "available_now", actual.availableNow)
                assertOptional(label, expected, "active_order_id", actual.activeOrderId)
                assertOptional(label, expected, "display_status", actual.displayStatus)
                assertOptional(label, expected, "unread_count", actual.unreadCount)
                assertOptional(label, expected, "selected_option_index", actual.selectedOptionIndex)
                assertOptional(label, expected, "show_price_guide", actual.showPriceGuide)
                assertOptional(label, expected, "show_outside_reason", actual.showOutsideReason)
                assertOptional(label, expected, "has_photo", actual.hasPhoto)
                assertOptional(label, expected, "current_action", actual.currentAction)
                assertOptional(label, expected, "step_count", actual.stepStates.size)
                assertOptional(label, expected, "secondary_item_count", actual.secondaryOptions.size)
                assertOptional(label, expected, "selected_count", actual.selectedCount)
                assertOptional(label, expected, "item_count", actual.items.size)
                if (expected.has("field_errors")) assertEquals(label, expected.getJSONArray("field_errors").strings(), actual.fieldErrors)
                if (expected.has("visible_actions")) assertEquals(label, expected.getJSONArray("visible_actions").strings(), actual.visibleActions)
                count++
            }
        }
        assertEquals(148, count)
    }

    private fun assertOptional(label: String, expected: JSONObject, key: String, actual: Any?) {
        if (expected.has(key)) assertEquals(label, if (expected.isNull(key)) null else expected.get(key), actual)
    }

    private fun JSONObject.map(): Map<String, Any?> = keys().asSequence().associateWith { key ->
        when (val value = if (isNull(key)) null else get(key)) {
            is JSONArray -> value.strings()
            else -> value
        }
    }

    private fun JSONArray.strings(): List<String> = (0 until length()).map(::getString)

    private fun repositoryFile(path: String): File {
        var directory = File(checkNotNull(System.getProperty("user.dir")))
        while (!File(directory, "design").isDirectory) directory = directory.parentFile ?: error("Repository root not found")
        return File(directory, path)
    }
}
