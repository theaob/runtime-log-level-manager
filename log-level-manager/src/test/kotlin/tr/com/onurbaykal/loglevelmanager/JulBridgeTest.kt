package tr.com.onurbaykal.loglevelmanager

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import tr.com.onurbaykal.loglevelmanager.backend.CompositeBackend
import tr.com.onurbaykal.loglevelmanager.backend.LogbackBackend
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.LogManager

class JulBridgeTest {
    private val julRoot = LogManager.getLogManager().getLogger("")
    private val julLogger = java.util.logging.Logger.getLogger("com.example.bridge.Legacy")
    private val appender = ListAppender<ILoggingEvent>().apply { start() }
    private lateinit var originalHandlers: List<java.util.logging.Handler>

    @BeforeEach
    fun setUp() {
        originalHandlers = julRoot.handlers.toList()
        if (originalHandlers.isEmpty()) julRoot.addHandler(ConsoleHandler())
        (LoggerFactory.getLogger("com.example.bridge.Legacy") as Logger).addAppender(appender)
        LogLevelManager.backend = CompositeBackend(LogbackBackend())
    }

    @AfterEach
    fun tearDown() {
        JulBridge.uninstall()
        LogLevelManager.setLevel("com.example.bridge.Legacy", null)
        julRoot.handlers.filterNot { it in originalHandlers }.forEach(julRoot::removeHandler)
        (LoggerFactory.getLogger("com.example.bridge.Legacy") as Logger).detachAppender(appender)
    }

    @Test
    fun `forwards JUL records and lets the main framework filter them`() {
        JulBridge.install()
        LogLevelManager.setLevel("com.example.bridge.Legacy", LogLevel.INFO)
        julLogger.log(Level.WARNING, "disk {0}% full", 97)
        julLogger.fine("dropped by Logback, whose level is INFO")

        assertEquals(listOf("WARN disk 97% full"), appender.list.map { "${it.level} ${it.formattedMessage}" })
    }

    @Test
    fun `levels set through the manager let lower JUL records through`() {
        JulBridge.install()
        LogLevelManager.setLevel("com.example.bridge.Legacy", LogLevel.DEBUG)
        julLogger.fine("now forwarded")
        julLogger.finest("still filtered")

        assertEquals(listOf("DEBUG now forwarded"), appender.list.map { "${it.level} ${it.formattedMessage}" })
    }

    @Test
    fun `uninstall restores the handlers and the root level`() {
        val before = julRoot.handlers.toList()
        val level = julRoot.level
        JulBridge.install()
        assertTrue(JulBridge.isInstalled)
        assertEquals(Level.ALL, julRoot.level)
        assertFalse(julRoot.handlers.any { it in before })

        JulBridge.uninstall()
        assertFalse(JulBridge.isInstalled)
        assertEquals(before.toSet(), julRoot.handlers.toSet())
        assertEquals(level, julRoot.level)
    }
}
