package noox.bzr.auth

import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthSharedFixtureTest {
    @Test
    fun allSharedAuthCasesMatch() {
        val fixturePaths = listOf(
            "design/fixtures/SCR-C10/cases.json",
            "design/fixtures/SCR-C11/cases.json",
            "design/fixtures/SCR-C12/cases.json",
            "design/fixtures/SCR-C13/cases.json",
        )
        var count = 0
        fixturePaths.forEach { path ->
            val fixture = JSONObject(repositoryFile(path).readText())
            val screen = fixture.getString("screen")
            val cases = fixture.getJSONArray("cases")
            repeat(cases.length()) { index ->
                val case = cases.getJSONObject(index)
                val actual = AuthLogic.reduce(screen, case.getJSONObject("input").map())
                val expected = case.getJSONObject("expected")
                val label = "$screen/${case.getString("id")}" 
                assertEquals(label, expected.getString("phase"), actual.phase.name.lowercase())
                assertEquals(label, expected.getBoolean("can_submit"), actual.canSubmit)
                assertNullable(label, expected, "name_error", actual.nameError)
                assertNullable(label, expected, "email_error", actual.emailError)
                assertNullable(label, expected, "phone_error", actual.phoneError)
                assertNullable(label, expected, "password_error", actual.passwordError)
                assertNullable(label, expected, "message_key", actual.messageKey)
                assertNullable(label, expected, "route", actual.route)
                count++
            }
        }
        assertEquals(29, count)
    }

    private fun assertNullable(label: String, expected: JSONObject, key: String, actual: String?) {
        if (expected.has(key)) assertEquals(label, if (expected.isNull(key)) null else expected.getString(key), actual)
    }

    private fun JSONObject.map(): Map<String, Any?> = keys().asSequence().associateWith { key -> if (isNull(key)) null else get(key) }

    private fun repositoryFile(path: String): File {
        var directory = File(checkNotNull(System.getProperty("user.dir")))
        while (!File(directory, "design").isDirectory) directory = directory.parentFile ?: error("Repository root not found")
        return File(directory, path)
    }
}
