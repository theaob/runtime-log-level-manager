package tr.com.onurbaykal.loglevelmanager

/**
 * Backend-neutral log levels, ordered from most to least verbose.
 *
 * Each [LoggingBackend] maps these onto its own levels and reports the subset it
 * supports through [LoggingBackend.supportedLevels].
 */
enum class LogLevel {
    TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF;

    companion object {
        /** Parses a level name case-insensitively, also accepting `WARNING`. */
        @JvmStatic
        fun parse(value: String): LogLevel {
            val normalized = value.trim().uppercase()
            return if (normalized == "WARNING") WARN else valueOf(normalized)
        }
    }
}
