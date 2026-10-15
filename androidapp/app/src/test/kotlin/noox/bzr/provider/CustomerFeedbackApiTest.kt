package noox.bzr.provider

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CustomerFeedbackApiTest {
    @Test fun ratingSendsTheSelectedStableCodesAndCommentToTheAuthenticatedEndpoint() {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setResponseCode(201).setBody("""{"data":{"id":1}}"""))
            val api = UrlConnectionProviderApi(server.url("/api/v1").toString())
            api.rateCustomerWithFeedback("T", 7, 4, listOf("COOPERATIVE", "PUNCTUAL"), "Clear description")
            val request = server.takeRequest()
            assertEquals("/api/v1/provider/orders/7/customer-rating", request.path)
            assertEquals("Bearer T", request.getHeader("Authorization"))
            val body = JSONObject(request.body.readUtf8())
            assertEquals(4, body.getInt("stars"))
            assertEquals("[\"COOPERATIVE\",\"PUNCTUAL\"]", body.getJSONArray("tags").toString())
            assertEquals("Clear description", body.getString("comment"))
        }
    }
}
