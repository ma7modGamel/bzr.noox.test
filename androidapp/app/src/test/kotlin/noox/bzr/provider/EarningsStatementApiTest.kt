package noox.bzr.provider

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class EarningsStatementApiTest {
    @Test fun filteredPagesAndExportUseTheSameAuthenticatedFiltersAndExportReturnsCsvBytes() {
        MockWebServer().use { server ->
            server.start()
            val api = UrlConnectionProviderApi(server.url("/api/v1").toString())
            server.enqueue(MockResponse().setBody("""{"data":{"operating_mode":"EMPLOYEE","summary":{"balance":"-350.00","available":"-350.00","pending":"0.00"},"transactions":[],"filter_options":[{"code":"ORDER","label":"From server"}],"meta":{"current_page":2,"last_page":3,"total":45}}}"""))
            val response = api.filteredEarnings("T", 2, "ORDER", "2026-10-01", "2026-10-13")
            val request = server.takeRequest()
            assertEquals("/api/v1/provider/earnings?page=2&type=ORDER&date_from=2026-10-01&date_to=2026-10-13", request.path)
            assertEquals("Bearer T", request.getHeader("Authorization"))
            assertEquals(2, response.currentPage)
            assertEquals(3, response.lastPage)
            assertEquals("From server", response.filterOptions.single().label)
            val csv = "\uFEFFID,Amount\norder-1,-100.00\n"
            server.enqueue(MockResponse().setHeader("Content-Type", "text/csv").setBody(csv))
            assertEquals(csv, api.exportEarnings("T", "ORDER", "2026-10-01", "2026-10-13").toString(Charsets.UTF_8))
            val export = server.takeRequest()
            assertEquals("/api/v1/provider/earnings/export?type=ORDER&date_from=2026-10-01&date_to=2026-10-13", export.path)
            assertEquals("Bearer T", export.getHeader("Authorization"))
        }
    }
}
