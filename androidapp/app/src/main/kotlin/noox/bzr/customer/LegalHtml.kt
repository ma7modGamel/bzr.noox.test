package noox.bzr.customer

import android.text.Editable
import android.text.Html
import android.text.Spanned
import androidx.core.text.HtmlCompat
import org.xml.sax.XMLReader

/** Native formatted text; custom list tags retain ordered numbering, which HtmlCompat omits. */
object LegalHtml {
    fun render(html: String): Spanned {
        val prepared = html.replace(Regex("<(\\/?)(ol|ul|li)(\\s[^>]*)?>", RegexOption.IGNORE_CASE)) { match ->
            val tag = when (match.groupValues[2].lowercase()) {
                "ol" -> "ordered-list"
                "ul" -> "unordered-list"
                else -> "list-item"
            }
            "<${match.groupValues[1]}$tag>"
        }
        return HtmlCompat.fromHtml(prepared, HtmlCompat.FROM_HTML_MODE_LEGACY, null, Lists())
    }

    private class Lists : Html.TagHandler {
        private data class Level(val ordered: Boolean, var count: Int = 0)
        private val levels = ArrayDeque<Level>()

        override fun handleTag(opening: Boolean, tag: String, output: Editable, reader: XMLReader) {
            when (tag) {
                "ordered-list", "unordered-list" -> {
                    line(output)
                    if (opening) levels.addLast(Level(tag == "ordered-list")) else levels.removeLastOrNull()
                }
                "list-item" -> {
                    line(output)
                    if (opening) {
                        val level = levels.lastOrNull()
                        level?.let { it.count++ }
                        output.append(if (level?.ordered == true) "${level.count}. " else "• ")
                    }
                }
            }
        }

        private fun line(output: Editable) {
            if (output.isNotEmpty() && output.last() != '\n') output.append('\n')
        }
    }
}
