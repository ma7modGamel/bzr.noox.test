package noox.bzr.links

import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class DeepLinkRouterTest {
    @Test
    fun routesMatchSharedFixture() {
        val fixture = JSONObject(repositoryFile("design/fixtures/deeplinks/cases.json").readText())
        val cases = fixture.getJSONArray("cases")
        repeat(cases.length()) { index ->
            val case = cases.getJSONObject(index)
            val input = case.getJSONObject("input")
            val expected = case.getJSONObject("expected")
            val actual = DeepLinkRouter.route(input.getString("link"), input.getString("provider_status"))
            val label = case.getString("id")
            assertEquals(label, expected.getString("mode"), actual.mode)
            assertEquals(label, expected.getString("target"), actual.target)
            assertEquals(label, expected.getInt("order_id"), actual.orderId)
        }
        assertEquals(23, cases.length())
    }

    private fun repositoryFile(path: String): File =
        generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, path) }
            .first { it.exists() }
}
