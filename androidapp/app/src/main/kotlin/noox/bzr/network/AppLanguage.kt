package noox.bzr.network

/** Resource selection and API requests support the same two languages. */
object AppLanguage {
    fun resolve(tags: List<String>): String = tags.firstNotNullOfOrNull { tag ->
        tag.substringBefore('-').substringBefore('_').lowercase().takeIf { it == "ar" || it == "en" }
    } ?: "ar"
}
