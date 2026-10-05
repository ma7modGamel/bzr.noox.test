package noox.bzr.customer

/**
 * The customer back stack (6ب, `design/fixtures/navigation/history.json`), shared rules with iOS:
 * the root clears it, a screen change records the previous screen when it was restorable, returning to a
 * recorded screen drops what came after it, and [commit] clears it after a mutation (publish, accept, cancel…)
 * so back never re-opens a submitted form. [back] answers the entry to show, the root when nothing is recorded,
 * or null on the root itself (the system may then leave the app).
 */
class ScreenHistory<T>(private val root: String, private val screenOf: (T) -> String) {
    private val entries = ArrayDeque<T>()
    private var current: T? = null
    private var committed = false

    fun show(next: T, restorable: Boolean = true) {
        val previous = current
        val screen = screenOf(next)
        current = next
        if (screen == root) {
            entries.clear()
            committed = false
            return
        }
        val changed = previous == null || screenOf(previous) != screen
        if (committed && changed) {
            // The first screen after a mutation (a same-screen "submitting" state keeps waiting).
            entries.clear()
            committed = false
        } else if (!committed && changed && previous != null && previousRestorable) {
            entries.addLast(previous)
        }
        val index = entries.indexOfFirst { screenOf(it) == screen }
        if (index >= 0) while (entries.size > index) entries.removeLast()
        previousRestorable = restorable
    }

    /** After a successful mutation: the next screen starts a fresh history. */
    fun commit() {
        committed = true
    }

    /** The entry to show on back, or null when already on the root. */
    fun back(rootEntry: () -> T): T? {
        val screen = current?.let(screenOf)
        if (screen == null || screen == root) return null
        val target = entries.removeLastOrNull() ?: rootEntry()
        current = target
        previousRestorable = true
        return target
    }

    private var previousRestorable = true
}
