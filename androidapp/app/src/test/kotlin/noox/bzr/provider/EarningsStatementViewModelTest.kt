package noox.bzr.provider

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import noox.bzr.auth.AuthSessionStore
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EarningsStatementViewModelTest {
    @Test fun filtersPaginationFailuresAndExportKeepTheAppliedStatement(): Unit = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            MockWebServer().use { server ->
                server.dispatcher = object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        val url = request.requestUrl!!
                        if (url.encodedPath.endsWith("/export")) return MockResponse().setHeader("Content-Type", "text/csv").setBody("ID,Amount\norder-1,-100.00\n")
                        if (url.queryParameter("date_from") == "bad") return MockResponse().setResponseCode(422).setBody("""{"error":{"code":"VALIDATION_FAILED","message":"Invalid date"}}""")
                        val page = url.queryParameter("page") ?: "1"
                        return MockResponse().setBody("""{"data":{"operating_mode":"EMPLOYEE","summary":{"balance":"-350.00","available":"-350.00","pending":"0.00"},"transactions":[{"id":"order-$page","type":"ORDER","title":"Order $page","detail":"Cash due","amount":"-100.00"}],"filter_options":[{"code":"ALL","label":"All from server"},{"code":"ORDER","label":"Orders from server"}],"meta":{"current_page":$page,"last_page":2}}}""")
                    }
                }
                server.start()
                val session = object : AuthSessionStore {
                    override var token: String? = "T"
                    override var email: String? = null
                    override fun clear() { token = null }
                }
                val model = ProviderViewModel(UrlConnectionProviderApi(server.url("/api/v1").toString()), session, "EGP")
                withTimeout(5000) {
                    model.loadEarnings()
                    model.stateFlow.first { it.items == listOf("Order 1") }
                    assertEquals(listOf("All from server", "Orders from server"), model.stateFlow.value.earningsFilterLabels)
                    model.updateEarningsFilters(typeIndex = 1, dateFrom = "2026-10-01")
                    assertTrue(model.stateFlow.value.earningsFiltersDirty)
                    model.exportEarnings { fail("Unapplied filters must block export") }
                    model.applyEarningsFilters()
                    model.stateFlow.first { !it.isBusy && !it.earningsFiltersDirty }
                    assertEquals("-350 EGP", model.stateFlow.value.secondaryOptions.first())
                    model.applyEarningsFilters(more = true)
                    model.stateFlow.first { it.items.size == 2 && !it.isBusy }
                    assertEquals(listOf("Order 1", "Order 2"), model.stateFlow.value.items)
                    assertFalse(model.stateFlow.value.earningsHasMore)
                    val csv = CompletableDeferred<ByteArray>()
                    model.exportEarnings { csv.complete(it) }
                    assertEquals("ID,Amount\norder-1,-100.00\n", csv.await().toString(Charsets.UTF_8))
                    model.updateEarningsFilters(dateFrom = "bad")
                    model.applyEarningsFilters()
                    model.stateFlow.first { it.messageKey == "provider.earnings.filter_error" && !it.isBusy }
                    assertEquals(listOf("Order 1", "Order 2"), model.stateFlow.value.items)
                    model.loadEarnings()
                    model.stateFlow.first { it.phase == ProviderPhase.Content && !it.isBusy }
                    assertEquals("2026-10-01", model.stateFlow.value.earningsDateFrom)
                    assertFalse(model.stateFlow.value.earningsFiltersDirty)
                    model.applyEarningsFilters(reset = true)
                    model.stateFlow.first { !it.isBusy && it.earningsDateFrom.isEmpty() }
                }
            }
        } finally { Dispatchers.resetMain() }
    }
}
