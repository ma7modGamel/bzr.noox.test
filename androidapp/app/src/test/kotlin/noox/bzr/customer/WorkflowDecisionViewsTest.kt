package noox.bzr.customer

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.descendants
import com.google.android.material.button.MaterialButton
import noox.bzr.design.views.PrimaryButtonView
import noox.bzr.screens.ScreenSnapshots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkflowDecisionViewsTest {
    @get:Rule val paparazzi = ScreenSnapshots.paparazzi()

    @Test fun waitingShowsTheServerDeadlineAndRefreshesWithoutSimulatedTechniciansOrEta() {
        ScreenSnapshots.prepare(paparazzi)
        val staff = C36AssignmentView(paparazzi.context)
        val info = "طلبك في انتظار تعيين فني من الإدارة.\nمهلة تعيين الفني: 2026-10-13 · 4:00 م"
        val state = CustomerLogic.reduce("SCR-C36", mapOf("waiting_info" to info, "order_title" to "سباكة — تسريب مياه", "order_rows" to listOf("#1044", "الحي الأول", "الآن"), "available_actions" to listOf("cancel")))
        staff.render(state)
        var action = ""
        staff.onAction = { action = it }
        staff.descendants.filterIsInstance<PrimaryButtonView>().first().descendants.filterIsInstance<MaterialButton>().first().performClick()
        assertEquals("follow", action)
        assertEquals(info, state.waitingInfo)
        paparazzi.snapshot(staff, name = "actual-staff-assignment-deadline")
        val market = C35NoOffersView(paparazzi.context)
        market.render(CustomerLogic.reduce("SCR-C35", mapOf("waiting_info" to "طلبك مفتوح لاستقبال عروض الفنيين.\nنهاية استقبال العروض: 2026-10-13 · 3:30 م", "timing_type" to "now", "summary_rows" to listOf("إصلاح تسريب", "الحي الأول", "", "500"), "available_actions" to listOf("edit_request", "cancel"))))
        val text = market.descendants.filterIsInstance<TextView>().filter { visible(it) }.joinToString("\n") { it.text }
        assertFalse(text.contains("فنيين قريبين"))
        assertFalse(text.contains("30–45"))
        assertTrue(text.contains("نهاية استقبال العروض"))
        paparazzi.snapshot(market, name = "actual-market-offer-deadline")
    }

    @Test fun customerCanApproveTheRealPendingExtraCostAndCannotApproveAnExpiredProposal() {
        ScreenSnapshots.prepare(paparazzi)
        val input = mapOf("type_label" to "أعمال إضافية", "amount" to "120", "reason" to "تغيير جزء إضافي لإصلاح التسريب", "original_amount" to "500", "total" to "620", "countdown_seconds" to 1800,
            "provider_name" to "أحمد محمد", "provider_category" to "سباكة", "available_actions" to listOf("approve_proposal", "reject_proposal", "chat"))
        val view = C31AdditionalCostView(paparazzi.context)
        val state = CustomerLogic.reduce("SCR-C31", input)
        view.render(state)
        var action = ""
        view.onAction = { action = it }
        assertTrue(state.canContinue)
        view.descendants.filterIsInstance<PrimaryButtonView>().first().descendants.filterIsInstance<MaterialButton>().first().performClick()
        assertEquals("approve_proposal", action)
        paparazzi.snapshot(view, name = "customer-approve-extra-cost")
        val expired = CustomerLogic.reduce("SCR-C31", input + ("expired" to true))
        assertFalse(expired.canContinue)
        view.render(expired)
        action = ""
        view.descendants.filterIsInstance<PrimaryButtonView>().first().descendants.filterIsInstance<MaterialButton>().first().performClick()
        assertEquals("", action)
    }

    @Test fun trackingOffersSharingAndShowsPreparationOrAnError() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C09TrackingView(paparazzi.context)
        val state = CustomerLogic.reduce("SCR-C09", mapOf("status" to "CONFIRMED", "available_actions" to listOf("share_visit", "chat"), "provider_name" to "أحمد", "order_number" to "1044", "order_title" to "إصلاح تسريب", "timing_type" to "NOW"))
        view.render(state.copy(isBusy = true))
        assertTrue(view.descendants.filterIsInstance<TextView>().any { visible(it) && it.text.toString().contains("جارٍ تجهيز رابط الزيارة") })
        paparazzi.snapshot(view, name = "visit-share-preparation")
        view.render(state.copy(messageKey = "share.error"))
        assertTrue(view.descendants.filterIsInstance<TextView>().any { visible(it) && it.text.toString().contains("تعذّر تجهيز رابط الزيارة") })
    }

    private fun visible(view: View): Boolean = view.visibility == View.VISIBLE && ((view.parent as? View)?.let(::visible) ?: true)
}
