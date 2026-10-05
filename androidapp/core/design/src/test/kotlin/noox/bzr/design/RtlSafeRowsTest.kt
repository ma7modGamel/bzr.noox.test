package noox.bzr.design

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Up to Android 13, a horizontal LinearLayout spaced with dividers shifts its row in RTL and cuts off the first item
 * (Redmi Note 11). Every row must be a BremoLinearLayout, which turns those dividers into margins: XML rows through
 * Theme.Bremo's `viewInflaterClass`, Kotlin rows by constructing or extending BremoLinearLayout directly.
 */
class RtlSafeRowsTest {
    private val root = generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "settings.gradle.kts").isFile }

    @Test
    fun kotlinNeverBuildsAPlainLinearLayout() {
        val offenders = root.walk()
            .filter { it.isFile && it.extension == "kt" && "/src/main/" in it.path && it.name != "BremoLinearLayout.kt" }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    val plain = Regex("""(?<![\w.])LinearLayout\((context|parent\.context)\b""").containsMatchIn(line) &&
                        !line.contains("VERTICAL")
                    val extends = Regex(""":\s*LinearLayout\(context, attrs\)""").containsMatchIn(line)
                    if (extends || (plain && !isVerticalBlock(file, index))) "${file.relativeTo(root)}:${index + 1}" else null
                }
            }.toList()
        assertTrue("Use BremoLinearLayout instead of LinearLayout at:\n" + offenders.joinToString("\n"), offenders.isEmpty())
    }

    @Test
    fun themeInflatesXmlRowsAsBremoLinearLayout() {
        val styles = File(root, "core/design/src/main/res/values/bremo_styles.xml").readText()
        assertTrue(styles.contains("""<item name="viewInflaterClass">noox.bzr.design.views.BremoViewInflater</item>"""))
    }

    /** A plain LinearLayout is fine when the next lines make it vertical (dividers work there). */
    private fun isVerticalBlock(file: File, index: Int): Boolean =
        file.readLines().drop(index).take(3).any { it.contains("VERTICAL") }
}
