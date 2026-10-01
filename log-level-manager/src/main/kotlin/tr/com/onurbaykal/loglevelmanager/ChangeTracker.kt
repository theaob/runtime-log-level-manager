package tr.com.onurbaykal.loglevelmanager

/** Remembers the level each logger had before it was first changed, so changes can be reverted. */
internal class ChangeTracker {
    /** @property applied level set through the manager most recently, used to spot outside changes */
    private class Change(val original: LogLevel?, var applied: LogLevel?)

    private val changes = LinkedHashMap<String, Change>()

    @Synchronized
    fun recordOriginal(name: String, original: LogLevel?) {
        if (!changes.containsKey(name)) changes[name] = Change(original, original)
    }

    /** Records the level now in effect; forgets [name] when it is back at its original level. */
    @Synchronized
    fun recordApplied(name: String, applied: LogLevel?) {
        val change = changes[name] ?: return
        if (change.original == applied) changes.remove(name) else change.applied = applied
    }

    /**
     * Forgets every logger whose level no longer matches what was last applied through the
     * manager, which means it was changed elsewhere, e.g. by a configuration reload.
     * Returns the names that were dropped.
     */
    @Synchronized
    fun dropOverwritten(currentLevel: (String) -> LogLevel?): List<String> {
        val dropped = changes.filter { (name, change) -> currentLevel(name) != change.applied }.keys.toList()
        dropped.forEach(changes::remove)
        return dropped
    }

    @Synchronized
    fun isModified(name: String): Boolean = changes.containsKey(name)

    @Synchronized
    fun count(): Int = changes.size

    /** Copy of the tracked loggers with their original levels, in the order they were first changed. */
    @Synchronized
    fun snapshot(): Map<String, LogLevel?> = changes.mapValues { it.value.original }

    @Synchronized
    fun forget(name: String) {
        changes.remove(name)
    }

    @Synchronized
    fun clear() = changes.clear()
}
