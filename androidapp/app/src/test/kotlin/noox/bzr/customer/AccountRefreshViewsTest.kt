package noox.bzr.customer

import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.core.view.descendants
import noox.bzr.design.views.MenuRowView
import noox.bzr.design.views.SecondaryButtonView
import noox.bzr.gallery.R
import noox.bzr.screens.RefreshScrollView
import noox.bzr.screens.ScreenSnapshots
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AccountRefreshViewsTest {
    @get:Rule val paparazzi = ScreenSnapshots.paparazzi()

    private fun settings(mode: String = "overview") = CustomerLogic.reduce("SCR-C33", mapOf(
        "event" to "loaded", "mode" to mode, "field_values" to listOf("Customer", "customer@example.test", "01012345678", ""),
        "available_actions" to listOf("update_account", "change_password", "logout"),
    ))

    @Test fun eachPersonalDetailOpensEditAndPhotoHasItsOwnEntry() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C33AccountSettingsView(paparazzi.context)
        val actions = mutableListOf<String>()
        var picked = false
        view.onAction = { actions.add(it) }
        view.onPickPhoto = { picked = true }
        view.render(settings())
        listOf(R.id.name, R.id.email, R.id.phone).forEach { id ->
            val row = view.findViewById<MenuRowView>(id)
            row.performClick()
        }
        assertEquals(List(3) { "update_account" }, actions)
        view.findViewById<SecondaryButtonView>(R.id.changePhoto).onClick()
        assertTrue(picked)
        paparazzi.snapshot(view, name = "personal-details")
    }

    @Test fun emailCanBeEditedAndPasswordIsRequiredOnlyWhenChangingEmail() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C33AccountSettingsView(paparazzi.context)
        val values = mutableListOf<Pair<Int, String>>()
        view.onFieldChange = { index, value -> values.add(index to value) }
        val state = settings("edit")
        view.render(state)
        assertEquals(noox.bzr.design.FieldVisualState.Empty, view.findViewById<noox.bzr.design.views.AppTextFieldView>(R.id.editEmail).state)
        assertEquals(View.GONE, view.findViewById<View>(R.id.emailPassword).visibility)
        val changed = CustomerLogic.reduce("SCR-C33", mapOf("mode" to "edit", "email_changed" to true, "field_values" to state.fieldValues))
        assertFalse(changed.canContinue)
        view.render(changed)
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.emailPassword).visibility)
        view.findViewById<noox.bzr.design.views.AppTextFieldView>(R.id.editEmail).onValueChange?.invoke("new@example.test")
        assertEquals(1 to "new@example.test", values.last())
    }

    @Test fun pullRefreshWorksFromTopAndIsDisabledWhileEditingOrBusy() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C33AccountSettingsView(paparazzi.context)
        var refreshes = 0
        view.refreshEnabled = true
        view.onPullRefresh = { refreshes += 1 }
        view.render(settings())
        view.measure(View.MeasureSpec.makeMeasureSpec(375, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(667, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, 375, 667)
        val scroll = view.findViewById<RefreshScrollView>(R.id.scroll)
        fun pull() {
            val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 120f, 60f, 0)
            val up = MotionEvent.obtain(0, 200, MotionEvent.ACTION_UP, 120f, 560f, 0)
            scroll.dispatchTouchEvent(down); scroll.dispatchTouchEvent(up)
            down.recycle(); up.recycle()
        }
        pull()
        assertEquals(1, refreshes)
        view.render(settings("edit")); pull()
        assertEquals(1, refreshes)
        view.render(settings().copy(isBusy = true)); pull()
        assertEquals(1, refreshes)
        view.render(settings().copy(isRefreshing = true)); pull()
        assertEquals(1, refreshes)
    }

    @Test fun refreshFeedbackKeepsTheDataVisibleAndAssignmentFollowShowsAcknowledgement() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C36AssignmentView(paparazzi.context)
        var followed = false
        view.onAction = { followed = it == "follow" }
        val state = CustomerLogic.reduce("SCR-C36", mapOf("summary_rows" to listOf("#1044", "Area", "Today"), "waiting_info" to "Admin is assigning a technician"))
        view.render(state.copy(isRefreshing = true))
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.refreshProgress).visibility)
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.content).visibility)
        view.render(state.copy(refreshMessageKey = "refresh.updated"))
        assertEquals(View.VISIBLE, view.findViewById<View>(R.id.refreshFeedback).visibility)
        view.findViewById<noox.bzr.design.views.PrimaryButtonView>(R.id.follow).onClick()
        assertTrue(followed)
        paparazzi.snapshot(view, name = "assignment-refreshed")
    }

    @Test fun backFromEditReturnsToOverviewBeforeLeavingSettings() {
        val session = object : noox.bzr.auth.AuthSessionStore {
            override var token: String? = "T"
            override var email: String? = "customer@example.test"
            override fun clear() { token = null }
        }
        val api = java.lang.reflect.Proxy.newProxyInstance(CustomerApi::class.java.classLoader, arrayOf(CustomerApi::class.java)) { _, method, _ -> throw AssertionError(method.name) } as CustomerApi
        val model = CustomerViewModel(api, session, "EGP")
        model.show("SCR-C33", mapOf("event" to "loaded"))
        model.accountAction("update_account")
        model.updateAccountField(0, "Unsaved")
        assertFalse(model.canRefresh)
        model.refreshCurrent()
        assertEquals("Unsaved", model.state.fieldValues[0])
        assertTrue(model.goBack())
        assertEquals("SCR-C33", model.state.screen)
        assertEquals(listOf("overview"), model.state.items)
    }

    @Test fun invalidPhoneOrEmailPreventsSaving() {
        val state = CustomerLogic.reduce("SCR-C33", mapOf("mode" to "edit", "field_values" to listOf("Customer", "invalid", "123", "")))
        assertFalse(state.canContinue)
        assertTrue(state.fieldErrors.containsAll(listOf("account.validation.email", "account.validation.phone")))
    }
}
