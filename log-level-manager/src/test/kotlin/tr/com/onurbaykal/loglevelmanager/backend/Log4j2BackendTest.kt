package tr.com.onurbaykal.loglevelmanager.backend

import tr.com.onurbaykal.loglevelmanager.LogLevel
import tr.com.onurbaykal.loglevelmanager.LoggingBackend
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class Log4j2BackendTest {
    private val backend = Log4j2Backend()

    @AfterEach
    fun cleanUp() {
        backend.setLevel("com.example.log4j", null)
        backend.setLevel("com.example.log4j.Service", null)
        backend.setLevel(LoggingBackend.ROOT_LOGGER_NAME, LogLevel.ERROR)
    }

    @Test
    fun `sets level of a class and removes the created config on reset`() {
        val logger = LogManager.getLogger("com.example.log4j.Service")
        assertFalse(logger.isDebugEnabled)

        backend.setLevel("com.example.log4j.Service", LogLevel.DEBUG)
        assertTrue(logger.isDebugEnabled)
        assertEquals(LogLevel.DEBUG, backend.getLogger("com.example.log4j.Service").configuredLevel)

        backend.setLevel("com.example.log4j.Service", null)
        assertFalse(logger.isDebugEnabled)
        assertNull(backend.getLogger("com.example.log4j.Service").configuredLevel)
        val context = LogManager.getContext(false) as LoggerContext
        assertFalse(context.configuration.loggers.containsKey("com.example.log4j.Service"))
    }

    @Test
    fun `package level is inherited and listed`() {
        backend.setLevel("com.example.log4j", LogLevel.FATAL)
        val info = backend.getLogger("com.example.log4j.Other")
        assertNull(info.configuredLevel)
        assertEquals(LogLevel.FATAL, info.effectiveLevel)
        assertTrue(backend.getLoggers().any { it.name == "com.example.log4j" && it.configuredLevel == LogLevel.FATAL })
    }

    @Test
    fun `root logger level can be changed but not reset`() {
        backend.setLevel("ROOT", LogLevel.WARN)
        assertEquals(LogLevel.WARN, backend.getLogger("com.example.log4j.Any").effectiveLevel)
        assertThrows<IllegalArgumentException> { backend.setLevel("ROOT", null) }
    }
}
