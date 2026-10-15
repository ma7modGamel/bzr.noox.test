package noox.bzr.customer

import kotlinx.serialization.json.Json
import noox.bzr.provider.ProviderOrder
import noox.bzr.provider.ProviderLogic
import noox.bzr.provider.P15InProgressView
import noox.bzr.screens.ScreenSnapshots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WarrantyVisitViewsTest {
    @get:Rule val paparazzi = ScreenSnapshots.paparazzi()
    private val json = Json { ignoreUnknownKeys = true }
    private val coverage = "زيارة ضمان: الزيارة والمصنعية مجانًا. الخامات خارج الضمان وتحتاج موافقة العميل على تكلفتها قبل التنفيذ."

    @Test fun providerReceivesWarrantyCoverageAndMaterialsChoicesFromTheServer() {
        val order = json.decodeFromString<ProviderOrder>("""{"id":8,"number":1043,"version":1,"status":"IN_PROGRESS","warranty_coverage_text":"$coverage","proposal_type_options":[{"code":"MATERIALS","label":"خامات"}],"available_actions":["submit_proposal","complete_work"]}""")
        assertEquals(coverage, order.toPayload().warrantyCoverage)
        assertEquals("MATERIALS", order.toPayload().proposalTypeOptions.single().code)
        ScreenSnapshots.prepare(paparazzi)
        val view = P15InProgressView(paparazzi.context)
        view.render(ProviderLogic.reduce("SCR-P15", mapOf("warranty_coverage" to coverage, "display_status" to "order.status.provider.IN_PROGRESS",
            "customer_name" to "محمد", "order_summary" to listOf("#1043", "زيارة ضمان لإصلاح تسريب", "الحي الأول", "موعد مجدول"),
            "step_states" to listOf("done", "done", "done", "active", "pending", "pending"), "available_actions" to order.availableActions,
            "before_photo_count" to 1, "after_photo_count" to 1)))
        paparazzi.snapshot(view, name = "warranty-technician-free-labor")
    }

    @Test fun customerCanFollowTheVisitAndSeesOnlyMaterialsAsPayable() {
        val claim = json.decodeFromString<CustomerDispute>("""{"id":3,"reason_code":"WARRANTY","reason_label":"ضمان الإصلاح 14 يومًا","description":"عاد التسريب في نفس المكان","status":"ASSIGNED","visit_order_id":8}""")
        val state = CustomerLogic.reduce("SCR-C27", mapOf("warranty" to true, "warranty_visit_id" to claim.visitOrderId, "existing" to true,
            "details" to listOf(claim.reasonLabel, claim.description), "status_details" to listOf("تم تنسيق زيارة فني بديل", "أحمد · 15 أكتوبر، 11 صباحًا")))
        assertEquals(8, state.warrantyVisitId)
        assertTrue(state.isWarranty)
        ScreenSnapshots.prepare(paparazzi)
        val follow = C27DisputeView(paparazzi.context)
        follow.render(state)
        paparazzi.snapshot(follow, name = "warranty-follow-replacement-visit")
        val payment = C21PaymentSummaryView(paparazzi.context)
        payment.render(CustomerLogic.reduce("SCR-C21", mapOf("warranty_coverage" to coverage, "amounts" to listOf("0", "120", "120"),
            "payment_method" to "CASH", "method_labels" to listOf("كاش"), "selected_method_index" to 0, "available_actions" to listOf("change_payment_method"))))
        paparazzi.snapshot(payment, name = "warranty-materials-only-payment")
    }
}
