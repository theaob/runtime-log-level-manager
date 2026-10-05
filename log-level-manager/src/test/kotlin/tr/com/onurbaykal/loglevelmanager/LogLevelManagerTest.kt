package tr.com.onurbaykal.loglevelmanager

import tr.com.onurbaykal.loglevelmanager.backend.CompositeBackend
import tr.com.onurbaykal.loglevelmanager.backend.LogbackBackend
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LogLevelManagerTest {

    @Test
    fun `detects logback through slf4j and adds JUL to it`() {
        val backend = assertInstanceOf(CompositeBackend::class.java, LoggingBackends.detect())
        assertInstanceOf(LogbackBackend::class.java, backend.primary)
    }

    @Test
    fun `tracks changes and reverts them`() {
        LogLevelManager.backend = LogbackBackend()
        LogLevelManager.setLevel("com.example.manager", LogLevel.WARN)
        LogLevelManager.setLevel("com.example.manager", LogLevel.TRACE)
        LogLevelManager.setLevel(LogLevelManagerTest::class, LogLevel.DEBUG)
        assertTrue(LogLevelManager.isModified("com.example.manager"))
        assertEquals(2, LogLevelManager.modifiedCount())

        LogLevelManager.revertAll()

        assertEquals(0, LogLevelManager.modifiedCount())
        assertEquals(null, LogLevelManager.getLogger("com.example.manager").configuredLevel)
        assertEquals(null, LogLevelManager.getLogger(LogLevelManagerTest::class.java.name).configuredLevel)
    }

    @Test
    fun `setting a logger back to its original level clears the change`() {
        LogLevelManager.backend = LogbackBackend()
        LogLevelManager.setLevel("com.example.roundtrip", LogLevel.ERROR)
        LogLevelManager.setLevel("com.example.roundtrip", null)
        assertFalse(LogLevelManager.isModified("com.example.roundtrip"))
    }

    @Test
    fun `revertAll keeps loggers whose revert failed and reverts the rest`() {
        val failing = object : LoggingBackend by LogbackBackend() {
            override fun setLevel(name: String, level: LogLevel?) {
                if (name == "com.example.broken" && level == null) throw IllegalStateException("boom")
                LogbackBackend().setLevel(name, level)
            }
        }
        LogLevelManager.backend = failing
        LogLevelManager.setLevel("com.example.broken", LogLevel.WARN)
        LogLevelManager.setLevel("com.example.fine", LogLevel.WARN)

        val error = assertThrows<IllegalStateException> { LogLevelManager.revertAll() }

        assertEquals("boom", error.message)
        assertEquals(null, LogLevelManager.getLogger("com.example.fine").configuredLevel)
        assertFalse(LogLevelManager.isModified("com.example.fine"))
        assertTrue(LogLevelManager.isModified("com.example.broken"))
        assertEquals(1, LogLevelManager.modifiedCount())

        LogLevelManager.backend = LogbackBackend()
        LogLevelManager.setLevel("com.example.broken", null)
    }

    @Test
    fun `changes overwritten outside the manager are dropped`() {
        val backend = LogbackBackend()
        LogLevelManager.backend = backend
        LogLevelManager.setLevel("com.example.reloaded", LogLevel.WARN)
        LogLevelManager.setLevel("com.example.untouched", LogLevel.WARN)

        // Simulate a configuration reload that assigns a different level
        backend.setLevel("com.example.reloaded", LogLevel.ERROR)

        assertEquals(listOf("com.example.reloaded"), LogLevelManager.dropOverwrittenChanges())
        assertFalse(LogLevelManager.isModified("com.example.reloaded"))
        assertTrue(LogLevelManager.isModified("com.example.untouched"))

        LogLevelManager.revertAll()
        assertEquals(LogLevel.ERROR, LogLevelManager.getLogger("com.example.reloaded").configuredLevel)
        assertEquals(null, LogLevelManager.getLogger("com.example.untouched").configuredLevel)
        backend.setLevel("com.example.reloaded", null)
    }

    @Test
    fun `parses level names`() {
        assertEquals(LogLevel.WARN, LogLevel.parse(" warning "))
        assertEquals(LogLevel.DEBUG, LogLevel.parse("debug"))
    }
}
