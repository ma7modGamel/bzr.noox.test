package noox.bzr.customer

import android.text.InputType
import noox.bzr.auth.AuthActions
import noox.bzr.auth.AuthLogic
import noox.bzr.auth.CustomerRegisterScreen
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.R as DesignR
import noox.bzr.design.views.ChatInputView
import noox.bzr.design.views.PrimaryButtonView
import noox.bzr.gallery.R
import noox.bzr.screens.ScreenSnapshots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MobileInteractionTest {
    @get:Rule
    val paparazzi = ScreenSnapshots.paparazzi()

    @Before
    fun prepare() = ScreenSnapshots.prepare(paparazzi)

    @Test
    fun error_action_reloads_when_supported_and_otherwise_returns() {
        var reloads = 0
        var backs = 0
        val screen = C01HomeView(paparazzi.context).apply {
            onReload = { reloads++ }
            onBack = { backs++ }
        }
        screen.render(CustomerUiState("SCR-C01", CustomerPhase.Error))
        screen.findViewById<View>(R.id.error).findViewById<View>(DesignR.id.button).performClick()
        assertEquals(1, reloads)
        assertEquals(0, backs)

        screen.onReload = null
        screen.render(CustomerUiState("SCR-C01", CustomerPhase.Error))
        screen.findViewById<View>(R.id.error).findViewById<View>(DesignR.id.button).performClick()
        assertEquals(1, backs)
    }

    @Test
    fun review_keeps_publish_in_footer_and_connects_edit_to_the_draft() {
        var edits = 0
        var publishes = 0
        val screen = C05ReviewView(paparazzi.context).apply {
            onEdit = { edits++ }
            onPublish = { publishes++ }
        }
        screen.render(CustomerUiState("SCR-C05", CustomerPhase.Content, canContinue = true))
        val footer = screen.findViewById<LinearLayout>(R.id.bottomBar)
        val publish = screen.findViewById<PrimaryButtonView>(R.id.publish)
        assertSame(footer, publish.parent)
        screen.findViewById<View>(DesignR.id.edit).performClick()
        publish.findViewById<View>(DesignR.id.button).performClick()
        assertEquals(1, edits)
        assertEquals(1, publishes)

        screen.render(CustomerUiState("SCR-C05", CustomerPhase.Loading))
        assertEquals(View.GONE, footer.visibility)
        screen.render(CustomerUiState("SCR-C05", CustomerPhase.Error))
        assertEquals(View.GONE, footer.visibility)
    }

    @Test
    fun chat_prevents_blank_sends_and_hides_composer_in_read_only_conversations() {
        var sends = 0
        val input = ChatInputView(paparazzi.context).apply {
            onValueChange = {}
            onSend = { sends++ }
        }
        val edit = input.findViewById<EditText>(DesignR.id.edit)
        val send = input.findViewById<View>(DesignR.id.send)
        edit.setText(" \n ")
        assertFalse(send.isEnabled)
        send.performClick()
        assertEquals(0, sends)
        edit.setText("موعد الزيارة مناسب")
        assertTrue(send.isEnabled)
        send.performClick()
        assertEquals(1, sends)

        val screen = C19ChatView(paparazzi.context)
        screen.render(CustomerUiState("SCR-C19", CustomerPhase.Content, options = listOf("", "", "READ_ONLY")))
        val footer = screen.findViewById<View>(R.id.bottomBar)
        assertEquals(View.GONE, footer.visibility)
        screen.render(CustomerUiState("SCR-C19", CustomerPhase.Content))
        assertEquals(View.VISIBLE, footer.visibility)
    }

    @Test
    fun registration_uses_native_email_and_phone_keyboards_with_independent_value_direction() {
        val screen = CustomerRegisterScreen(paparazzi.context).apply {
            actions = AuthActions(onFieldChange = { _, _ -> })
            render(AuthLogic.reduce("SCR-C11", emptyMap()))
        }
        val email = screen.findViewById<View>(R.id.email).findViewById<EditText>(DesignR.id.edit)
        val phone = screen.findViewById<View>(R.id.phone).findViewById<EditText>(DesignR.id.edit)
        assertEquals(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, email.inputType and InputType.TYPE_MASK_VARIATION)
        assertEquals(InputType.TYPE_CLASS_PHONE, phone.inputType and InputType.TYPE_MASK_CLASS)
        assertEquals(View.TEXT_DIRECTION_LTR, email.textDirection)
        assertEquals(View.TEXT_DIRECTION_LTR, phone.textDirection)
        assertEquals(View.AUTOFILL_HINT_EMAIL_ADDRESS, email.autofillHints?.first())
    }

    @Test
    fun loading_button_blocks_repeated_submission() {
        var submissions = 0
        val button = PrimaryButtonView(paparazzi.context).apply { onClick = { submissions++ } }
        button.state = ButtonVisualState.Loading
        button.findViewById<View>(DesignR.id.button).performClick()
        assertEquals(0, submissions)
        button.state = ButtonVisualState.Normal
        button.findViewById<View>(DesignR.id.button).performClick()
        assertEquals(1, submissions)
    }
}
