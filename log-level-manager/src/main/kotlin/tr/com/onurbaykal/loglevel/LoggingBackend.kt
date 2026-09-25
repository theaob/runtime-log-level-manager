package tr.com.onurbaykal.loglevel

/**
 * Adapter over a concrete logging framework.
 *
 * Built-in implementations exist for Logback, Log4j2 and java.util.logging and are picked
 * automatically by [LoggingBackends.detect]. A custom implementation can be registered through
 * [java.util.ServiceLoader] (`META-INF/services/tr.com.onurbaykal.loglevel.LoggingBackend`) or
 * assigned to [LogLevelManager.backend].
 */
interface LoggingBackend {
    /** Human readable framework name shown in the UI. */
    val name: String

    /** Levels that can be selected for this backend, most verbose first. */
    val supportedLevels: List<LogLevel>

    /** All loggers currently known to the framework. */
    fun getLoggers(): List<LoggerInfo>

    /** Returns the logger with the given name, creating it in the framework if necessary. */
    fun getLogger(name: String): LoggerInfo

    /**
     * Sets the level of the given logger. `null` removes the explicit level so the logger
     * inherits it from its parent again; this is not allowed for the root logger.
     */
    fun setLevel(name: String, level: LogLevel?)

    companion object {
        const val ROOT_LOGGER_NAME = "ROOT"

        /** Maps user input such as `root` or an empty string onto [ROOT_LOGGER_NAME]. */
        @JvmStatic
        fun normalizeName(name: String): String {
            val trimmed = name.trim()
            return if (trimmed.isEmpty() || trimmed.equals(ROOT_LOGGER_NAME, ignoreCase = true)) ROOT_LOGGER_NAME else trimmed
        }
    }
}
