package noox.bzr.customer

import kotlinx.serialization.KSerializer
import noox.bzr.network.BremoHttp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DEC-061 / 9أ stage 2: every home and order model decodes the real server answers recorded by
 * tests/Feature/Api/MobileCustomerOrderResponsesTest.php (RECORD_API_RESPONSES=1).
 */
class CustomerOrderModelsDecodeTest {
    private val order = DataItem.serializer(CustomerOrder.serializer())

    @Test
    fun configDecodes() {
        val config = decode("config", AppConfig.serializer())
        assertEquals("EMPLOYEE", config.operatingMode)
        assertEquals("08:00", config.serviceHours.from)
        assertTrue(config.optionLists.getValue("customer_cancellation_reasons").any { it.code == "NO_LONGER_NEEDED" })
        assertTrue(config.optionDefaults.isNotEmpty())
    }

    @Test
    fun catalogAndAddressesDecode() {
        val catalog = decode("catalog", DataList.serializer(CatalogCategory.serializer())).data
        assertTrue(catalog.isNotEmpty())
        assertTrue(catalog.all { it.problemTypes.isNotEmpty() })
        assertTrue(decode("catalog_city", DataList.serializer(CatalogCategory.serializer())).data.isNotEmpty())
        val address = decode("addresses", DataList.serializer(CustomerAddress.serializer())).data.single()
        assertTrue(address.isDefault)
        assertEquals("البيت · الحي الأول", addressLabel(address))
    }

    @Test
    fun openOrderHasNoProviderOrAmounts() {
        val open = decode("order_open", order).data
        assertEquals("OPEN", open.status)
        assertNull(open.provider)
        assertNull(open.stepper)
        assertNull(open.amounts.finalAmount)
        assertEquals("UNPAID", open.amounts.paymentStatus)
        assertEquals(listOf("cancel"), open.availableActions)
    }

    @Test
    fun confirmedAndOnTheWayOrdersCarryTheProvider() {
        for (name in listOf("order_confirmed", "order_on_the_way")) {
            val decoded = decode(name, order).data
            assertNotNull(name, decoded.provider)
            val provider = decoded.provider!!
            assertEquals(name, "4.80", provider.ratingAvg)
            assertEquals(name, "01022222222", provider.phone)
            assertNotNull(name, decoded.stepper)
            assertNotNull(name, decoded.location.lat)
        }
    }

    @Test
    fun awaitingConfirmationAndClosedOrdersCarryAmounts() {
        val awaiting = decode("order_awaiting_confirmation", order).data
        assertEquals("AWAITING_CONFIRMATION", awaiting.status)
        assertEquals("450.00", awaiting.amounts.finalAmount)
        val closed = decode("order_closed", order).data
        assertEquals("CLOSED", closed.status)
        assertEquals("CASH", closed.amounts.paymentMethod)
        assertEquals("SUCCEEDED", closed.latestPayment?.status)
        assertNotNull(closed.termination.closedAt)
        assertTrue(closed.stepper!!.all { it.state == "done" })
    }

    @Test
    fun cancelledOrderCarriesItsReason() {
        val cancelled = decode("order_cancelled", order).data
        assertEquals("NO_LONGER_NEEDED", cancelled.termination.reasonCode)
        assertNotNull(cancelled.termination.reasonLabel)
        assertNull(cancelled.termination.note)
        assertTrue(cancelled.availableActions.isEmpty())
    }

    @Test
    fun ordersPagesDecode() {
        val current = decode("orders_current", OrdersPage.serializer())
        assertEquals(1, current.data.size)
        assertEquals(1, current.meta.currentPage)
        val past = decode("orders_past", OrdersPage.serializer())
        assertEquals(setOf("CLOSED", "CANCELLED"), past.data.map(CustomerOrder::status).toSet())
    }

    @Test
    fun trackingDecodes() {
        val tracking = decode("tracking_on_the_way", TrackingResponse.serializer())
        assertEquals("30.0590000", tracking.lastLocation?.lat)
        assertEquals(1, tracking.etaMinutes)
        assertTrue(tracking.etaApproximate)
    }

    private fun <T> decode(name: String, serializer: KSerializer<T>): T = BremoHttp.json.decodeFromString(serializer, recorded(name))

    companion object {
        fun recorded(name: String): String =
            requireNotNull(CustomerOrderModelsDecodeTest::class.java.classLoader!!.getResource("api/customer/$name.json")) {
                "missing recorded answer $name"
            }.readText()
    }
}
