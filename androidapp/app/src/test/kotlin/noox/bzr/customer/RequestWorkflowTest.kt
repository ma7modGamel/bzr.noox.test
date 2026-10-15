package noox.bzr.customer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class RequestWorkflowTest {
    private fun timing(mode: String = "staff", material: Int = 2, available: Boolean = true) = mapOf<String, Any?>(
        "operating_mode" to mode, "timing_type" to "now", "address_id" to "12",
        "selected_material_index" to material, "now_available" to available,
        "material_labels" to listOf("From server"), "material_codes" to listOf("UNSURE"),
    )

    @Test fun staffIncludesMaterialsAndHasFourSteps() {
        val state = CustomerLogic.reduce("SCR-C04", timing())
        assertEquals(4, state.requestSteps)
        assertTrue(state.showMaterials)
        assertTrue(state.canContinue)
        assertFalse(CustomerLogic.reduce("SCR-C04", timing(material = -1)).canContinue)
    }

    @Test fun marketplaceRetainsItsSeparateMaterialStep() {
        val state = CustomerLogic.reduce("SCR-C04", timing(mode = "marketplace", material = -1))
        assertEquals(5, state.requestSteps)
        assertFalse(state.showMaterials)
        assertTrue(state.canContinue)
    }

    @Test fun unavailableNowIsDisabledButAScheduledChoiceRemainsUsable() {
        assertFalse(CustomerLogic.reduce("SCR-C04", timing(available = false)).canContinue)
        val state = CustomerLogic.reduce("SCR-C04", timing(available = false) + mapOf(
            "timing_type" to "scheduled", "slot_id" to "44", "availability_failed" to true))
        assertFalse(state.nowAvailable)
        assertTrue(state.canContinue)
    }

    @Test fun cachedCallActionsAreNeverPresented() {
        val state = CustomerLogic.reduce("SCR-C09", mapOf(
            "status" to "ARRIVED", "available_actions" to listOf("call", "chat")))
        assertFalse(state.visibleActions.contains("call"))
        assertTrue(state.visibleActions.contains("chat"))
    }
}
