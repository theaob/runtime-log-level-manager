package tr.com.onurbaykal.loglevelmanager.backend

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import tr.com.onurbaykal.loglevelmanager.LogLevel
import tr.com.onurbaykal.loglevelmanager.LoggerInfo
import tr.com.onurbaykal.loglevelmanager.LoggingBackend
import org.slf4j.LoggerFactory

/** Logback, reached through SLF4J. */
class LogbackBackend : LoggingBackend {

    private val context: LoggerContext
        get() = LoggerFactory.getILoggerFactory() as LoggerContext

    override val name: String = "Logback"

    override val supportedLevels: List<LogLevel> =
        listOf(LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR, LogLevel.OFF)

    override fun getLoggers(): List<LoggerInfo> = context.loggerList.map(::toInfo)

    override fun getLogger(name: String): LoggerInfo = toInfo(logger(name))

    override fun setLevel(name: String, level: LogLevel?) {
        val logger = logger(name)
        require(level != null || logger.name != Logger.ROOT_LOGGER_NAME) { "The root logger level cannot be reset" }
        logger.level = level?.let(::toLogback)
    }

    private fun logger(name: String): Logger = context.getLogger(LoggingBackend.normalizeName(name))

    private fun toInfo(logger: Logger) = LoggerInfo(
        name = if (logger.name == Logger.ROOT_LOGGER_NAME) LoggingBackend.ROOT_LOGGER_NAME else logger.name,
        configuredLevel = logger.level?.let(::fromLogback),
        effectiveLevel = fromLogback(logger.effectiveLevel),
    )

    private fun fromLogback(level: Level): LogLevel = when (level.toInt()) {
        Level.OFF_INT -> LogLevel.OFF
        Level.ERROR_INT -> LogLevel.ERROR
        Level.WARN_INT -> LogLevel.WARN
        Level.INFO_INT -> LogLevel.INFO
        Level.DEBUG_INT -> LogLevel.DEBUG
        else -> LogLevel.TRACE
    }

    private fun toLogback(level: LogLevel): Level = when (level) {
        LogLevel.TRACE -> Level.TRACE
        LogLevel.DEBUG -> Level.DEBUG
        LogLevel.INFO -> Level.INFO
        LogLevel.WARN -> Level.WARN
        LogLevel.ERROR, LogLevel.FATAL -> Level.ERROR
        LogLevel.OFF -> Level.OFF
    }
}
