package noox.bzr.customer

import kotlinx.serialization.KSerializer
import noox.bzr.customer.CustomerOrderModelsDecodeTest.Companion.recorded
import noox.bzr.network.BremoHttp
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DEC-061 / 9أ stage 3, group C: addresses, cities, areas and slots decode the real server answers
 * (MobileCustomerAddressResponsesTest), and saving sends the same body as the JSONObject code did.
 */
class CustomerAddressModelsDecodeTest {
    @Test
    fun referencesAndSlotsDecode() {
        assertEquals("دمياط الجديدة", decode("cities", DataList.serializer(IdName.serializer())).data.single().name)
        assertTrue(decode("areas", DataList.serializer(IdName.serializer())).data.size >= 6)
        val slots = decode("slots", DataList.serializer(SlotRange.serializer())).data
        assertEquals(6, slots.size)
        assertTrue(slots.first().start < slots.first().end)
    }

    @Test
    fun savedAddressDecodes() {
        val address = decode("address_saved", DataItem.serializer(CustomerAddress.serializer())).data
        assertEquals("البيت", address.label)
        assertEquals("الحي الأول · شارع النيل، عمارة 7", address.detail())
        assertNull(address.landmark)
        assertEquals(31.4368, address.lat!!.toDouble(), 0.0001)
        assertTrue(address.isDefault)
    }

    @Test
    fun saveSendsTheFullBodyAndPicksTheMethod() {
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = MockResponse().setResponseCode(201).setBody(recorded("address_saved"))
        }
        server.start()
        try {
            val api = UrlConnectionCustomerApi(server.url("/api/v1/").toString())
            val body = AddressBody("البيت", 1, 2, "شارع النيل", "7", "3", "12", "", 31.4368, 31.667, true)
            api.saveAddress("T", null, body)
            api.saveAddress("T", 9, body)

            val create = server.takeRequest()
            val update = server.takeRequest()
            assertEquals("POST" to "addresses", create.method to create.path!!.substringAfter("/api/v1/"))
            assertEquals("PATCH" to "addresses/9", update.method to update.path!!.substringAfter("/api/v1/"))
            val sent = JSONObject(create.body.readUtf8())
            assertEquals(
                setOf("label", "city_id", "area_id", "address_text", "building", "floor", "apartment", "landmark", "lat", "lng", "is_default"),
                sent.keys().asSequence().toSet(),
            )
            assertEquals("", sent.getString("landmark"))
            assertTrue(sent.getBoolean("is_default"))
        } finally {
            server.shutdown()
        }
    }

    private fun <T> decode(name: String, serializer: KSerializer<T>): T = BremoHttp.json.decodeFromString(serializer, recorded(name))
}
