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
            assertEquals("PATCH" to "addresses/9", update.getHeader("X-HTTP-Method-Override") to update.path!!.substringAfter("/api/v1/"))
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

    @Test
    fun deleteAcceptsBackendJsonSuccess() {
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                if (request.method == "POST" && request.getHeader("X-HTTP-Method-Override") == "DELETE") {
                    MockResponse().setResponseCode(200).setBody("""{"data":{"deleted_id":9,"default_address":null}}""")
                } else MockResponse().setResponseCode(403).setBody("<html>Forbidden by hosting method policy</html>")
        }
        server.start()
        try {
            val deleted = UrlConnectionCustomerApi(server.url("/api/v1/").toString()).deleteAddress("T", 9)
            assertEquals(9, deleted.deletedId)
            assertNull(deleted.defaultAddress)
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("DELETE", request.getHeader("X-HTTP-Method-Override"))
            assertEquals("/api/v1/addresses/9", request.path)
        } finally { server.shutdown() }
    }

    @Test
    fun settingPrimaryDecodesAllAddressFlagsActionsAndFullDisplayFromBackend() {
        val server = MockWebServer()
        val preferred = JSONObject(recorded("address_saved")).getJSONObject("data")
        preferred.put("display_address", "Full address supplied by server")
        preferred.put("actions", org.json.JSONArray("""[{"code":"edit","label":"Server edit"},{"code":"delete","label":"Server delete"}]"""))
        server.enqueue(MockResponse().setBody(JSONObject().put("data", preferred).put("addresses", org.json.JSONArray().put(preferred)).toString()))
        server.start()
        try {
            val update = UrlConnectionCustomerApi(server.url("/api/v1/").toString()).makeAddressDefault("T", 9)
            assertTrue(update.data.isDefault)
            assertEquals("Server delete", update.addresses.single().actions[1].label)
            assertEquals("Full address supplied by server", update.addresses.single().detail())
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/v1/addresses/9/default", request.path)
            assertNull(request.getHeader("X-HTTP-Method-Override"))
        } finally { server.shutdown() }
    }

    private fun <T> decode(name: String, serializer: KSerializer<T>): T = BremoHttp.json.decodeFromString(serializer, recorded(name))
}
