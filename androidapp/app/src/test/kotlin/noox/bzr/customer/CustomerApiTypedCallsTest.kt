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

    @Test fun personalDetailsAndAvatarUseOwnedMediaAndMethodOverride() {
        val response = """{"user":{"id":1,"name":"Customer","email":"new@example.test","phone":"01012345678","is_verified":false,"avatar_url":"https://example.test/api/v1/me/avatar?v=2"}}"""
        server.enqueue(MockResponse().setBody(response))
        api.updateAccountDetails("T", "Customer", "new@example.test", "01012345678", "current-password")
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("PATCH", request.getHeader("X-HTTP-Method-Override"))
        val body = JSONObject(request.body.readUtf8())
        assertEquals("new@example.test", body.getString("email"))
        assertEquals("current-password", body.getString("current_password"))
        server.enqueue(MockResponse().setBody(response))
        assertEquals("https://example.test/api/v1/me/avatar?v=2", api.updateAvatar("T", 42).avatarUrl)
        assertEquals(42, JSONObject(server.takeRequest().body.readUtf8()).getInt("avatar_media_id"))
    }

    @Test fun notificationBadgeUsesTotalUnreadRatherThanTheFirstPage() {
        server.enqueue(MockResponse().setBody("""{"data":[],"meta":{"unread_count":25}}"""))
        assertEquals(25, api.unreadNotificationCount("T"))
    }

    @Test
    fun availabilityUsesTheAuthenticatedAddressAndCategory() {
        server.enqueue(MockResponse().setBody("{\"now_available\":false}"))
        assertEquals(false, api.requestAvailability("T", 12, 7))
        val request = server.takeRequest()
        assertEquals("/api/v1/orders/availability?customer_address_id=12&category_id=7", request.path)
        assertEquals("Bearer T", request.getHeader("Authorization"))
    }

    @Test
    fun instapayAndReviewSendReceiptAndOptionalPhotoIds() {
        server.enqueue(MockResponse().setBody("""{"data":{"id":21,"status":"PENDING_VERIFICATION","amount":"700.00"}}"""))
        assertEquals("PENDING_VERIFICATION", api.submitInstapay("T", 7, "IPN-778812", 14, 9).status)
        val transfer = server.takeRequest()
        assertEquals("/api/v1/orders/7/instapay-transfers", transfer.path)
        val transferBody = JSONObject(transfer.body.readUtf8())
        assertEquals(14, transferBody.getInt("receipt_media_id"))
        assertEquals(9, transferBody.getInt("expected_version"))
        assertEquals("IPN-778812", transferBody.getString("transfer_reference"))
        server.enqueue(MockResponse().setResponseCode(201).setBody("{}"))
        api.submitReviewWithPhotos("T", 7, 5, 4, 5, "Good repair", listOf(15, 16))
        val review = server.takeRequest()
        assertEquals("/api/v1/orders/7/review", review.path)
        assertEquals("[15,16]", JSONObject(review.body.readUtf8()).getJSONArray("media_ids").toString())
    }

    @Test
    fun sharingCreatesAnAuthenticatedTemporaryLinkWithoutSendingPersonalData() {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"data":{"id":12,"url":"https://example.test/v/test-token","expires_at":"2026-10-14T12:00:00+00:00"}}"""))
        val link = api.shareVisit("T", 7)
        assertEquals("https://example.test/v/test-token", link.url)
        assertEquals("2026-10-14T12:00:00+00:00", link.expiresAt)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/orders/7/share-links", request.path)
        assertEquals("Bearer T", request.getHeader("Authorization"))
        assertEquals(0, request.body.size)
    }

    @Test
    fun proposalsDecodeTheCurrentAndProjectedTotalsFromTheServer() {
        server.enqueue(MockResponse().setBody("""{"data":[{"id":9,"type":"MATERIALS","type_label":"خامات","amount":"80.00","current_total":"620.00","projected_total":"700.00","reason":"قطعة غيار","status":"PENDING","expires_at":"2020-01-01T12:30:00+00:00","server_time":"2020-01-01T12:00:00+00:00","expiry_message":"No cancellation"}]}"""))
        val proposal = api.proposals("T", 7).single()
        assertEquals(1800, proposal.remainingSeconds(java.time.OffsetDateTime.parse("2026-10-13T12:00:00+00:00")))
        assertEquals("No cancellation", proposal.expiryMessage)
        assertEquals("620.00", proposal.currentTotal)
        assertEquals("700.00", proposal.projectedTotal)
        assertEquals("/api/v1/orders/7/proposals", server.takeRequest().path)
    }

    @Test
    fun privateProfileImagesUseAuthenticationAndRejectOtherHosts() {
        server.enqueue(MockResponse().setBody("image bytes").setHeader("Content-Type", "image/jpeg"))
        val bytes = api.profileImage("T", server.url("/api/v1/providers/7/images/avatar?order_id=8").toString())
        assertEquals("image bytes", bytes.toString(Charsets.UTF_8))
        val request = server.takeRequest()
        assertEquals("Bearer T", request.getHeader("Authorization"))
        assertEquals("/api/v1/providers/7/images/avatar?order_id=8", request.path)
        try {
            api.profileImage("T", "https://outside.example/image.jpg")
            fail("An image request must not send the account token to another host")
        } catch (_: IllegalArgumentException) { }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun accountAndConversationsDecodeRealTotalsUnreadCountsAndAvailability() {
        server.enqueue(MockResponse().setBody("""{"user":{"id":1,"name":"محمد","email":"customer@example.test","is_verified":true,"order_statistics":{"completed":24,"active":4},"addresses_count":2}}"""))
        val account = api.account("T")
        assertEquals(24, account.orderStatistics?.completed)
        assertEquals(4, account.orderStatistics?.active)
        server.takeRequest()
        server.enqueue(MockResponse().setBody("""{"data":[{"id":8,"order":{"id":7},"provider":{"id":3,"available_now":true},"status":"OPEN","unread_count":3}]}"""))
        val conversation = api.conversations("T").data.single()
        assertEquals(3, conversation.unreadCount)
        assertEquals(true, conversation.provider.availableNow)
        server.takeRequest()
    }

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
        api.publish("T", PublishOrderBody(1, 2, 3, "NOW", termsAccepted = true, mediaIds = emptyList(), materialsResponsibility = "UNSURE"), "K")

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
            assertEquals("POST", it.method)
            assertEquals("PATCH", it.getHeader("X-HTTP-Method-Override"))
            assertEquals("/api/v1/orders/7/payment-method", it.path)
            assertEquals("CASH", JSONObject(it.body.readUtf8()).getString("payment_method"))
        }
        assertEquals(6, JSONObject(server.takeRequest().body.readUtf8()).getInt("expected_version"))
        server.takeRequest().let {
            assertEquals("/api/v1/orders", it.path)
            assertEquals("K", it.getHeader("Idempotency-Key"))
            val body = JSONObject(it.body.readUtf8())
            assertEquals(2, body.getInt("category_id"))
            assertEquals(0, body.getJSONArray("media_ids").length())
            // Missing optional values are left out, as the JSONObject body did (BR-008: no pricing in employee mode).
            assertEquals(
                setOf("customer_address_id", "category_id", "problem_type_id", "timing_type", "terms_accepted", "media_ids", "materials_responsibility"),
                body.keys().asSequence().toSet(),
            )
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
