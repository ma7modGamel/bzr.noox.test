package noox.bzr.design.views

import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import app.cash.paparazzi.Paparazzi
import noox.bzr.design.FieldImeAction
import noox.bzr.design.FieldVisualState
import noox.bzr.design.R
import noox.bzr.design.SelectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InputAccessibilityTest {
    @get:Rule
    val paparazzi = Paparazzi(theme = "Theme.Bremo")

    @Test
    fun app_text_field_links_the_label_and_exposes_error_and_ime_states() {
        val field = AppTextFieldView(paparazzi.context).apply {
            label = "البريد الإلكتروني"
            error = "البريد غير صحيح"
            state = FieldVisualState.Error
        }
        val label = field.findViewById<TextView>(R.id.label)
        val edit = field.findViewById<EditText>(R.id.edit)
        val error = field.findViewById<TextView>(R.id.error)

        assertEquals(edit.id, label.labelFor)
        assertEquals(View.VISIBLE, error.visibility)
        assertEquals("البريد غير صحيح", error.text.toString())
        assertEquals(EditorInfo.IME_ACTION_NEXT, edit.imeOptions)

        field.imeAction = FieldImeAction.Done

        assertEquals(EditorInfo.IME_ACTION_DONE, edit.imeOptions)
    }

    @Test
    @Suppress("DEPRECATION")
    fun selectable_chip_exposes_selection_and_keeps_a_48dp_touch_target() {
        val chip = SelectableChipView(paparazzi.context).apply {
            text = "اليوم"
            state = SelectionState.Selected
        }
        val width = View.MeasureSpec.makeMeasureSpec(120, View.MeasureSpec.EXACTLY)
        val height = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        chip.measure(width, height)
        val node = AccessibilityNodeInfo.obtain()
        chip.onInitializeAccessibilityNodeInfo(node)

        assertTrue(chip.measuredHeight >= chip.resources.getDimensionPixelSize(R.dimen.bremo_size_touch_target_min))
        assertTrue(chip.isSelected)
        assertTrue(node.isCheckable)
        assertTrue(node.isChecked)
        assertEquals("اليوم", node.contentDescription)

        node.recycle()
    }
}
