package tr.com.onurbaykal.loglevelmanager.backend

import tr.com.onurbaykal.loglevelmanager.LogLevel
import tr.com.onurbaykal.loglevelmanager.LoggingBackend
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory

class LogbackBackendTest {
    private val backend = LogbackBackend()

    @AfterEach
    fun cleanUp() {
        backend.setLevel("com.example", null)
        backend.setLevel("com.example.logback.Service", null)
        backend.setLevel(LoggingBackend.ROOT_LOGGER_NAME, LogLevel.DEBUG)
    }

    @Test
    fun `lists existing loggers with root first-class`() {
        LoggerFactory.getLogger("com.example.logback.Listed")
        val names = backend.getLoggers().map { it.name }
        assertTrue(LoggingBackend.ROOT_LOGGER_NAME in names)
        assertTrue("com.example.logback.Listed" in names)
    }

    @Test
    fun `sets level of a class and resets it again`() {
        val slf4j = LoggerFactory.getLogger("com.example.logback.Service")
        backend.setLevel(LoggingBackend.ROOT_LOGGER_NAME, LogLevel.INFO)
        assertFalse(slf4j.isDebugEnabled)

        backend.setLevel("com.example.logback.Service", LogLevel.TRACE)
        assertTrue(slf4j.isTraceEnabled)
        assertEquals(LogLevel.TRACE, backend.getLogger("com.example.logback.Service").configuredLevel)

        backend.setLevel("com.example.logback.Service", null)
        val info = backend.getLogger("com.example.logback.Service")
        assertNull(info.configuredLevel)
        assertEquals(LogLevel.INFO, info.effectiveLevel)
        assertFalse(slf4j.isDebugEnabled)
    }

    @Test
    fun `package level is inherited by classes`() {
        backend.setLevel("com.example", LogLevel.WARN)
        val info = backend.getLogger("com.example.logback.Other")
        assertNull(info.configuredLevel)
        assertEquals(LogLevel.WARN, info.effectiveLevel)
    }

    @Test
    fun `root logger cannot be reset`() {
        assertThrows<IllegalArgumentException> { backend.setLevel("root", null) }
        assertEquals(LoggingBackend.ROOT_LOGGER_NAME, backend.getLogger("root").name)
    }
}
