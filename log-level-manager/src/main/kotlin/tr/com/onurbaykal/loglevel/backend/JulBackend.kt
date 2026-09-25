package tr.com.onurbaykal.loglevel.backend

import tr.com.onurbaykal.loglevel.LogLevel
import tr.com.onurbaykal.loglevel.LoggerInfo
import tr.com.onurbaykal.loglevel.LoggingBackend
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger

/**
 * java.util.logging. Note that JUL handlers have their own level (the default `ConsoleHandler`
 * only prints INFO and above), so lowering a logger below INFO may also require lowering the
 * handler level in the application's logging configuration.
 */
class JulBackend : LoggingBackend {

    /** JUL only holds loggers weakly; keep the ones we configured alive so their level sticks. */
    private val configured = ConcurrentHashMap<String, Logger>()

    private val manager: LogManager
        get() = LogManager.getLogManager()

    override val name: String = "java.util.logging"

    override val supportedLevels: List<LogLevel> =
        listOf(LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR, LogLevel.OFF)

    override fun getLoggers(): List<LoggerInfo> =
        manager.loggerNames.toList().mapNotNull { manager.getLogger(it) }.map(::toInfo)

    override fun getLogger(name: String): LoggerInfo = toInfo(logger(name))

    override fun setLevel(name: String, level: LogLevel?) {
        val logger = logger(name)
        require(level != null || logger.name.isNotEmpty()) { "The root logger level cannot be reset" }
        logger.level = level?.let(::toJul)
        if (level == null) configured.remove(logger.name) else configured[logger.name] = logger
    }

    private fun logger(name: String): Logger {
        val normalized = LoggingBackend.normalizeName(name)
        return if (normalized == LoggingBackend.ROOT_LOGGER_NAME) manager.getLogger("") else Logger.getLogger(normalized)
    }

    private fun toInfo(logger: Logger): LoggerInfo {
        val effective = generateSequence(logger) { it.parent }.firstNotNullOfOrNull { it.level } ?: Level.INFO
        return LoggerInfo(
            name = logger.name.ifEmpty { LoggingBackend.ROOT_LOGGER_NAME },
            configuredLevel = logger.level?.let(::fromJul),
            effectiveLevel = fromJul(effective),
        )
    }

    private fun fromJul(level: Level): LogLevel {
        val value = level.intValue()
        return when {
            value == Level.OFF.intValue() -> LogLevel.OFF
            value >= Level.SEVERE.intValue() -> LogLevel.ERROR
            value >= Level.WARNING.intValue() -> LogLevel.WARN
            value >= Level.INFO.intValue() -> LogLevel.INFO
            value >= Level.FINE.intValue() -> LogLevel.DEBUG
            else -> LogLevel.TRACE
        }
    }

    private fun toJul(level: LogLevel): Level = when (level) {
        LogLevel.TRACE -> Level.FINEST
        LogLevel.DEBUG -> Level.FINE
        LogLevel.INFO -> Level.INFO
        LogLevel.WARN -> Level.WARNING
        LogLevel.ERROR, LogLevel.FATAL -> Level.SEVERE
        LogLevel.OFF -> Level.OFF
    }
}
