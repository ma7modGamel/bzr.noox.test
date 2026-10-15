package noox.bzr.customer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.descendants
import noox.bzr.design.views.AppTextFieldView
import noox.bzr.design.views.SecondaryButtonView
import noox.bzr.provider.ProviderActiveOrderView
import noox.bzr.provider.ProviderLogic
import noox.bzr.screens.ScreenSnapshots
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class StaffMediaAndArrivalViewsTest {
    @get:Rule val paparazzi = ScreenSnapshots.paparazzi()
    private fun fixture(): Bitmap = Bitmap.createBitmap(320, 160, Bitmap.Config.ARGB_8888).apply {
        val canvas = Canvas(this)
        canvas.drawColor(Color.LTGRAY)
        val paint = Paint().apply { color = Color.DKGRAY; textSize = 24f }
        canvas.drawText("TEST IMAGE", 64f, 80f, paint)
    }
    private fun labels(view: android.view.ViewGroup) = view.descendants.filterIsInstance<TextView>().filter { it.visibility == View.VISIBLE }.joinToString("\n") { it.text.toString() }

    @Test fun chatShowsAllAttachedPrivateImagesAlongsideTheMessage() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C19ChatView(paparazzi.context)
        val loaded = mutableListOf<String>()
        view.loadImage = { url, ready -> loaded += url; ready(fixture()) }
        view.render(CustomerLogic.reduce("SCR-C19", mapOf("conversation_status" to "OPEN", "provider_name" to "أحمد", "message_bodies" to listOf("صورة العطل الحالي"), "message_kinds" to listOf("received_image"), "message_times" to listOf("10:30"), "image_groups" to listOf("https://example.test/a\nhttps://example.test/b"))))
        paparazzi.snapshot(view, name = "staff-chat-private-images")
        assertEquals(listOf("https://example.test/a", "https://example.test/b"), loaded)
        assertEquals(2, view.descendants.filterIsInstance<AuthenticatedPhotoView>().count())
    }

    @Test fun imageFailureHasRetryAndOldCallbackCannotReplaceANewerImage() {
        ScreenSnapshots.prepare(paparazzi)
        val view = AuthenticatedPhotoView(paparazzi.context)
        var old: ((Bitmap?) -> Unit)? = null
        view.loadImage = { _, ready -> old = ready }
        view.bind("https://example.test/old")
        view.loadImage = { _, ready -> ready(null) }
        view.bind("https://example.test/new")
        assertTrue(labels(view).contains("تعذّر تحميل الصورة"))
        old!!.invoke(fixture())
        assertTrue(labels(view).contains("تعذّر تحميل الصورة"))
        view.loadImage = { _, ready -> ready(fixture()) }
        view.descendants.filterIsInstance<SecondaryButtonView>().single().onClick()
        assertTrue(view.descendants.filterIsInstance<ImageView>().single().visibility == View.VISIBLE)
        paparazzi.snapshot(view, name = "staff-image-retry")
    }

    @Test fun quoteShowsOnlyOptionalCurrentFaultImageAndNoCompletionEvidence() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C30ExecutionQuoteView(paparazzi.context)
        view.loadImage = { _, ready -> ready(fixture()) }
        val input = mapOf("amount" to "300", "reason" to "تسريب أسفل الحوض؛ المطلوب تغيير الوصلة", "total" to "300", "available_actions" to listOf("approve_proposal", "reject_proposal"), "provider_name" to "أحمد")
        view.render(CustomerLogic.reduce("SCR-C30", input + mapOf("has_photo" to true)))
        assertEquals(0, view.descendants.filterIsInstance<AuthenticatedPhotoView>().count())
        view.render(CustomerLogic.reduce("SCR-C30", input + mapOf("proposal_photo_url" to "https://example.test/fault")))
        assertEquals(1, view.descendants.filterIsInstance<AuthenticatedPhotoView>().count())
        assertTrue(labels(view).contains("صورة العطل الحالي"))
        assertFalse(labels(view).contains("بعد الإصلاح"))
        paparazzi.snapshot(view, name = "staff-current-fault-before-price-approval")
    }

    @Test fun completedOrderHistoryDisplaysBeforeAndAfterPhotosFromServer() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C26OrderHistoryView(paparazzi.context)
        view.loadImage = { _, ready -> ready(fixture()) }
        view.render(CustomerLogic.reduce("SCR-C26", mapOf("status" to "CLOSED", "display_status" to "CLOSED", "summary" to listOf("#120", "سباكة", "صيانة حنفية", "الحي الأول", "15 أكتوبر"), "work_before_urls" to listOf("https://example.test/before"), "work_after_urls" to listOf("https://example.test/after"))))
        assertEquals(2, view.descendants.filterIsInstance<AuthenticatedPhotoView>().count())
        paparazzi.snapshot(view, name = "staff-completed-order-details")
        view.findViewById<androidx.core.widget.NestedScrollView>(noox.bzr.gallery.R.id.scroll).fullScroll(View.FOCUS_DOWN)
        paparazzi.snapshot(view, name = "staff-completed-order-before-after")
    }

    @Test fun arrivalCodeIsCustomerOnlyDuringTravelAndHasRefreshAction() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C09TrackingView(paparazzi.context)
        var action = ""
        view.onAction = { action = it }
        view.render(CustomerLogic.reduce("SCR-C09", mapOf("status" to "ON_THE_WAY", "provider_name" to "أحمد", "arrival_code_required" to true, "arrival_code" to "123456", "arrival_code_until" to "15 أكتوبر 10:40")))
        assertTrue(labels(view).contains("123456"))
        view.descendants.filterIsInstance<SecondaryButtonView>().single { it.text == paparazzi.context.getString(noox.bzr.design.R.string.arrival_code_refresh) }.onClick()
        assertEquals("refresh_arrival_code", action)
        paparazzi.snapshot(view, name = "staff-customer-arrival-code-top")
        view.findViewById<androidx.core.widget.NestedScrollView>(noox.bzr.gallery.R.id.scroll).fullScroll(View.FOCUS_DOWN)
        paparazzi.snapshot(view, name = "staff-customer-arrival-code")
        view.render(CustomerLogic.reduce("SCR-C09", mapOf("status" to "ARRIVED")))
        assertFalse(labels(view).contains("123456"))
    }

    @Test fun technicianCanEnterCustomerCodeOrReportAnExceptionToAdministration() {
        ScreenSnapshots.prepare(paparazzi)
        val view = ProviderActiveOrderView(paparazzi.context, "SCR-P13")
        var code = ""
        var action = ""
        view.onArrivalCodeChange = { code = it }
        view.onAction = { action = it }
        view.render(ProviderLogic.reduce("SCR-P13", mapOf("arrival_code_required" to true, "available_actions" to listOf("mark_arrived", "report_arrival_exception"), "display_status" to "order.status.provider.ON_THE_WAY", "step_states" to listOf("done", "active", "pending", "pending", "pending", "pending"), "order_summary" to listOf("#120", "صيانة حنفية", "الحي الأول", "الآن"))))
        val fields = view.descendants.filterIsInstance<AppTextFieldView>().filter { it.visibility == View.VISIBLE }.toList()
        fields.single { it.label == paparazzi.context.getString(noox.bzr.design.R.string.arrival_code_input) }.onValueChange!!.invoke("123456")
        assertEquals("123456", code)
        fields.single { it.label == paparazzi.context.getString(noox.bzr.design.R.string.arrival_exception_note) }.onValueChange!!.invoke("العميل غير موجود في المكان")
        view.descendants.filterIsInstance<SecondaryButtonView>().single { it.text == paparazzi.context.getString(noox.bzr.design.R.string.arrival_exception_report) }.onClick()
        assertEquals("report_arrival_exception", action)
        paparazzi.snapshot(view, name = "staff-technician-arrival-code-and-exception")
    }
    @Test fun publishedPolicyDisplaysTheApprovedWarrantyAndMessagingRules() {
        ScreenSnapshots.prepare(paparazzi)
        val body = javaClass.classLoader!!.getResource("api/customer/staff_terms_published.json")!!.readText()
        val page = org.json.JSONObject(body).getJSONObject("data")
        val terms = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString(TermsPage.serializer(), page.toString())
        val view = C34TermsView(paparazzi.context)
        view.render(CustomerLogic.reduce("SCR-C34", mapOf("body" to terms.displayBody, "version" to page.getInt("version").toString(), "effective_at" to page.getString("effective_at"))))
        assertTrue(labels(view).contains("الزيارة والمصنعية فقط"))
        assertTrue(labels(view).contains("بالرسائل داخل التطبيق فقط"))
        assertTrue(labels(view).contains("تأكيد وصول الفني"))
        assertTrue(labels(view).contains("سياسة الإلغاء"))
        assertTrue(labels(view).contains("سياسة الخصوصية"))
        paparazzi.snapshot(view, name = "staff-published-terms-top")
        val text = view.descendants.filterIsInstance<TextView>().first { it.text.toString().contains("ضمان الإصلاح لمدة 14") }
        val offset = text.text.toString().indexOf("ضمان الإصلاح لمدة 14")
        val line = text.layout.getLineForOffset(offset)
        view.findViewById<androidx.core.widget.NestedScrollView>(noox.bzr.gallery.R.id.scroll).scrollTo(0, text.top + text.layout.getLineTop(line))
        paparazzi.snapshot(view, name = "staff-published-warranty-and-arrival-policy")
    }

}
