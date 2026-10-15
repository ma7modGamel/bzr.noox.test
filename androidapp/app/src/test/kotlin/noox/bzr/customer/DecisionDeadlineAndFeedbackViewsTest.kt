package noox.bzr.customer

import android.view.View
import android.widget.TextView
import androidx.core.view.descendants
import com.google.android.material.button.MaterialButton
import noox.bzr.design.views.PrimaryButtonView
import noox.bzr.design.views.SelectableChipView
import noox.bzr.design.views.TextAreaFieldView
import noox.bzr.provider.P17CustomerRatingView
import noox.bzr.provider.ProviderLogic
import noox.bzr.screens.ScreenSnapshots
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class DecisionDeadlineAndFeedbackViewsTest {
    @get:Rule val paparazzi = ScreenSnapshots.paparazzi()

    @Test fun proposalShowsTheRealDeadlineAndExpiryOutcomeAndPreventsLateDecisions() {
        ScreenSnapshots.prepare(paparazzi)
        val input = mapOf("amount" to "120", "original_amount" to "500", "total" to "620", "reason" to "قطعة غيار لإصلاح التسريب", "type_label" to "خامات",
            "provider_name" to "أحمد محمد", "provider_category" to "سباكة", "countdown_seconds" to 1800,
            "proposal_deadline" to "2026-10-13 · 4:30 م", "proposal_expiry_message" to "انتهاء المهلة يرفض هذا البند دون إلغاء الطلب أو إضافة تكلفته.",
            "available_actions" to listOf("approve_proposal", "reject_proposal", "chat"))
        val state = CustomerLogic.reduce("SCR-C31", input)
        val view = C31AdditionalCostView(paparazzi.context)
        view.render(state)
        assertTrue(text(view).contains("30:00"))
        assertTrue(text(view).contains("دون إلغاء الطلب"))
        paparazzi.snapshot(view, name = "additional-cost-real-decision-deadline")
        var action = ""
        view.onAction = { action = it }
        // The order's old available_actions cannot keep a decision open after the local countdown ends.
        view.render(state.copy(countdownSeconds = 0))
        assertEquals(1, view.descendants.filterIsInstance<PrimaryButtonView>().count())
        view.descendants.filterIsInstance<PrimaryButtonView>().first().descendants.filterIsInstance<MaterialButton>().first().performClick()
        assertEquals("", action)
        val reject = view.descendants.filterIsInstance<MaterialButton>().first { it.text.toString() == "رفض التكلفة" }
        assertFalse(reject.isEnabled)
        reject.performClick()
        assertEquals("", action)
        assertTrue(text(view).contains("انتهت"))
        paparazzi.snapshot(view, name = "additional-cost-expired-no-cancellation")
        val quote = C30ExecutionQuoteView(paparazzi.context)
        quote.render(CustomerLogic.reduce("SCR-C30", input + mapOf("amount" to "500", "total" to "500", "type_label" to "عرض التنفيذ", "reason" to "إصلاح التسريب",
            "proposal_expiry_message" to "انتهاء المهلة يغلق المعاينة المجانية دون تنفيذ أو دفع.")))
        paparazzi.snapshot(quote, name = "execution-quote-real-decision-deadline")
    }

    @Test fun ratingShowsServerTagsAndCommentAndRetainsSelectionsWhenEditing() {
        ScreenSnapshots.prepare(paparazzi)
        val input = mapOf("stars" to 4, "order_summary" to listOf("#1044 • إصلاح تسريب", "محمد أحمد", "تم الإغلاق"), "tag_labels" to listOf("تعاون أثناء الزيارة", "التزام بالموعد"),
            "tag_codes" to listOf("COOPERATIVE", "PUNCTUAL"), "selected_tags" to listOf("0"), "comment" to "العميل متعاون وملتزم بالموعد", "available_actions" to listOf("rate_customer"))
        val state = ProviderLogic.reduce("SCR-P17", input)
        val view = P17CustomerRatingView(paparazzi.context)
        view.render(state)
        var tag = -1
        var comment = ""
        view.onTag = { tag = it }
        view.onComment = { comment = it }
        view.descendants.filterIsInstance<SelectableChipView>().last().performClick()
        assertEquals(1, tag)
        view.descendants.filterIsInstance<TextAreaFieldView>().single().onValueChange?.invoke("تعليق جديد")
        assertEquals("تعليق جديد", comment)
        assertEquals(listOf("COOPERATIVE", "PUNCTUAL"), state.optionCodes)
        assertEquals(listOf(0), state.selectedPrimaryIndices)
        assertTrue(state.canContinue)
        paparazzi.snapshot(view, name = "provider-customer-rating-tags-comment")
        view.render(state.copy(messageKey = "error.body"))
        assertEquals("العميل متعاون وملتزم بالموعد", view.descendants.filterIsInstance<TextAreaFieldView>().single().value)
        assertTrue(text(view).contains("تعذّر الاتصال بالخادم"))
        val invalid = ProviderLogic.reduce("SCR-P17", input + ("comment" to "س".repeat(501)))
        assertFalse(invalid.canContinue)
        assertTrue("provider.rating.comment_limit" in invalid.fieldErrors)
    }

    @Test fun countdownUsesElapsedTimeAndNeverRestartsAfterReturningToTheScreen() {
        val state = CustomerLogic.reduce("SCR-C31", mapOf("countdown_seconds" to 60, "proposal_loaded_uptime" to 100))
        assertEquals(50, CustomerLogic.proposalRemaining(state, 110))
        assertEquals(0, CustomerLogic.proposalRemaining(state, 170))
    }

    private fun text(view: android.view.ViewGroup) = view.descendants.filterIsInstance<TextView>().filter { it.visibility == View.VISIBLE }.joinToString("\n") { it.text.toString() }
}
