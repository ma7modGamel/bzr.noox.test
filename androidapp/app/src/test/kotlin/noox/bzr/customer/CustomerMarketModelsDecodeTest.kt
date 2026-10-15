package noox.bzr.customer

import kotlinx.serialization.KSerializer
import noox.bzr.customer.CustomerOrderModelsDecodeTest.Companion.recorded
import noox.bzr.network.BremoHttp
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DEC-061 / 9أ stage 3, group A: offers, the provider profile and proposals decode the real server answers
 * (MobileCustomerMarketResponsesTest, MobileCustomerOrderResponsesTest), and the calls send the same paths.
 */
class CustomerMarketModelsDecodeTest {
    @Test
    fun offersDecode() {
        val offers = decode("offers", DataList.serializer(CustomerOffer.serializer())).data
        assertEquals(2, offers.size)
        val first = offers.first { it.etaMinutes != null }
        assertEquals("450.00", first.price)
        assertEquals("تغيير الخرطوم وتركيب الخلاط", first.includesText)
        assertEquals("4.80", first.provider.ratingAvg)
        assertEquals(12, first.provider.completedOrders)
        val second = offers.first { it.etaMinutes == null }
        assertNull(second.includesText)
        assertEquals("يمكن الحضور بعد ساعة", second.note)
    }

    @Test
    fun providerContextDecodes() {
        val provider = decode("provider_context", DataItem.serializer(ProviderContext.serializer())).data
        assertEquals("كريم السباك", provider.name)
        assertTrue(provider.isVerified)
        assertEquals(9, provider.experienceYears)
        assertEquals("4.50", provider.ratingBreakdown.punctuality)
        assertEquals(listOf("تسريب مياه"), provider.specialties.map { it.name })
        assertEquals(4, provider.reviews.single().punctuality)
        assertTrue("accept_offer" in provider.availableActions)
    }

    @Test
    fun proposalsDecode() {
        val pending = decode("proposals_pending", DataList.serializer(CustomerProposal.serializer())).data.single()
        assertEquals("PENDING", pending.status)
        assertEquals("EXECUTION_QUOTE", pending.type)
        assertEquals("450.00", pending.projectedTotal)
        assertNull(pending.photoPath)
        assertTrue(pending.expiresAt!!.isNotBlank())
        val approved = decode("proposals_approved", DataList.serializer(CustomerProposal.serializer())).data.single()
        assertEquals("APPROVED", approved.status)
    }

    @Test
    fun callsSendTheSamePaths() {
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = MockResponse().setBody(
                when {
                    request.path!!.contains("/offers") -> recorded("offers")
                    request.path!!.contains("/providers/") -> recorded("provider_context")
                    else -> recorded("proposals_pending")
                },
            )
        }
        server.start()
        try {
            val api = UrlConnectionCustomerApi(server.url("/api/v1/").toString())
            assertEquals(2, api.offers("T", 7, "price").size)
            assertEquals("كريم السباك", api.provider("T", 3, 7).name)
            assertFalse(api.proposals("T", 7).isEmpty())
            val paths = List(3) { server.takeRequest() }.map { it.path!!.substringAfter("/api/v1/") to it.getHeader("Authorization") }
            assertEquals(
                listOf("orders/7/offers?sort=price" to "Bearer T", "providers/3?order_id=7" to "Bearer T", "orders/7/proposals" to "Bearer T"),
                paths,
            )
        } finally {
            server.shutdown()
        }
    }

    private fun <T> decode(name: String, serializer: KSerializer<T>): T = BremoHttp.json.decodeFromString(serializer, recorded(name))
}
