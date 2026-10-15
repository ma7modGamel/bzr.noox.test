package noox.bzr.links

import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/** DEC-067: the shared restore cases, the same ones Swift Core runs. */
class ModeMemoryTest {
    @Test
    fun sharedRestoreCasesMatch() {
        val cases = JSONObject(repositoryFile("design/fixtures/navigation/mode-restore.json").readText()).getJSONArray("cases")
        repeat(cases.length()) { index ->
            val case = cases.getJSONObject(index)
            val input = case.getJSONObject("input")
            val expected = case.getJSONObject("expected")
            val result = ModeMemory.restoreTarget(
                lastMode = if (input.isNull("last_mode")) null else input.getString("last_mode"),
                providerStatus = input.getString("provider_status"),
                hasPendingLink = input.getBoolean("has_pending_link"),
            )
            assertEquals(case.getString("id"), ModeRestore(expected.getString("target"), expected.getBoolean("forget")), result)
        }
        assertEquals(7, cases.length())
    }

    private fun repositoryFile(path: String): File {
        var directory = File(checkNotNull(System.getProperty("user.dir")))
        while (!File(directory, "design").isDirectory) directory = directory.parentFile ?: error("Repository root not found")
        return File(directory, path)
    }
}
