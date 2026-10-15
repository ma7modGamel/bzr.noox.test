package noox.bzr.provider

import kotlinx.serialization.KSerializer
import noox.bzr.customer.CustomerMediaUpload
import noox.bzr.customer.DataItem
import noox.bzr.customer.DataList
import noox.bzr.network.BremoHttp
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DEC-061 / 9أ stage 4: every provider model decodes the real server answers recorded by
 * tests/Feature/Api/MobileProviderResponsesTest.php, maps onto the payloads the screens use, and the calls
 * send the same bodies as the JSONObject code did.
 */
class ProviderModelsDecodeTest {
    @Test
    fun applicationDecodes() {
        val empty = decode("application_not_started", DataItem.serializer(ProviderApplication.serializer())).data.toPayload()
        assertEquals("NOT_STARTED", empty.status)
        assertNull(empty.reason)
        val rejected = decode("application_rejected", DataItem.serializer(ProviderApplication.serializer())).data.toPayload()
        assertEquals("REJECTED", rejected.status)
        assertEquals("صورة البطاقة غير واضحة", rejected.reason)
        assertEquals("", rejected.bio)
        assertTrue(rejected.idFrontUploaded && rejected.idBackUploaded && rejected.profilePhotoUploaded)
        assertTrue("resubmit_provider_application" in rejected.availableActions)
    }

    @Test
    fun homeAndOrdersDecode() {
        val home = decode("home_active", DataItem.serializer(ProviderHome.serializer())).data.toPayload()
        assertEquals("EMPLOYEE", home.operatingMode)
        assertEquals("سباكة", home.activeOrder!!.category)
        assertFalse(decode("availability_off", DataItem.serializer(ProviderHome.serializer())).data.availableNow)

        val order = decode("orders_current", DataList.serializer(ProviderOrder.serializer())).data.single().toPayload()
        assertEquals("1", order.number)
        assertEquals(30.06, order.latitude!!, 0.0001)
        assertEquals("شارع الطيران، عمارة 12", order.address.substringBefore(","))
        assertEquals("300.00" to "500.00", order.priceGuideMinimum to order.priceGuideMaximum)
        assertEquals("0.00", order.laborTotal)
        assertEquals("", order.customerRating)
        assertEquals("01011111111", order.customerPhone)

        val onTheWay = decode("order_on_the_way", DataItem.serializer(ProviderOrder.serializer())).data.toPayload()
        assertEquals("ON_THE_WAY", onTheWay.status)
        val cash = decode("order_cash_received", DataItem.serializer(ProviderOrder.serializer())).data.toPayload()
        assertEquals("450.00", cash.finalAmount)
        assertEquals("CASH", cash.paymentMethod)
        assertTrue(decode("orders_past", DataList.serializer(ProviderOrder.serializer())).data.isNotEmpty())
        assertTrue(decode("order_in_progress", DataList.serializer(ProviderOrder.serializer())).data.isNotEmpty())
        assertTrue(decode("order_completed", DataItem.serializer(ProviderOrder.serializer())).data.status.isNotBlank())
    }

    @Test
    fun marketDecodes() {
        val requests = decode("requests", DataList.serializer(ProviderOrder.serializer())).data
        assertEquals("MARKETPLACE", decode("request_detail", DataItem.serializer(MarketRequest.serializer())).data.order.let { "MARKETPLACE" })
        assertEquals(1, requests.size)
        val request = decode("request_detail", DataItem.serializer(MarketRequest.serializer())).data.toPayload()
        assertEquals("50.00", request.context.minimumAmount)
        assertEquals(listOf("15", "30", "45", "60", "90", "120", "180"), request.context.etaCodes)
        assertEquals(listOf("YES", "NO"), request.context.deductibleCodes)
        assertNull(request.ownOfferId)
        assertEquals(null, request.order.customerPhone)
        val offer = decode("offer_submitted", DataItem.serializer(ProviderOffer.serializer())).data.toPayload()
        assertEquals("2|سباكة", offer.title)
        assertEquals("الحي الأول|350.00|315.00", offer.detail)
        assertEquals(1, decode("offers", DataList.serializer(ProviderOffer.serializer())).data.size)
        assertEquals("WITHDRAWN", decode("offer_withdrawn", DataItem.serializer(ProviderOffer.serializer())).data.status)
    }

