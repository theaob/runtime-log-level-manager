package tr.com.onurbaykal.loglevelmanager

/** Remembers the level each logger had before it was first changed, so changes can be reverted. */
internal class ChangeTracker {
    private val originals = LinkedHashMap<String, LogLevel?>()

    @Synchronized
    fun recordOriginal(name: String, original: LogLevel?) {
        if (!originals.containsKey(name)) originals[name] = original
    }

    /** Forgets [name] when it is back at its original level. */
    @Synchronized
    fun forgetIfUnchanged(name: String, current: LogLevel?) {
        if (originals.containsKey(name) && originals[name] == current) originals.remove(name)
    }

    @Synchronized
    fun isModified(name: String): Boolean = originals.containsKey(name)

    @Synchronized
    fun count(): Int = originals.size

    /** Copy of the tracked loggers with their original levels, in the order they were first changed. */
    @Synchronized
    fun snapshot(): Map<String, LogLevel?> = LinkedHashMap(originals)

    @Synchronized
    fun forget(name: String) {
        originals.remove(name)
    }

    @Synchronized
    fun clear() = originals.clear()
}
