package noox.bzr.customer

import android.view.View
import noox.bzr.gallery.R
import noox.bzr.provider.P15InProgressView
import noox.bzr.provider.ProviderLogic
import noox.bzr.screens.ScreenSnapshots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RepairAndPaymentViewsTest {
    @get:Rule val paparazzi = ScreenSnapshots.paparazzi()

    private fun transfer(receipt: Boolean, status: String = "") = mapOf(
        "event" to "loaded", "is_instapay" to true, "channel_labels" to listOf("إنستاباي"),
        "selected_channel_index" to 0, "transfer_reference" to "IPN-778812", "receipt_uploaded" to receipt,
        "instapay_address" to "demo@instapay", "instapay_name" to "حساب توضيحي", "order_reference" to "#1042",
        "total" to "700 جنيه", "available_actions" to listOf("submit_instapay_transfer"), "payment_status" to status,
    )

    @Test fun instapayRequiresReceiptAndPendingVerificationLocksSubmission() {
        ScreenSnapshots.prepare(paparazzi)
        assertFalse(CustomerLogic.reduce("SCR-C22", transfer(false)).canContinue)
        assertTrue(CustomerLogic.reduce("SCR-C22", transfer(true)).canContinue)
        assertFalse(CustomerLogic.reduce("SCR-C22", transfer(true, "PENDING_VERIFICATION")).canContinue)
        val view = C22ElectronicPaymentView(paparazzi.context)
        view.render(CustomerLogic.reduce("SCR-C22", transfer(true)))
        paparazzi.snapshot(view, name = "instapay-receipt-form")
        view.findViewById<androidx.core.widget.NestedScrollView>(R.id.scroll).fullScroll(View.FOCUS_DOWN)
        paparazzi.snapshot(view, name = "instapay-submit-receipt")
        view.render(CustomerLogic.reduce("SCR-C22", transfer(true, "PENDING_VERIFICATION")))
        paparazzi.snapshot(view, name = "instapay-awaiting-admin")
    }

    @Test fun reviewPhotosRemainOptionalAndUploadDisablesSubmission() {
        ScreenSnapshots.prepare(paparazzi)
        val input = mapOf("event" to "loaded", "ratings" to listOf("5", "5", "5"), "available_actions" to listOf("rate"), "photo_count" to 0,
            "provider_name" to "أحمد محمد", "order_title" to "إصلاح تسريب")
        assertTrue(CustomerLogic.reduce("SCR-C24", input).canContinue)
        assertFalse(CustomerLogic.reduce("SCR-C24", input + ("event" to "uploading")).canContinue)
        val view = C24RatingView(paparazzi.context)
        view.render(CustomerLogic.reduce("SCR-C24", input + ("photo_count" to 2)))
        paparazzi.snapshot(view, name = "review-with-optional-photos")
        view.findViewById<androidx.core.widget.NestedScrollView>(R.id.scroll).fullScroll(View.FOCUS_DOWN)
        paparazzi.snapshot(view, name = "review-optional-photos-input")
    }

    @Test fun technicianSeesBothPhotoPhasesAndWarrantyUsesItsOwnForm() {
        ScreenSnapshots.prepare(paparazzi)
        val view = P15InProgressView(paparazzi.context)
        view.render(ProviderLogic.reduce("SCR-P15", mapOf("event" to "loaded", "display_status" to "order.status.provider.IN_PROGRESS",
            "customer_name" to "محمد", "order_summary" to listOf("#1042", "إصلاح تسريب", "الحي الأول", "الآن"),
            "available_actions" to listOf("complete_work", "upload_repair_before", "upload_repair_after"), "before_photo_count" to 1, "after_photo_count" to 0, "step_states" to listOf("done", "done", "done", "active", "pending", "pending"))))
        assertEquals(1, ProviderLogic.reduce("SCR-P15", mapOf("before_photo_count" to 1)).beforePhotoCount)
        paparazzi.snapshot(view, name = "technician-required-repair-photos")
        view.findViewById<androidx.core.widget.NestedScrollView>(R.id.scroll).fullScroll(View.FOCUS_DOWN)
        paparazzi.snapshot(view, name = "technician-repair-photo-controls")
        val input = mapOf("warranty" to true, "description" to "عاد التسريب بعد إصلاحه في نفس المكان", "available_actions" to listOf("request_warranty"))
        assertTrue(CustomerLogic.reduce("SCR-C27", input).canContinue)
        val warranty = C27DisputeView(paparazzi.context)
        warranty.render(CustomerLogic.reduce("SCR-C27", input))
        paparazzi.snapshot(warranty, name = "fourteen-day-warranty-request")
    }
}
