package noox.bzr.customer

import android.view.View
import android.widget.TextView
import android.widget.LinearLayout
import androidx.core.view.descendants
import noox.bzr.design.views.AppTextFieldView
import noox.bzr.design.views.SecondaryButtonView
import noox.bzr.provider.ProviderActiveOrderView
import noox.bzr.provider.ProviderLogic
import noox.bzr.screens.ScreenSnapshots
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class StaticDataSourcesViewsTest {
    @get:Rule val paparazzi = ScreenSnapshots.paparazzi()

    @Test fun budgetUsesServerAmountsAndHidesTheGuideWhenUnconfigured() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C38BudgetView(paparazzi.context)
        view.render(CustomerLogic.reduce("SCR-C38", mapOf("operating_mode" to "marketplace", "budget_amounts" to listOf("125.00", "225.00", "325.00"))))
        assertTrue(text(view).contains("125.00"))
        assertTrue(text(view).contains("325.00"))
        assertEquals(3, view.findViewById<LinearLayout>(noox.bzr.gallery.R.id.quickAmounts).childCount)
        paparazzi.snapshot(view, name = "backend-budget-guide")
        view.render(CustomerLogic.reduce("SCR-C38", mapOf("operating_mode" to "marketplace")))
        assertEquals(0, view.findViewById<LinearLayout>(noox.bzr.gallery.R.id.quickAmounts).childCount)
        assertEquals(View.GONE, view.findViewById<View>(noox.bzr.gallery.R.id.areaAverage).visibility)
        paparazzi.snapshot(view, name = "backend-budget-guide-unconfigured")
    }

    @Test fun timingHasNoFixedArrivalWindowBeforeTravel() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C04TimingView(paparazzi.context)
        view.render(CustomerLogic.reduce("SCR-C04", mapOf("operating_mode" to "staff", "now_available" to true, "category_available" to true, "timing_labels" to listOf("الآن", "موعد آخر"), "timing_codes" to listOf("NOW", "SCHEDULED"))))
        assertFalse(text(view).contains("30–45"))
        assertTrue(text(view).contains("بعد بدء تحرك الفني"))
        paparazzi.snapshot(view, name = "backend-timing-without-arrival-promise")
    }

    @Test fun reviewChoicesUseServerLabelsAndSendStableCodes() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C24RatingView(paparazzi.context)
        var selected = ""
        view.onTag = { _, code -> selected = code }
        val state = CustomerLogic.reduce("SCR-C24", mapOf("ratings" to listOf("5", "4", "5"), "available_actions" to listOf("rate"), "tag_labels" to listOf("حريص على الأثاث"), "tag_codes" to listOf("CAREFUL"), "tags" to listOf("CAREFUL")))
        view.render(state)
        assertTrue(text(view).contains("حريص على الأثاث"))
        assertFalse(text(view).contains("شغل متقن"))
        val chip = view.descendants.first { it.contentDescription?.toString()?.contains("حريص على الأثاث") == true || (it is TextView && it.text.toString() == "حريص على الأثاث") }
        var clickable: View? = chip
        while (clickable != null && !clickable.isClickable) clickable = clickable.parent as? View
        assertNotNull(clickable)
        clickable!!.performClick()
        assertEquals("CAREFUL", selected)
        paparazzi.snapshot(view, name = "backend-service-review-tags-top")
        view.findViewById<androidx.core.widget.NestedScrollView>(noox.bzr.gallery.R.id.scroll).fullScroll(View.FOCUS_DOWN)
        paparazzi.snapshot(view, name = "backend-service-review-tags")
    }

    @Test fun offerAvailabilityIsNotShownOnEveryCardAndSortsAreServerLabels() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C06OffersView(paparazzi.context)
        val rows = listOf("1|أحمد|4.8|20|300|المصنعية|20|الخامات عند العميل||true|1|سباكة|true", "2|محمود|4.5|12|250|المصنعية|30|الخامات عند العميل||true|2|سباكة|false")
        view.render(CustomerLogic.reduce("SCR-C06", mapOf("offer_rows" to rows, "timing_type" to "now", "sort_labels" to listOf("اختيار محسوب من الخادم", "السعر"), "sort_codes" to listOf("best", "price"))))
        val dots = view.descendants.filter { it.id == noox.bzr.design.R.id.availability }.toList()
        assertEquals(2, dots.size)
        assertEquals(View.VISIBLE, dots[0].visibility)
        assertEquals(View.GONE, dots[1].visibility)
        assertTrue(text(view).contains("اختيار محسوب من الخادم"))
        paparazzi.snapshot(view, name = "backend-offer-availability-and-sorts")
    }

    @Test fun executionShowsOnlyServerStagesTimesAndOptionalTechnicianEstimate() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C09TrackingView(paparazzi.context)
        val input = mapOf("status" to "IN_PROGRESS", "timing_type" to "now", "provider_name" to "أحمد", "order_title" to "صيانة حنفية", "execution_labels" to listOf("وصل الفني", "الإصلاح قيد التنفيذ", "انتهى الإصلاح"), "execution_states" to listOf("done", "active", "pending"), "execution_times" to listOf("14 أكتوبر 10:00", "14 أكتوبر 10:05", ""))
        view.render(CustomerLogic.reduce("SCR-C09", input))
        assertFalse(text(view).contains("انتهاء الإصلاح المتوقع"))
        assertFalse(text(view).contains("تم فحص المشكلة"))
        assertFalse(text(view).contains("تمت الموافقة على التكلفة"))
        assertTrue(text(view).contains("14 أكتوبر 10:05"))
        view.render(CustomerLogic.reduce("SCR-C09", input + mapOf("completion_estimate_at" to "14 أكتوبر 10:50")))
        assertTrue(text(view).contains("14 أكتوبر 10:50"))
        assertTrue(text(view).contains("تقدير يحدده الفني"))
        paparazzi.snapshot(view, name = "backend-execution-steps-and-estimate")
    }

    @Test fun technicianCanEnterAndSubmitAnEstimateWithoutChangingOrderState() {
        ScreenSnapshots.prepare(paparazzi)
        val view = ProviderActiveOrderView(paparazzi.context, "SCR-P15")
        var submitted: String? = null
        view.onCompletionEstimate = { submitted = it }
        view.render(ProviderLogic.reduce("SCR-P15", mapOf("available_actions" to listOf("update_completion_estimate"), "display_status" to "order.status.provider.IN_PROGRESS", "step_states" to listOf("done", "done", "done", "active", "pending", "pending"), "customer_name" to "عميل الاختبار", "order_summary" to listOf("#120", "صيانة حنفية", "الحي الأول", "الآن"))))
        val field = view.descendants.filterIsInstance<AppTextFieldView>().single { it.visibility == View.VISIBLE }
        val save = view.descendants.filterIsInstance<SecondaryButtonView>().single { it.text == paparazzi.context.getString(noox.bzr.design.R.string.provider_completion_estimate_save) }
        field.onValueChange!!.invoke("45")
        save.findViewById<View>(noox.bzr.design.R.id.button).performClick()
        assertEquals("45", submitted)
        submitted = null
        field.onValueChange!!.invoke("0")
        save.findViewById<View>(noox.bzr.design.R.id.button).performClick()
        assertNull(submitted)
        assertNotNull(field.error)
        field.onValueChange!!.invoke("45")
        paparazzi.snapshot(view, name = "backend-technician-completion-estimate-top")
        view.findViewById<androidx.core.widget.NestedScrollView>(noox.bzr.gallery.R.id.scroll).fullScroll(View.FOCUS_DOWN)
        paparazzi.snapshot(view, name = "backend-technician-completion-estimate")
    }

    @Test fun failedReviewKeepsTheSelectedCodesAndCommentAndAllowsRetry() {
        val state = CustomerLogic.reduce("SCR-C24", mapOf("event" to "error", "ratings" to listOf("5", "4", "5"), "comment" to "شكرا", "tags" to listOf("CAREFUL"), "available_actions" to listOf("rate")))
        assertEquals("error.body", state.messageKey)
        assertEquals(listOf("CAREFUL"), state.items)
        assertEquals("شكرا", state.fieldValues.first())
        assertTrue(state.canContinue)
    }

    private fun text(view: android.view.ViewGroup): String = view.descendants.filterIsInstance<TextView>().filter { it.visibility == View.VISIBLE }.joinToString("\n") { it.text.toString() }
}
