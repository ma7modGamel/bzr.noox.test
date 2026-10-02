import Foundation

/// The customer back stack (6ب, `design/fixtures/navigation/history.json`), shared rules with Android:
/// the root clears it, a screen change records the previous screen when it was restorable, returning to a
/// recorded screen drops what came after it, and `commit()` clears it after a mutation (publish, accept,
/// cancel…) so back never re-opens a submitted form. `back` answers the entry to show, the root when nothing
/// is recorded, or nil on the root itself.
public struct ScreenHistory<Entry> {
    private let root: String
    private let screenOf: (Entry) -> String
    private var entries: [Entry] = []
    private var current: Entry?
    private var committed = false
    private var previousRestorable = true

    public init(root: String, screenOf: @escaping (Entry) -> String) {
        self.root = root
        self.screenOf = screenOf
    }

    public mutating func show(_ next: Entry, restorable: Bool = true) {
        let previous = current
        let screen = screenOf(next)
        current = next
        if screen == root {
            entries.removeAll()
            committed = false
            previousRestorable = restorable
            return
        }
        let changed = previous.map { screenOf($0) != screen } ?? true
        if committed && changed {
            // The first screen after a mutation (a same-screen "submitting" state keeps waiting).
            entries.removeAll()
            committed = false
        } else if !committed && changed, let previous, previousRestorable {
            entries.append(previous)
        }
        if let index = entries.firstIndex(where: { screenOf($0) == screen }) {
            entries.removeSubrange(index...)
        }
        previousRestorable = restorable
    }

    /// After a successful mutation: the next screen starts a fresh history.
    public mutating func commit() { committed = true }

    /// The entry to show on back, or nil when already on the root.
    public mutating func back(rootEntry: () -> Entry) -> Entry? {
        guard let current, screenOf(current) != root else { return nil }
        let target = entries.popLast() ?? rootEntry()
        self.current = target
        previousRestorable = true
        return target
    }
}
