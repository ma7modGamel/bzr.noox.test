package noox.bzr.customer

import kotlinx.serialization.KSerializer
import noox.bzr.auth.AccountResponse
import noox.bzr.customer.CustomerOrderModelsDecodeTest.Companion.recorded
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
 * DEC-061 / 9أ stage 3, groups D and F: payment, disputes, help, notifications, account and terms decode the
 * real server answers (MobileCustomerAccountResponsesTest), and the calls send the expected bodies.
 */
class CustomerSupportModelsDecodeTest {
    @Test
    fun answersDecode() {
        val payment = decode("payment_created", DataItem.serializer(PaymentAttempt.serializer())).data
        assertEquals("KIOSK", payment.channel)
        assertTrue(payment.referenceNumber!!.isNotBlank())
        val dispute = decode("dispute_opened", DataItem.serializer(CustomerDispute.serializer())).data
        assertEquals("جودة التنفيذ غير مرضية", dispute.reasonLabel)
        assertNull(dispute.resolutionNote)
        assertEquals(1, decode("disputes", DataList.serializer(CustomerDispute.serializer())).data.size)
        assertTrue(decode("faqs", DataList.serializer(FaqItem.serializer())).data.isNotEmpty())
        val notifications = decode("notifications", DataList.serializer(NotificationItem.serializer())).data
        assertEquals(2, notifications.size)
        assertTrue(notifications.all { it.readAt == null && it.deepLink!!.startsWith("bremo://") })
        assertFalse(decode("me_updated", AccountResponse.serializer()).user.ratingRemindersEnabled)
        val terms = decode("terms", DataItem.serializer(TermsPage.serializer())).data
        assertTrue(terms.version > 0 && terms.body.isNotBlank())
    }

    @Test
    fun callsSendTheExpectedBodies() {
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path!!.substringAfter("/api/v1/")
                return when {
                    path.endsWith("/payments") -> MockResponse().setResponseCode(201).setBody(recorded("payment_created"))
                    path == "me" && request.getHeader("X-HTTP-Method-Override") == "PATCH" -> MockResponse().setBody(recorded("me_updated"))
                    else -> MockResponse().setResponseCode(204)
                }
            }
        }
        server.start()
        try {
            val api = UrlConnectionCustomerApi(server.url("/api/v1/").toString())
            api.createPayment("T", 7, "KIOSK", 3)
            api.submitReview("T", 7, 5, 4, 5, null)
            api.reportProvider("T", 2, 7, "OFF_PLATFORM_REQUEST", null)
            api.unregisterDevice("T", "push")
            api.logout("T")
            api.updateRatingReminders("T", false)

            val requests = List(6) { server.takeRequest() }
            assertEquals(
                listOf("POST orders/7/payments", "POST orders/7/review", "POST providers/2/reports", "DELETE me/devices", "POST auth/logout", "PATCH me"),
                requests.map { "${it.getHeader("X-HTTP-Method-Override") ?: it.method} ${it.path!!.substringAfter("/api/v1/")}" },
            )
            val bodies = requests.map { JSONObject(it.body.readUtf8().ifBlank { "{}" }) }
            assertEquals(3, bodies[0].getInt("expected_version"))
            assertTrue(bodies[1].has("comment") && bodies[1].isNull("comment"))
            assertEquals(7, bodies[2].getInt("order_id"))
            assertEquals("push", bodies[3].getString("token"))
            assertEquals(0, bodies[4].length())
            assertFalse(bodies[5].getBoolean("rating_reminders_enabled"))
        } finally {
            server.shutdown()
        }
    }

    private fun <T> decode(name: String, serializer: KSerializer<T>): T = BremoHttp.json.decodeFromString(serializer, recorded(name))
}
