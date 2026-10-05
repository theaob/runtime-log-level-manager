package tr.com.onurbaykal.loglevelmanager

/**
 * Snapshot of a single logger.
 *
 * @property name logger name, [LoggingBackend.ROOT_LOGGER_NAME] for the root logger
 * @property configuredLevel level set explicitly on this logger, `null` when it is inherited
 * @property effectiveLevel level actually in effect, taking inheritance into account
 * @property source short label of the framework the logger comes from when it is not the main
 *   one, e.g. `JUL` for a java.util.logging logger listed next to Logback loggers; `null` otherwise
 */
data class LoggerInfo(
    val name: String,
    val configuredLevel: LogLevel?,
    val effectiveLevel: LogLevel,
    val source: String? = null,
) {
    val isRoot: Boolean get() = name == LoggingBackend.ROOT_LOGGER_NAME
    val isConfigured: Boolean get() = configuredLevel != null
}
