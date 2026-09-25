package io.github.theaob.loglevel

/**
 * Snapshot of a single logger.
 *
 * @property name logger name, [LoggingBackend.ROOT_LOGGER_NAME] for the root logger
 * @property configuredLevel level set explicitly on this logger, `null` when it is inherited
 * @property effectiveLevel level actually in effect, taking inheritance into account
 */
data class LoggerInfo(
    val name: String,
    val configuredLevel: LogLevel?,
    val effectiveLevel: LogLevel,
) {
    val isRoot: Boolean get() = name == LoggingBackend.ROOT_LOGGER_NAME
    val isConfigured: Boolean get() = configuredLevel != null
}
