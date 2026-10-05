package tr.com.onurbaykal.loglevelmanager.backend

import tr.com.onurbaykal.loglevelmanager.JulBridge
import tr.com.onurbaykal.loglevelmanager.LogLevel
import tr.com.onurbaykal.loglevelmanager.LoggerInfo
import tr.com.onurbaykal.loglevelmanager.LoggingBackend

/**
 * Main backend (Logback or Log4j2) plus java.util.logging in one list.
 *
 * Code that logs through JUL, such as JavaFX and parts of the JDK, is invisible to Logback and
 * Log4j2. This backend lists those loggers next to the main ones, tagged with [JUL_SOURCE], and
 * applies level changes to JUL as well: a JUL logger filters records by its own level before any
 * handler, including the [JulBridge], sees them, so the two sides have to be kept in step.
 */
class CompositeBackend(
    val primary: LoggingBackend,
    val jul: JulBackend = JulBackend(),
) : LoggingBackend {

    override val name: String get() = "${primary.name} + JUL"

    override val supportedLevels: List<LogLevel> get() = primary.supportedLevels

    override fun getLoggers(): List<LoggerInfo> {
        val main = primary.getLoggers()
        val mainNames = main.mapTo(HashSet()) { it.name }
        // The JUL root is driven by the main root (see setLevel), so it is not listed twice.
        val julOnly = jul.getLoggers().filter { !it.isRoot && it.name !in mainNames }.map { it.copy(source = JUL_SOURCE) }
        return main + julOnly
    }

    override fun getLogger(name: String): LoggerInfo {
        val normalized = LoggingBackend.normalizeName(name)
        if (isJulOnly(normalized)) return jul.getLogger(normalized).copy(source = JUL_SOURCE)
        return primary.getLogger(normalized)
    }

    override fun setLevel(name: String, level: LogLevel?) {
        val normalized = LoggingBackend.normalizeName(name)
        if (isJulOnly(normalized)) {
            jul.setLevel(normalized, level)
            return
        }
        primary.setLevel(normalized, level)
        // Mirror the change so JUL does not filter what the main framework would now accept. Only
        // loggers JUL already knows are touched unless the bridge is on, in which case every
        // record has to get through JUL to reach the main framework.
        if (JulBridge.isInstalled || jul.exists(normalized)) jul.setLevel(normalized, level)
    }

    private fun isJulOnly(normalized: String): Boolean =
        normalized != LoggingBackend.ROOT_LOGGER_NAME && jul.exists(normalized) && !primary.exists(normalized)

    companion object {
        const val JUL_SOURCE = "JUL"
    }
}
