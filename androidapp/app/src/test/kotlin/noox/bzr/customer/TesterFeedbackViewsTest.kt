package noox.bzr.customer

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.descendants
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton
import noox.bzr.design.views.AppBottomSheetView
import noox.bzr.screens.ScreenSnapshots
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TesterFeedbackViewsTest {
    @get:Rule val paparazzi = ScreenSnapshots.paparazzi()

    @Test fun providerCardRequiresBackendPermission() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C02AccountView(paparazzi.context)
        val state = CustomerLogic.reduce("SCR-C02", mapOf("name" to "Customer", "available_actions" to listOf("logout")))
        view.render(state)
        assertFalse(text(view).contains("وضع الفني"))
        view.render(state.copy(visibleActions = listOf("open_provider_mode", "logout")))
        assertTrue(text(view).contains("وضع الفني"))
    }

    @Test fun cancellationDockReservesSpaceAndNotesCanScrollAboveIt() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C20CancellationView(paparazzi.context)
        view.render(CustomerLogic.reduce("SCR-C20", mapOf("reason_labels" to (1..10).map { "Reason $it" }, "selected_reason_index" to 0, "note" to "Test note")))
        val width = 320
        val height = 568
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)
        val sheet = view.descendants.filterIsInstance<AppBottomSheetView>().single()
        val scroll = view.findViewById<NestedScrollView>(noox.bzr.gallery.R.id.scroll)
        val sheetRect = android.graphics.Rect()
        val scrollRect = android.graphics.Rect()
        sheet.getDrawingRect(sheetRect); view.offsetDescendantRectToMyCoords(sheet, sheetRect)
        scroll.getDrawingRect(scrollRect); view.offsetDescendantRectToMyCoords(scroll, scrollRect)
        assertTrue("Scroll body must end above confirmation dock", scrollRect.bottom <= sheetRect.top)
        scroll.fullScroll(View.FOCUS_DOWN)
        assertTrue(scroll.scrollY > 0)
    }

    @Test fun logoutSheetsUseExplicitConfirmAndCancelActionsOnBothAccountScreens() {
        ScreenSnapshots.prepare(paparazzi)
        val account = C02AccountView(paparazzi.context)
        var confirmed = false
        var dismissed = false
        account.onConfirmLogout = { confirmed = true }
        account.onDismissLogout = { dismissed = true }
        account.render(CustomerUiState("SCR-C02", CustomerPhase.Content))
        assertFalse(visible(account.descendants.filterIsInstance<AppBottomSheetView>().single()))
        account.render(CustomerUiState("SCR-C02", CustomerPhase.Content, showLogoutConfirmation = true))
        assertFalse(confirmed)
        val sheet = account.descendants.filterIsInstance<AppBottomSheetView>().single()
        assertTrue(visible(sheet))
        sheet.descendants.filterIsInstance<MaterialButton>().last().performClick()
        assertTrue(dismissed)
        assertFalse(confirmed)
        sheet.descendants.filterIsInstance<MaterialButton>().first().performClick()
        assertTrue(confirmed)
        val settings = C33AccountSettingsView(paparazzi.context)
        var action = ""
        settings.onAction = { action = it }
        settings.render(CustomerLogic.reduce("SCR-C33", mapOf("available_actions" to listOf("logout"))).copy(showLogoutConfirmation = true))
        val settingsSheet = settings.descendants.filterIsInstance<AppBottomSheetView>().single()
        assertTrue(settingsSheet.title.contains("تسجيل الخروج"))
        settingsSheet.descendants.filterIsInstance<MaterialButton>().first().performClick()
        assertEquals("confirm_logout", action)
        settingsSheet.descendants.filterIsInstance<MaterialButton>().last().performClick()
        assertEquals("dismiss_logout", action)
    }

    @Test fun logoutKeepsSessionUntilConfirmationAndClearsItOnceConfirmed() {
        val loggedOut = java.util.concurrent.CountDownLatch(1)
        val session = object : noox.bzr.auth.AuthSessionStore {
            override var token: String? = "T"
            override var email: String? = "employee@example.test"
            override fun clear() { token = null; email = null }
        }
        val api = java.lang.reflect.Proxy.newProxyInstance(CustomerApi::class.java.classLoader, arrayOf(CustomerApi::class.java)) { _, method, _ ->
            if (method.name == "logout") { loggedOut.countDown(); Unit } else throw AssertionError(method.name)
        } as CustomerApi
        val model = CustomerViewModel(api, session, "EGP")
        model.show("SCR-C02", mapOf("available_actions" to listOf("logout")))
        model.requestLogoutConfirmation()
        assertEquals("T", session.token)
        assertFalse(model.requiresAuthentication)
        model.dismissLogoutConfirmation()
        model.confirmLogout()
        assertEquals("T", session.token)
        model.show("SCR-C33", mapOf("available_actions" to listOf("logout")))
        model.accountAction("logout")
        assertEquals("T", session.token)
        model.accountAction("confirm_logout")
        assertNull(session.token)
        assertTrue(model.requiresAuthentication)
        assertTrue(loggedOut.await(2, java.util.concurrent.TimeUnit.SECONDS))
    }

    @Test fun addressDeletionFailurePreservesListAndClosesBusySheet() {
        val state = CustomerLogic.reduce("SCR-C14", mapOf("event" to "delete_failed", "address_labels" to listOf("Home", "Work"), "selected_index" to 1))
        assertEquals(CustomerPhase.Content, state.phase)
        assertEquals(listOf("Home", "Work"), state.items)
        assertEquals(1, state.selectedIndex)
        assertEquals("address.delete.failed", state.messageKey)
        assertFalse(state.showConfirmation)
        assertFalse(state.isBusy)
    }

    private fun text(view: ViewGroup) = view.descendants.filterIsInstance<TextView>().filter(::visible).joinToString("\n") { it.text }
    private fun visible(view: View): Boolean = view.visibility == View.VISIBLE && ((view.parent as? View)?.let(::visible) ?: true)
}