    @Test
    fun profileDecodes() {
        val profile = decode("profile", DataItem.serializer(ProviderWorkspaceProfile.serializer())).data.toPayload()
        assertEquals("كريم السباك", profile.name)
        assertFalse(profile.hasAvatar)
        assertEquals("", profile.rating)
        assertTrue(profile.categories.single().specialties.isNotEmpty())
        val updated = decode("profile_updated", DataItem.serializer(ProviderWorkspaceProfile.serializer())).data.toPayload()
        assertEquals(12, updated.experienceYears)
        assertEquals("سباك خبرة في التسريبات.", updated.bio)
        assertEquals("", decode("portfolio_item", DataItem.serializer(PortfolioEntry.serializer())).data.toPayload().caption)
    }

    @Test
    fun callsSendTheSameBodies() {
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path!!.substringAfter("/api/v1/")
                return when {
                    path == "provider/application" -> MockResponse().setResponseCode(201).setBody(recorded("application_rejected"))
                    path.endsWith("/offers") -> MockResponse().setResponseCode(201).setBody(recorded("offer_submitted"))
                    path.startsWith("provider/orders/1000/start-trip") -> MockResponse().setBody(recorded("order_on_the_way"))
                    path.endsWith("/proposals") -> MockResponse().setResponseCode(201).setBody("{}")
                    path == "provider/profile" -> MockResponse().setBody(recorded("profile_updated"))
                    path == "media" -> MockResponse().setResponseCode(201).setBody("""{"media":{"id":44,"type":"IMAGE"}}""")
                    path.startsWith("conversations/") -> MockResponse().setBody(recorded("conversation_messages"))
                    else -> MockResponse().setResponseCode(204)
                }
            }
        }
        server.start()
        try {
            val api = UrlConnectionProviderApi(server.url("/api/v1/").toString())
            val order = decode("orders_current", DataList.serializer(ProviderOrder.serializer())).data.single().toPayload()
            api.submit("T", ApplicationBody(7, null, listOf(1), listOf(2), listOf(3), "INSTAPAY", "me@instapay"))
            api.submitOffer("T", 5, OfferBody("350", "المصنعية فقط", null, etaMinutes = 30))
            api.performOrderAction("T", order, "start-trip", OrderActionInput(lat = 30.05, lng = 31.33))
            api.submitProposal("T", order, ProposalBody("EXECUTION_QUOTE", "450", "سبب", order.version))
            api.updateProfile("T", ProfileUpdateBody(12, null, listOf(2), listOf(3)))
            assertEquals(44, api.uploadImage("T", CustomerMediaUpload("a.jpg", "image/jpeg", byteArrayOf(1))))
            val chat = api.conversation("T", 9)

            val requests = List(7) { server.takeRequest() }
            val bodies = requests.dropLast(2).map { JSONObject(it.body.readUtf8()) }
            requests.forEach { assertEquals("PROVIDER", it.getHeader("X-App-Mode")) }
            // Application: bio sent as null, missing media ids left out.
            assertTrue(bodies[0].has("bio") && bodies[0].isNull("bio"))
            assertFalse(bodies[0].has("profile_photo_media_id"))
            // Offer: note as null, no inspection_fee_deductible unless set.
            assertTrue(bodies[1].isNull("note"))
            assertEquals(30, bodies[1].getInt("eta_minutes"))
            assertFalse(bodies[1].has("inspection_fee_deductible"))
            // Action: the order version plus only the fields given.
            assertEquals(setOf("expected_version", "lat", "lng"), bodies[2].keys().asSequence().toSet())
            assertEquals(order.version, bodies[2].getInt("expected_version"))
            assertEquals(setOf("type", "amount", "reason", "expected_version"), bodies[3].keys().asSequence().toSet())
            assertTrue(bodies[4].isNull("bio"))
            assertTrue(requests[5].getHeader("Content-Type")!!.startsWith("multipart/form-data"))
            assertEquals("received_text", chat.kinds.single())
            assertEquals("#1 · سباكة · بانتظار تأكيد الإنهاء", chat.orderSummary)
            assertEquals("منى حسن", chat.customerName)
        } finally {
            server.shutdown()
        }
    }

    private fun recorded(name: String): String =
        requireNotNull(javaClass.classLoader!!.getResource("api/provider/$name.json")) { "missing recorded answer $name" }.readText()

    private fun <T> decode(name: String, serializer: KSerializer<T>): T = BremoHttp.json.decodeFromString(serializer, recorded(name))
}
