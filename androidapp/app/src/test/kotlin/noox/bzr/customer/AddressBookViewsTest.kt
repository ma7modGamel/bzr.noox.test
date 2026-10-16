package noox.bzr.customer

import android.graphics.Rect
import android.view.View
import android.widget.LinearLayout
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.detectEnvironment
import com.android.resources.Density
import com.android.resources.LayoutDirection
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import noox.bzr.design.views.AppBottomSheetView
import noox.bzr.gallery.R
import noox.bzr.gallery.databinding.ItemAddressBinding
import noox.bzr.screens.ScreenSnapshots
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AddressBookViewsTest(private val width: Int, private val height: Int, private val locale: String, private val fontScale: Float) {
    @get:Rule val paparazzi = Paparazzi(
        environment = detectEnvironment().copy(compileSdkVersion = 34),
        deviceConfig = DeviceConfig.PIXEL_5.copy(screenWidth = width, screenHeight = height, density = Density.MEDIUM,
            fontScale = fontScale, layoutDirection = if (locale == "ar") LayoutDirection.RTL else LayoutDirection.LTR, locale = locale),
        theme = "Theme.Bremo",
    )

    private fun state(selection: Boolean = false): CustomerUiState {
        val labels = if (locale == "ar") listOf("العمل", "منزل العائلة", "المنزل الأساسي", "المكتب", "الشقة الأخرى") else listOf("Work", "Family home", "Primary home", "Office", "Other apartment")
        val actions = listOf(CustomerOption("edit", if (locale == "ar") "تعديل" else "Edit"), CustomerOption("set_default", if (locale == "ar") "تعيين كعنوان أساسي" else "Set as primary"), CustomerOption("delete", if (locale == "ar") "حذف" else "Delete"))
        return CustomerLogic.reduce("SCR-C14", mapOf(
            "event" to "loaded", "selection_mode" to selection, "address_labels" to labels,
            "address_details" to List(5) { if (locale == "ar") "دمياط الجديدة · الحي الأول · شارع الاختبار، مبنى ${it + 1}" else "New Damietta · First district · Test street, building ${it + 1}" },
            "primary_address_index" to 2, "selected_index" to 0, "address_ids" to (91..95).map(Int::toString),
            "address_actions" to List(5) { if (it == 2) actions.filter { action -> action.code != "set_default" } else actions },
        ))
    }

    @Test fun fiveAddressesStayCompactPrimaryIsFirstAndAddRemainsVisibleWhileScrolling() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C14AddressesView(paparazzi.context)
        var selected = -1
        view.onSelect = { selected = it }
        val management = C14AddressesView(paparazzi.context)
        management.render(state(selection = false))
        paparazzi.snapshot(management, name = "address-book-manage-$locale-$width-$fontScale")
        val state = state(selection = true)
        view.render(state)
        paparazzi.snapshot(view, name = "address-book-five-$locale-$width-$fontScale")
        val list = view.findViewById<RecyclerView>(R.id.list)
        assertEquals(5, list.adapter!!.itemCount)
        val first = ItemAddressBinding.bind((list.findViewHolderForAdapterPosition(0)!!.itemView as LinearLayout))
        assertEquals(state.items[2], first.label.text.toString())
        assertEquals(View.VISIBLE, first.primaryBadge.visibility)
        first.card.performClick()
        assertEquals(2, selected)
        if (fontScale == 1f) assertTrue("Normal address cards should remain compact", list.measuredHeight <= 5 * 160)
        val scroll = view.findViewById<NestedScrollView>(R.id.scroll)
        scroll.fullScroll(View.FOCUS_DOWN)
        paparazzi.snapshot(view, name = "address-book-bottom-$locale-$width-$fontScale")
        val add = view.findViewById<View>(R.id.add)
        val addRect = Rect(); add.getDrawingRect(addRect); view.offsetDescendantRectToMyCoords(add, addRect)
        assertTrue(addRect.top >= 0 && addRect.bottom <= view.height)
        val scrollRect = Rect(); scroll.getDrawingRect(scrollRect); view.offsetDescendantRectToMyCoords(scroll, scrollRect)
        assertTrue(scrollRect.bottom <= addRect.top)
    }

    @Test fun managementShowsPrimaryWithoutAnOrderSelection() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C14AddressesView(paparazzi.context)
        val management = state(selection = false)
        assertFalse(management.selectionMode)
        view.render(management)
        paparazzi.snapshot(view, name = "saved-addresses-management-$locale-$width-$fontScale")
        val list = view.findViewById<RecyclerView>(R.id.list)
        for (index in 0 until list.childCount) {
            val row = ItemAddressBinding.bind(list.getChildAt(index) as LinearLayout)
            assertFalse(row.card.isActivated)
        }
    }

    @Test fun confirmationNamesTheChosenAddressAndBusyStatePreventsRepeatedDeletion() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C14AddressesView(paparazzi.context)
        val state = state().copy(showConfirmation = true, pendingIndex = 2)
        var confirmations = 0
        view.onConfirmDelete = { confirmations++ }
        view.render(state)
        paparazzi.snapshot(view, name = "address-delete-confirm-$locale-$width-$fontScale")
        val overlay = view.findViewById<android.widget.FrameLayout>(R.id.overlay)
        val sheet = overlay.getChildAt(0) as AppBottomSheetView
        assertTrue(sheet.title.contains(state.items[2]))
        assertTrue(overlay.height == view.height)
        sheet.onAction()
        assertEquals(1, confirmations)
        view.render(state.copy(isBusy = true))
        sheet.onAction()
        assertEquals(1, confirmations)
        assertEquals(noox.bzr.design.ButtonVisualState.Loading, sheet.actionState)
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}x{1}-{2}-{3}") fun devices() = listOf(
            arrayOf<Any>(320, 568, "ar", 1f), arrayOf<Any>(375, 667, "ar", 1f),
            arrayOf<Any>(667, 375, "ar", 1f), arrayOf<Any>(375, 667, "en", 1f), arrayOf<Any>(375, 667, "ar", 2f),
        )
    }
}
