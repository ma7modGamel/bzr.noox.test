package noox.bzr.customer

import android.view.View
import android.widget.TextView
import androidx.core.view.descendants
import noox.bzr.screens.ScreenSnapshots
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MediaDurationViewsTest {
    @get:Rule val paparazzi = ScreenSnapshots.paparazzi()

    @Test fun videosAtThirtySecondsPassAndLongerOrUnreadableVideosAreRejected() {
        assertNull(CustomerMediaLimits.videoError(30.0))
        assertEquals("media.validation.video_duration", CustomerMediaLimits.videoError(30.001))
        assertEquals("media.validation.video_duration", CustomerMediaLimits.videoError(60.0))
        assertEquals("media.validation.video_unreadable", CustomerMediaLimits.videoError(null))
        assertEquals("media.validation.video_unreadable", CustomerMediaLimits.videoError(Double.NaN))
    }

    @Test fun theScreenShowsTheThirtySecondLimitAndLetsTheCustomerContinueWithoutTheRejectedVideo() {
        ScreenSnapshots.prepare(paparazzi)
        val view = C17MediaView(paparazzi.context)
        val state = CustomerLogic.reduce("SCR-C17", mapOf("media_kinds" to emptyList<String>(), "media_states" to emptyList<String>()))
        view.render(state)
        assertTrue(text(view).contains("30 ثانية"))
        assertTrue(text(view).contains("120 ثانية"))
        paparazzi.snapshot(view, name = "problem-video-thirty-second-limit")
        val rejected = CustomerLogic.reduce("SCR-C17", mapOf("media_error" to CustomerMediaLimits.videoError(31.0)))
        view.render(rejected)
        assertTrue(rejected.items.isEmpty())
        assertTrue(rejected.canContinue)
        assertTrue(text(view).contains("اختر فيديو أقصر"))
        paparazzi.snapshot(view, name = "problem-video-too-long-selection")
    }

    private fun text(view: android.view.ViewGroup): String = view.descendants.filterIsInstance<TextView>().filter { it.visibility == View.VISIBLE }.joinToString("\n") { it.text.toString() }
}
