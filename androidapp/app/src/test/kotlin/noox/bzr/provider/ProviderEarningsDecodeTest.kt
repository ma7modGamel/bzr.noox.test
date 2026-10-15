package noox.bzr.provider

import kotlinx.serialization.KSerializer
import noox.bzr.network.BremoHttp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DEC-061 / DEC-068: the earnings model decodes the real server answers recorded by
 * tests/Feature/Api/MobileProviderEarningsResponsesTest.php (RECORD_API_RESPONSES=1).
 */
class ProviderEarningsDecodeTest {
    @Test
    fun employeeEarningsDecode() {
        val earnings = decode("earnings_employee", ProviderEarningsResponse.serializer()).data
        assertEquals("EMPLOYEE", earnings.operatingMode)
        assertEquals("-350.00", earnings.summary.balance)
        assertFalse(earnings.dues.blocked)
        assertNull(earnings.dues.limit)
        assertEquals(listOf("REMITTANCE", "ORDER"), earnings.transactions.map { it.type })

        val payload = earnings.toPayload()
        assertEquals(listOf("-350.00", "-350.00", "0.00"), payload.summary)
        assertFalse(payload.duesBlocked)
    }

    @Test
    fun blockedMarketplaceEarningsDecode() {
        val payload = decode("earnings_marketplace_blocked", ProviderEarningsResponse.serializer()).data.toPayload()
        assertEquals("MARKETPLACE", payload.operatingMode)
        assertTrue(payload.duesBlocked)
        assertEquals("650.00", payload.duesDebt)
        assertEquals("500.00", payload.duesLimit)
        assertEquals(listOf("عمولة مستحقة للمنصة · 650.00 جنيه"), payload.transactionDetails)
    }

    private fun <T> decode(name: String, serializer: KSerializer<T>): T {
        val text = checkNotNull(javaClass.classLoader?.getResource("api/provider/$name.json")).readText()
        return BremoHttp.json.decodeFromString(serializer, text)
    }
}
