package noox.bzr.screens

import androidx.fragment.app.FragmentFactory
import noox.bzr.gallery.R
import noox.bzr.provider.P02ProfileFragment
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.xmlpull.v1.XmlPullParser

class FragmentInstantiationTest {
    @get:Rule
    val paparazzi = ScreenSnapshots.paparazzi()

    @Test
    fun navigationDestinationsCanBeCreatedBeforeAttachment() {
        val destinations = linkedSetOf(P02ProfileFragment::class.java.name)
        paparazzi.context.resources.getXml(R.navigation.nav_graph).use { parser ->
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "fragment") {
                    destinations += checkNotNull(parser.getAttributeValue("http://schemas.android.com/apk/res/android", "name"))
                }
                parser.next()
            }
        }

        val factory = FragmentFactory()
        for (destination in destinations) {
            val fragment = factory.instantiate(javaClass.classLoader!!, destination)
            assertNull("$destination must not need an Activity during construction", fragment.activity)
        }
    }
}
