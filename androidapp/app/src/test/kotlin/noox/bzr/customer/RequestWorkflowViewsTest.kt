package noox.bzr.customer

import android.view.View
import android.widget.LinearLayout
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.views.PrimaryButtonView
import noox.bzr.screens.ScreenSnapshots
import noox.bzr.gallery.R
import noox.bzr.design.R as DesignR
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RequestWorkflowViewsTest {
    @get:Rule val paparazzi = ScreenSnapshots.paparazzi()

    @Test fun staffCanSelectMaterialsAndContinueDirectlyToReview() {
        ScreenSnapshots.prepare(paparazzi)
        val input = ScreenSnapshots.cases("SCR-C04").first { it.first == "staff_scheduled" }.second
        val view = C04TimingView(paparazzi.context)
        var material = -1
        var route = ""
        view.onMaterialSelect = { material = it }
        view.onOpen = { route = it }
        view.render(CustomerLogic.reduce("SCR-C04", input))

        val choices = view.findViewById<LinearLayout>(R.id.materials)
        assertEquals(3, choices.childCount)
        choices.getChildAt(0).performClick()
        assertEquals(0, material)
        val next = view.findViewById<PrimaryButtonView>(R.id.next)
        assertEquals(ButtonVisualState.Normal, next.state)
        next.findViewById<View>(DesignR.id.button).performClick()
        assertEquals("SCR-C05", route)
        paparazzi.snapshot(view, name = "staff-four-steps-materials")
        view.findViewById<androidx.core.widget.NestedScrollView>(R.id.scroll).fullScroll(View.FOCUS_DOWN)
        paparazzi.snapshot(view, name = "staff-material-options")
    }

    @Test fun marketplaceKeepsItsBudgetStepAndHidesInlineMaterials() {
        ScreenSnapshots.prepare(paparazzi)
        val input = ScreenSnapshots.cases("SCR-C04").first { it.first == "marketplace_now" }.second
        val view = C04TimingView(paparazzi.context)
        var route = ""
        view.onOpen = { route = it }
        view.render(CustomerLogic.reduce("SCR-C04", input))

        assertEquals(View.GONE, view.findViewById<View>(R.id.materialsSection).visibility)
        view.findViewById<PrimaryButtonView>(R.id.next).findViewById<View>(DesignR.id.button).performClick()
        assertEquals("SCR-C38", route)
        paparazzi.snapshot(view, name = "marketplace-five-steps-timing")
    }
}
