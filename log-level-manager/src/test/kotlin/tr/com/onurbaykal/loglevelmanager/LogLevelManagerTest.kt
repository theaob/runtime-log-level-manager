package tr.com.onurbaykal.loglevelmanager

import tr.com.onurbaykal.loglevelmanager.backend.LogbackBackend
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LogLevelManagerTest {

    @Test
    fun `detects logback through slf4j`() {
        assertInstanceOf(LogbackBackend::class.java, LoggingBackends.detect())
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
    fun `parses level names`() {
        assertEquals(LogLevel.WARN, LogLevel.parse(" warning "))
        assertEquals(LogLevel.DEBUG, LogLevel.parse("debug"))
    }
}
