package noox.bzr.customer

import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomerSharedFixtureTest {
    @Test
    fun allCustomerCasesMatch() {
        var count = 0
        val fixturePaths = listOf(
            "design/fixtures/SCR-C01/cases.json", "design/fixtures/SCR-C02/cases.json",
            "design/fixtures/SCR-C03/cases.json", "design/fixtures/SCR-C04/cases.json",
            "design/fixtures/SCR-C05/cases.json", "design/fixtures/SCR-C06/cases.json",
            "design/fixtures/SCR-C07/cases.json", "design/fixtures/SCR-C08/cases.json",
            "design/fixtures/SCR-C09/cases.json", "design/fixtures/SCR-C14/cases.json",
            "design/fixtures/SCR-C15/cases.json", "design/fixtures/SCR-C16/cases.json",
            "design/fixtures/SCR-C17/cases.json", "design/fixtures/SCR-C18/cases.json",
            "design/fixtures/SCR-C19/cases.json", "design/fixtures/SCR-C20/cases.json",
            "design/fixtures/SCR-C21/cases.json", "design/fixtures/SCR-C22/cases.json",
            "design/fixtures/SCR-C23/cases.json", "design/fixtures/SCR-C24/cases.json",
            "design/fixtures/SCR-C25/cases.json", "design/fixtures/SCR-C26/cases.json",
            "design/fixtures/SCR-C27/cases.json", "design/fixtures/SCR-C28/cases.json",
            "design/fixtures/SCR-C29/cases.json",
            "design/fixtures/SCR-C30/cases.json", "design/fixtures/SCR-C31/cases.json",
            "design/fixtures/SCR-C32/cases.json", "design/fixtures/SCR-C33/cases.json",
            "design/fixtures/SCR-C34/cases.json", "design/fixtures/SCR-C35/cases.json",
        )
        fixturePaths.forEach { path ->
            val fixture = JSONObject(repositoryFile(path).readText())
            val screen = fixture.getString("screen")
            val cases = fixture.getJSONArray("cases")
            repeat(cases.length()) { index ->
                val case = cases.getJSONObject(index)
                val actual = CustomerLogic.reduce(screen, case.getJSONObject("input").map())
                val expected = case.getJSONObject("expected")
                val label = "$screen/${case.getString("id")}" 
                assertEquals(label, expected.getString("phase"), actual.phase.name.lowercase())
                assertEquals(label, expected.getBoolean("can_continue"), actual.canContinue)
                assertOptional(label, expected, "message_key", actual.messageKey)
                assertOptional(label, expected, "description_error", actual.descriptionError)
                assertOptional(label, expected, "terms_error", actual.termsError)
                assertOptional(label, expected, "show_pricing", actual.showPricing)
                assertOptional(label, expected, "show_map", actual.showMap)
                assertOptional(label, expected, "show_eta", actual.showEta)
                assertOptional(label, expected, "show_stepper", actual.showStepper)
                assertOptional(label, expected, "show_status_card", actual.showStatusCard)
                assertOptional(label, expected, "eta_approximate", actual.etaApproximate)
                assertOptional(label, expected, "item_count", actual.items.size)
                assertOptional(label, expected, "selected_index", actual.selectedIndex)
                assertOptional(label, expected, "selected_option_index", actual.selectedOptionIndex)
                if (expected.has("field_errors")) assertEquals(label, expected.getJSONArray("field_errors").strings(), actual.fieldErrors)
                assertOptional(label, expected, "is_busy", actual.isBusy)
                assertOptional(label, expected, "is_editing", actual.isEditing)
                assertOptional(label, expected, "is_default", actual.isDefault)
                assertOptional(label, expected, "show_confirmation", actual.showConfirmation)
                assertOptional(label, expected, "pending_index", actual.pendingIndex)
                assertOptional(label, expected, "countdown_seconds", actual.countdownSeconds)
                assertOptional(label, expected, "has_photo", actual.hasPhoto)
                if (expected.has("visible_actions")) assertEquals(label, expected.getJSONArray("visible_actions").strings(), actual.visibleActions)
                count++
            }
        }
        assertEquals(206, count)
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
