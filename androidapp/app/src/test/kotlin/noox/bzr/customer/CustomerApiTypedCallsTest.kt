package noox.bzr.customer

import noox.bzr.auth.AuthModelsDecodeTest
import noox.bzr.customer.CustomerOrderModelsDecodeTest.Companion.recorded
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/** The typed customer calls send the same requests as the JSONObject code did and read the recorded answers. */
class CustomerApiTypedCallsTest {
    private lateinit var server: MockWebServer
    private lateinit var api: UrlConnectionCustomerApi

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        api = UrlConnectionCustomerApi(server.url("/api/v1/").toString())
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun homeReadsConfigOrdersAddressesCatalogAndAccount() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = MockResponse().setBody(
                when (request.path!!.substringAfter("/api/v1/").substringBefore('?')) {
                    "config" -> recorded("config")
                    "orders" -> recorded("orders_current")
                    "addresses" -> recorded("addresses")
                    "catalog" -> recorded("catalog_city")
                    "me" -> AuthModelsDecodeTest.recorded("me_200_unverified")
                    else -> error("unexpected ${request.path}")
                },
            )
        }

        val home = api.home("T")

        val requests = List(server.requestCount) { server.takeRequest() }
        assertEquals(
            listOf("config", "orders?scope=current&page=1", "addresses", "catalog?city_id=1000", "me"),
            requests.map { it.path!!.substringAfter("/api/v1/") },
        )
        requests.forEach { assertEquals("CUSTOMER", it.getHeader("X-App-Mode")) }
        assertNull(requests.first().getHeader("Authorization"))
        assertEquals("Bearer T", requests[1].getHeader("Authorization"))
        assertEquals("staff", home.operatingMode)
        assertEquals(1, home.orderCount)
        assertEquals(home.orders.single().id, home.firstOrderId)
        assertEquals(1000, home.addressId)
        assertEquals(1000, home.addressCityId)
        assertEquals("البيت · الحي الأول", home.addressLabel)
        assertEquals("سارة", home.customerName)
        assertEquals("plumbing", home.categories.first().iconKey)
        assertEquals("22:00", home.serviceHoursTo)
    }

    @Test
    fun orderOrdersAndTrackingUseTheirPaths() {
        server.enqueue(MockResponse().setBody(recorded("order_on_the_way")))
        server.enqueue(MockResponse().setBody(recorded("orders_past")))
        server.enqueue(MockResponse().setBody(recorded("tracking_on_the_way")))

        assertEquals("ON_THE_WAY", api.order("T", 7).status)
        assertEquals(2, api.orders("T", "past", 2).data.size)
        val tracking = api.tracking("T", 7)

        assertEquals("/api/v1/orders/7", server.takeRequest().path)
        assertEquals("/api/v1/orders?scope=past&page=2", server.takeRequest().path)
        assertEquals("/api/v1/orders/7/tracking", server.takeRequest().path)
        assertEquals(30.059, tracking.latitude!!, 0.0001)
        assertEquals(1, tracking.etaMinutes)
    }

    @Test
    fun orderActionsSendTheSameBodies() {
        repeat(5) { server.enqueue(MockResponse().setBody(recorded("order_cancelled"))) }

        api.cancelOrder("T", 7, "NO_LONGER_NEEDED", null, 3)
        api.decideProposal("T", 7, 9, approve = true, expectedVersion = 4)
        api.changePaymentMethod("T", 7, "CASH", 5)
        api.confirmCompletion("T", 7, 6)
        api.publish("T", JSONObject().put("category_id", 2).put("terms_accepted", true), "K")

        server.takeRequest().let {
            assertEquals("/api/v1/orders/7/cancel", it.path)
            val body = JSONObject(it.body.readUtf8())
            assertEquals("NO_LONGER_NEEDED", body.getString("reason_code"))
            assertEquals(3, body.getInt("expected_version"))
        }
        server.takeRequest().let {
            assertEquals("/api/v1/orders/7/proposals/9/decide", it.path)
            assertEquals(true, JSONObject(it.body.readUtf8()).getBoolean("approve"))
        }
        server.takeRequest().let {
            assertEquals("PATCH", it.method)
            assertEquals("/api/v1/orders/7/payment-method", it.path)
            assertEquals("CASH", JSONObject(it.body.readUtf8()).getString("payment_method"))
        }
        assertEquals(6, JSONObject(server.takeRequest().body.readUtf8()).getInt("expected_version"))
        server.takeRequest().let {
            assertEquals("/api/v1/orders", it.path)
            assertEquals("K", it.getHeader("Idempotency-Key"))
            assertEquals(2, JSONObject(it.body.readUtf8()).getInt("category_id"))
        }
    }

    @Test
    fun refusedRequestKeepsTheServerMessage() {
        server.enqueue(
            MockResponse().setResponseCode(409)
                .setBody("""{"error":{"code":"CONFLICT","message":"تغيّر الطلب، حدّث الصفحة."}}"""),
        )
        try {
            api.cancelOrder("T", 7, "OTHER", "x", 1)
            fail("expected a refusal")
        } catch (exception: CustomerApiException) {
            assertEquals(409, exception.status)
            assertEquals("CONFLICT", exception.error?.code)
            assertEquals("تغيّر الطلب، حدّث الصفحة.", exception.error?.message)
        }
    }
}
