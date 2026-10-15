package noox.bzr.provider

import android.view.View
import android.widget.TextView
import androidx.core.view.descendants
import noox.bzr.design.views.SecondaryButtonView
import noox.bzr.design.views.SelectableChipView
import noox.bzr.screens.ScreenSnapshots
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class EarningsStatementViewsTest {
    @get:Rule val paparazzi = ScreenSnapshots.paparazzi()

    @Test fun filtersUseServerChoicesAndExportWaitsUntilDraftFiltersAreApplied() {
        ScreenSnapshots.prepare(paparazzi)
        val state = ProviderLogic.reduce("SCR-P18", mapOf("operating_mode" to "EMPLOYEE", "summary" to listOf("-350 ج", "-350 ج", "0 ج"), "transaction_titles" to listOf("طلب #1044", "توريد نقدي"), "transaction_details" to listOf("نقدية مطلوب توريدها · 650 جنيه", "تم توريد 300 جنيه"), "transaction_states" to listOf("ORDER", "REMITTANCE")))
            .copy(earningsFilterLabels = listOf("كل الحركات", "طلبات مكتملة", "تحويلات المنصة", "توريدات نقدية"), earningsFilterCodes = listOf("ALL", "ORDER", "PAYOUT", "REMITTANCE"), earningsTypeIndex = 0, earningsDateFrom = "2026-10-01", earningsDateTo = "2026-10-13", earningsHasMore = true)
        val view = P18EarningsView(paparazzi.context)
        view.render(state)
        var selected = -1
        var exports = 0
        view.onType = { selected = it }
        view.onExport = { exports++ }
        view.descendants.filterIsInstance<SelectableChipView>().last().performClick()
        assertEquals(3, selected)
        val export = view.findViewById<SecondaryButtonView>(noox.bzr.gallery.R.id.exportStatement)
        assertTrue(export.enabled)
        export.onClick()
        assertEquals(1, exports)
        paparazzi.snapshot(view, name = "earnings-filters-statement-export")
        view.findViewById<androidx.core.widget.NestedScrollView>(noox.bzr.gallery.R.id.scroll).fullScroll(View.FOCUS_DOWN)
        paparazzi.snapshot(view, name = "earnings-filtered-transactions-pagination")
        view.render(state.copy(earningsFiltersDirty = true))
        assertFalse(export.enabled)
        assertFalse(view.findViewById<SecondaryButtonView>(noox.bzr.gallery.R.id.loadMore).enabled)
        view.render(state.copy(messageKey = "provider.earnings.filter_error"))
        assertTrue(view.descendants.filterIsInstance<TextView>().any { it.text.toString().contains("طلب #1044") })
    }

    @Test fun anEmptyFilteredResultStillOffersResetAndExport() {
        ScreenSnapshots.prepare(paparazzi)
        val view = P18EarningsView(paparazzi.context)
        val state = ProviderLogic.reduce("SCR-P18", mapOf("summary" to listOf("0", "0", "0"))).copy(earningsFilterLabels = listOf("كل الحركات"))
        view.render(state)
        assertTrue(view.findViewById<SecondaryButtonView>(noox.bzr.gallery.R.id.resetFilters).enabled)
        assertTrue(view.findViewById<SecondaryButtonView>(noox.bzr.gallery.R.id.exportStatement).enabled)
    }
}
