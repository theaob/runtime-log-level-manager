package tr.com.onurbaykal.loglevel.backend

import tr.com.onurbaykal.loglevel.LogLevel
import tr.com.onurbaykal.loglevel.LoggerInfo
import tr.com.onurbaykal.loglevel.LoggingBackend
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.config.LoggerConfig
import java.util.concurrent.ConcurrentHashMap

/** Log4j2 core, used either directly or through `log4j-slf4j2-impl`. */
class Log4j2Backend : LoggingBackend {

    /** LoggerConfigs created by this backend; they are removed again when their level is reset. */
    private val createdConfigs = ConcurrentHashMap.newKeySet<String>()

    private val context: LoggerContext
        get() = LogManager.getContext(false) as LoggerContext

    override val name: String = "Log4j2"

    override val supportedLevels: List<LogLevel> =
        listOf(LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR, LogLevel.FATAL, LogLevel.OFF)

    override fun getLoggers(): List<LoggerInfo> {
        val context = context
        val names = LinkedHashSet<String>()
        names += context.configuration.loggers.keys
        context.loggers.mapTo(names) { it.name }
        return names.map { toInfo(context, it) }
    }

    override fun getLogger(name: String): LoggerInfo = toInfo(context, toLog4jName(name))

    override fun setLevel(name: String, level: LogLevel?) {
        val context = context
        val config = context.configuration
        val log4jName = toLog4jName(name)
        val loggerConfig = if (log4jName.isEmpty()) config.rootLogger else config.loggers[log4jName]
        if (level == null) {
            require(log4jName.isNotEmpty()) { "The root logger level cannot be reset" }
            when {
                loggerConfig == null -> Unit
                createdConfigs.remove(log4jName) -> config.removeLogger(log4jName)
                else -> loggerConfig.level = null
            }
        } else if (loggerConfig == null) {
            config.addLogger(log4jName, LoggerConfig(log4jName, toLog4j(level), true))
            createdConfigs += log4jName
        } else {
            loggerConfig.level = toLog4j(level)
        }
        context.updateLoggers()
    }

    private fun toInfo(context: LoggerContext, log4jName: String): LoggerInfo {
        val config = context.configuration
        val exact = if (log4jName.isEmpty()) config.rootLogger else config.loggers[log4jName]
        return LoggerInfo(
            name = if (log4jName.isEmpty()) LoggingBackend.ROOT_LOGGER_NAME else log4jName,
            configuredLevel = exact?.explicitLevel?.let(::fromLog4j),
            effectiveLevel = fromLog4j(config.getLoggerConfig(log4jName).level ?: Level.ERROR),
        )
    }

    private fun toLog4jName(name: String): String =
        LoggingBackend.normalizeName(name).let { if (it == LoggingBackend.ROOT_LOGGER_NAME) LogManager.ROOT_LOGGER_NAME else it }

    private fun fromLog4j(level: Level): LogLevel = when {
        level == Level.OFF -> LogLevel.OFF
        level.isMoreSpecificThan(Level.FATAL) -> LogLevel.FATAL
        level.isMoreSpecificThan(Level.ERROR) -> LogLevel.ERROR
        level.isMoreSpecificThan(Level.WARN) -> LogLevel.WARN
        level.isMoreSpecificThan(Level.INFO) -> LogLevel.INFO
        level.isMoreSpecificThan(Level.DEBUG) -> LogLevel.DEBUG
        else -> LogLevel.TRACE
    }

    private fun toLog4j(level: LogLevel): Level = when (level) {
        LogLevel.TRACE -> Level.TRACE
        LogLevel.DEBUG -> Level.DEBUG
        LogLevel.INFO -> Level.INFO
        LogLevel.WARN -> Level.WARN
        LogLevel.ERROR -> Level.ERROR
        LogLevel.FATAL -> Level.FATAL
        LogLevel.OFF -> Level.OFF
    }
}
