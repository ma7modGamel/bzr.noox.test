package noox.bzr.customer

import noox.bzr.network.BremoHttp
import noox.bzr.provider.OrderActionInput
import noox.bzr.provider.ProviderOrder
import noox.bzr.provider.UrlConnectionProviderApi
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class StaticDataSourcesApiTest {
    @Test fun structuredReviewSendsCodesAndPhotosWithoutChangingTheComment() {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setResponseCode(201).setBody("""{"data":{"id":1,"average":4.67}}"""))
            UrlConnectionCustomerApi(server.url("/api/v1").toString()).submitStructuredReview("T", 7, 5, 4, 5, "شكرا", listOf(11), listOf("CAREFUL"))
            val request = server.takeRequest()
            assertEquals("/api/v1/orders/7/review", request.path)
            assertEquals("Bearer T", request.getHeader("Authorization"))
            val body = JSONObject(request.body.readUtf8())
            assertEquals("CAREFUL", body.getJSONArray("tags").getString(0))
            assertEquals("شكرا", body.getString("comment"))
            assertEquals(11, body.getJSONArray("media_ids").getInt(0))
        }
    }

    @Test fun completionEstimateKeepsMinutesVersionAndReturnedTime() {
        val raw = requireNotNull(javaClass.classLoader!!.getResource("api/provider/order_in_progress.json")).readText()
        val record = JSONObject(raw).getJSONArray("data").getJSONObject(0)
        record.put("completion_estimate", JSONObject().put("estimated_at", "2026-10-14T10:45:00.000000Z").put("updated_at", "2026-10-14T10:00:00.000000Z").put("overdue", false))
        val order = BremoHttp.json.decodeFromString(ProviderOrder.serializer(), record.toString()).toPayload()
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setBody(JSONObject().put("data", record).toString()))
            val returned = UrlConnectionProviderApi(server.url("/api/v1").toString()).performOrderAction("T", order, "completion-estimate", OrderActionInput(minutes = 45))
            val request = server.takeRequest()
            assertEquals("/api/v1/provider/orders/${order.id}/completion-estimate", request.path)
            assertEquals("Bearer T", request.getHeader("Authorization"))
            val body = JSONObject(request.body.readUtf8())
            assertEquals(order.version, body.getInt("expected_version"))
            assertEquals(45, body.getInt("minutes"))
            assertEquals("2026-10-14T10:45:00.000000Z", returned.completionEstimateAt)
        }
    }
}
