package noox.bzr.design

import java.io.File
import java.time.Instant
import java.time.ZoneId
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedCoreFixtureTest {
    @Test
    fun sharedOrderCasesCoverActionsAndOnHoldStepper() {
        val fixture = fixture()
        val orders = fixture.getJSONArray("orders")

        assertEquals(listOf("open_employee_waiting_assignment", "disputed_from_in_progress"), (0 until orders.length()).map { orders.getJSONObject(it).getString("case_id") })

        val open = orders.getJSONObject(0)
        assertEquals(listOf("action.edit_request", "action.cancel"), open.getJSONArray("expected_action_text_keys").strings())
        assertTrue(open.isNull("expected_stepper_state"))
        assertTrue(open.getJSONObject("payload").isNull("stepper"))

        val disputed = orders.getJSONObject(1)
        assertEquals(listOf("action.chat", "action.call"), disputed.getJSONArray("expected_action_text_keys").strings())
        assertEquals("on_hold", disputed.getString("expected_stepper_state"))
        assertEquals("on_hold", disputed.getJSONObject("payload").getJSONArray("stepper").getJSONObject(3).getString("state"))
    }

    @Test
    fun formatsEverySharedFormattingCase() {
        val formatting = fixture().getJSONObject("formatting")
        val number = formatting.getJSONObject("number")
        assertEquals(number.getString("expected"), BzrFormat.number(number.getDouble("input")))

        val amount = formatting.getJSONObject("amount")
        assertEquals(amount.getString("expected"), BzrFormat.amount(amount.getDouble("input"), amount.getString("currency")))

        val countdown = formatting.getJSONObject("countdown")
        assertEquals(countdown.getString("expected"), BzrFormat.countdown(countdown.getInt("input_seconds")))

        val time = formatting.getJSONObject("time")
        assertEquals(
            time.getString("expected"),
            BzrFormat.time(Instant.parse(time.getString("iso8601")), ZoneId.of(time.getString("timezone")), time.getString("morning"), time.getString("evening")),
        )

        val weekdays = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
        val months = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
        val dates = formatting.getJSONArray("dates")
        for (index in 0 until dates.length()) {
            val date = dates.getJSONObject(index)
            assertEquals(
                date.getString("expected"),
                BzrFormat.dateLabel(
                    Instant.parse(date.getString("iso8601")),
                    Instant.parse(date.getString("reference")),
                    ZoneId.of(date.getString("timezone")),
                    "اليوم",
                    "غدًا",
                    weekdays,
                    months,
                ),
            )
        }
    }

    private fun fixture(): JSONObject {
        var directory = File(checkNotNull(System.getProperty("user.dir")))
        while (!File(directory, "design/fixtures/contracts/mobile-core.json").isFile) {
            directory = directory.parentFile ?: error("Repository root not found")
        }
        return JSONObject(File(directory, "design/fixtures/contracts/mobile-core.json").readText())
    }

    private fun org.json.JSONArray.strings(): List<String> = (0 until length()).map(::getString)
}
