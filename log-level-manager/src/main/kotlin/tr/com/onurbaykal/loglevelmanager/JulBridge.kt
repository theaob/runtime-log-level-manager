package tr.com.onurbaykal.loglevelmanager

import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.LogRecord
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

/**
 * Routes java.util.logging records into SLF4J, so that output from JavaFX, the JDK and other
 * JUL-based code ends up in the application's Logback or Log4j2 appenders and can be filtered
 * from the log level window like everything else.
 *
 * ```kotlin
 * JulBridge.install()   // once at startup; needs slf4j-api on the classpath
 * ```
 *
 * Levels are kept in step by [tr.com.onurbaykal.loglevelmanager.backend.CompositeBackend]:
 * a JUL logger drops records below its own level before this handler sees them, so a level set
 * through [LogLevelManager] is applied to the JUL logger of the same name too.
 */
object JulBridge {

    private var removedHandlers: List<Handler> = emptyList()
    private var previousRootLevel: Level? = null
    private var handler: ForwardingHandler? = null

    /** Whether [install] is in effect. */
    @JvmStatic
    val isInstalled: Boolean
        @Synchronized get() = handler != null

    /** Whether SLF4J is available, i.e. whether [install] can work. */
    @JvmStatic
    val isAvailable: Boolean
        get() = runCatching { Class.forName("org.slf4j.LoggerFactory") }.isSuccess

    /**
     * Installs the bridge on the JUL root logger. Existing root handlers (typically the console
     * handler) are removed so that records are not printed twice; [uninstall] puts them back.
     * The JUL root level is lowered to `ALL`; from then on the per-logger levels mirrored by
     * [LogLevelManager] decide what gets through.
     *
     * @param removeExistingHandlers `false` keeps the current root handlers next to the bridge
     * @throws IllegalStateException when SLF4J is not on the classpath
     */
    @JvmStatic
    @JvmOverloads
    @Synchronized
    fun install(removeExistingHandlers: Boolean = true) {
        if (handler != null) return
        check(isAvailable) { "org.slf4j:slf4j-api is required to bridge java.util.logging" }
        val root = LogManager.getLogManager().getLogger("")
        if (removeExistingHandlers) {
            removedHandlers = root.handlers.toList()
            removedHandlers.forEach(root::removeHandler)
        }
        previousRootLevel = root.level
        root.level = Level.ALL
        handler = ForwardingHandler().also(root::addHandler)
    }

    /** Removes the bridge and restores the handlers and root level [install] replaced. */
    @JvmStatic
    @Synchronized
    fun uninstall() {
        val current = handler ?: return
        val root = LogManager.getLogManager().getLogger("")
        root.removeHandler(current)
        removedHandlers.forEach(root::addHandler)
        removedHandlers = emptyList()
        root.level = previousRootLevel
        handler = null
    }

    /** JUL handler that forwards each record to the SLF4J logger of the same name. */
    private class ForwardingHandler : Handler() {
        private val formatter = SimpleFormatter()

        init {
            level = Level.ALL
        }

        override fun publish(record: LogRecord?) {
            if (record == null) return
            val logger = org.slf4j.LoggerFactory.getLogger(record.loggerName ?: Logger.GLOBAL_LOGGER_NAME)
            val message by lazy { formatter.formatMessage(record) }
            val value = record.level.intValue()
            when {
                value >= Level.SEVERE.intValue() -> if (logger.isErrorEnabled) logger.error(message, record.thrown)
                value >= Level.WARNING.intValue() -> if (logger.isWarnEnabled) logger.warn(message, record.thrown)
                value >= Level.INFO.intValue() -> if (logger.isInfoEnabled) logger.info(message, record.thrown)
                value >= Level.FINE.intValue() -> if (logger.isDebugEnabled) logger.debug(message, record.thrown)
                else -> if (logger.isTraceEnabled) logger.trace(message, record.thrown)
            }
        }

        override fun flush() = Unit
        override fun close() = Unit
    }
}
