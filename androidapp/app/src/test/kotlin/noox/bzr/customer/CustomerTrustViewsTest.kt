package noox.bzr.customer

import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.descendants
import noox.bzr.screens.ScreenSnapshots
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class CustomerTrustViewsTest {
    @get:Rule val paparazzi = ScreenSnapshots.paparazzi()

    @Test fun accountShowsCompletedAndActiveTotalsWithoutAnInventedRating() {
        ScreenSnapshots.prepare(paparazzi)
        val state = CustomerLogic.reduce("SCR-C02", mapOf("name" to "محمد أحمد", "phone" to "01012345678", "is_verified" to true, "completed_orders" to "24", "active_orders" to "4", "address_count" to "2"))
        val view = C02AccountView(paparazzi.context)
        view.render(state)
        val text = text(view)
        assertTrue(text.contains("24"))
        assertTrue(text.contains("طلبات نشطة"))
        assertFalse(text.contains("4.9"))
        assertFalse(text.contains("تقييمك"))
        paparazzi.snapshot(view, name = "customer-real-account-statistics")
    }

    @Test fun profileLoadsServerImageUrlsAndShowsOnlyIdentityVerification() {
        ScreenSnapshots.prepare(paparazzi)
        val imageFile = generateSequence(File(".").absoluteFile) { it.parentFile }.map { File(it, "design/images/home-hero.jpg") }.first { it.exists() }
        val image = BitmapFactory.decodeFile(imageFile.absolutePath)
        assertNotNull(image)
        val urls = mutableListOf<String>()
        val view = C07ProviderView(paparazzi.context)
        view.loadImage = { url, complete -> urls += url; complete(image) }
        val state = CustomerLogic.reduce("SCR-C07", mapOf("provider_name" to "أحمد محمد", "provider_verified" to true, "provider_available" to true,
            "provider_rating" to "4.8", "provider_services" to "35", "provider_experience" to "8", "provider_category" to "سباكة", "provider_about" to "متخصص في إصلاح السباكة المنزلية",
            "provider_avatar_url" to "https://example.test/profile", "portfolio_urls" to listOf("https://example.test/work"), "portfolio_captions" to listOf("صورة توضيحية للاختبار"), "available_actions" to listOf("chat")))
        view.render(state)
        assertEquals(listOf("https://example.test/profile", "https://example.test/work"), urls)
        assertTrue(view.descendants.filterIsInstance<ImageView>().any { it.tag == "https://example.test/profile" && it.drawable != null })
        val text = text(view)
        assertFalse(text.contains("سرعة الرد"))
        assertFalse(text.contains("سجل جنائي"))
        assertFalse(text.contains("مهارة"))
        paparazzi.snapshot(view, name = "technician-real-images-identity-only")
        view.findViewById<androidx.core.widget.NestedScrollView>(noox.bzr.gallery.R.id.scroll).fullScroll(View.FOCUS_DOWN)
        paparazzi.snapshot(view, name = "technician-identity-verification")
        view.loadImage = { _, complete -> complete(null) }
        view.render(state.copy(providerAvatarUrl = "https://example.test/missing"))
        assertTrue(view.descendants.filterIsInstance<ImageView>().filter { it.tag == "https://example.test/missing" }.none { visible(it) })
    }

    @Test fun messagesShowUnreadCountsAndAvailabilityThenFilterUnreadConversations() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C18MessagesView(paparazzi.context)
        val state = CustomerLogic.reduce("SCR-C18", mapOf("provider_names" to listOf("أحمد", "محمود"), "order_summaries" to listOf("سباكة • جاري التنفيذ", "كهرباء • تم الإغلاق"),
            "conversation_statuses" to listOf("OPEN", "READ_ONLY"), "last_messages" to listOf("وصلت إلى العنوان", "شكرًا لك"), "unread_counts" to listOf("3", "0"), "providers_available" to listOf("true", "false")))
        view.render(state)
        assertEquals(listOf(3, 0), state.unreadCounts)
        assertEquals(listOf(true, false), state.providersAvailable)
        assertTrue(text(view).contains("متاح الآن"))
        assertFalse(text(view).contains("متصل الآن"))
        paparazzi.snapshot(view, name = "messages-real-unread-availability")
        view.descendants.filterIsInstance<TextView>().first { it.text.toString() == "غير مقروءة" }.parent.let { (it as View).performClick() }
        assertTrue(text(view).contains("أحمد"))
        assertFalse(text(view).contains("محمود"))
    }

    private fun text(view: android.view.ViewGroup): String = view.descendants.filterIsInstance<TextView>().filter(::visible).joinToString("\n") { it.text.toString() }
    private fun visible(view: View): Boolean = view.visibility == View.VISIBLE && ((view.parent as? View)?.let(::visible) ?: true)
}
