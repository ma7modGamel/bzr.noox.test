package noox.bzr.customer

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import noox.bzr.provider.OrderActionInput
import noox.bzr.provider.ProviderOrder
import noox.bzr.provider.UrlConnectionProviderApi
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class StaffMediaAndArrivalApiTest {
    @Test fun customerCodeRefreshUsesItsAuthenticatedEndpointAndDoesNotModifyTheOrder() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"data":{"code":"123456","expires_at":"2026-10-15T10:40:00Z","server_time":"2026-10-15T10:30:00Z"}}"""))
            val code = UrlConnectionCustomerApi(server.url("/api/v1/").toString()).arrivalCode("T", 7, true)
            assertEquals("123456", code.code)
            val request = server.takeRequest()
            assertEquals("/api/v1/orders/7/arrival-code", request.path)
            assertEquals("Bearer T", request.getHeader("Authorization"))
            assertEquals("true", Json.parseToJsonElement(request.body.readUtf8()).jsonObject["refresh"]!!.jsonPrimitive.content)
        }
    }

    @Test fun providerArrivalCarriesTheCodeAndExpectedVersionAndPrivateImagesKeepAuthentication() {
        MockWebServer().use { server ->
            val list = javaClass.classLoader!!.getResource("api/provider/order_in_progress.json")!!.readText()
            val record = Json.parseToJsonElement(list).jsonObject["data"]!!.jsonArray.first()
            val decoder = Json { ignoreUnknownKeys = true }
            val order = decoder.decodeFromJsonElement(ProviderOrder.serializer(), record).toPayload()
            server.enqueue(MockResponse().setBody("""{"data":$record}"""))
            val api = UrlConnectionProviderApi(server.url("/api/v1/").toString())
            api.performOrderAction("T", order, "arrived", OrderActionInput(lat = 30.06, lng = 31.34, arrivalCode = "123456"))
            val request = server.takeRequest()
            val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
            assertEquals("123456", body["arrival_code"]!!.jsonPrimitive.content)
            assertEquals(order.version.toString(), body["expected_version"]!!.jsonPrimitive.content)
            server.enqueue(MockResponse().setBody("image-bytes"))
            assertEquals("image-bytes", api.image("T", server.url("/api/v1/private-image").toString()).decodeToString())
            assertEquals("Bearer T", server.takeRequest().getHeader("Authorization"))
            runCatching { api.image("T", "https://another.test/image") }.onSuccess { fail("Cross-origin image must be rejected") }
        }
    }
}
