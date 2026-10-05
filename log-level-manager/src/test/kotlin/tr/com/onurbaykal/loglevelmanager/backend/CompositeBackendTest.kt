package tr.com.onurbaykal.loglevelmanager.backend

import tr.com.onurbaykal.loglevelmanager.JulBridge
import tr.com.onurbaykal.loglevelmanager.LogLevel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.logging.Level
import java.util.logging.LogManager
import java.util.logging.Logger

class CompositeBackendTest {
    private val backend = CompositeBackend(LogbackBackend())

    // JUL drops loggers nobody references; keep the ones the tests look at alive.
    private val julOnly = Logger.getLogger("com.example.composite.JulOnly")
    private val shared = Logger.getLogger("com.example.composite.Shared")

    @AfterEach
    fun cleanUp() {
        JulBridge.uninstall()
        backend.setLevel("com.example.composite.JulOnly", null)
        backend.setLevel("com.example.composite.Shared", null)
        backend.setLevel("com.example.composite.MainOnly", null)
        shared.level = null
    }

    @Test
    fun `lists JUL-only loggers tagged, without duplicating shared ones or the root`() {
        LoggerFactory.getLogger("com.example.composite.Shared")
        val loggers = backend.getLoggers()

        val julOnlyInfo = loggers.single { it.name == "com.example.composite.JulOnly" }
        assertEquals(CompositeBackend.JUL_SOURCE, julOnlyInfo.source)
        val sharedInfo = loggers.single { it.name == "com.example.composite.Shared" }
        assertNull(sharedInfo.source)
        assertEquals(1, loggers.count { it.isRoot })
        assertTrue(backend.name.endsWith("+ JUL"))
    }

    @Test
    fun `level of a JUL-only logger goes to JUL and not to the main framework`() {
        backend.setLevel("com.example.composite.JulOnly", LogLevel.DEBUG)

        assertEquals(Level.FINE, julOnly.level)
        assertEquals(LogLevel.DEBUG, backend.getLogger("com.example.composite.JulOnly").configuredLevel)
        assertFalse(backend.primary.exists("com.example.composite.JulOnly"))
    }

    @Test
    fun `level of a shared logger is mirrored to JUL`() {
        LoggerFactory.getLogger("com.example.composite.Shared")
        backend.setLevel("com.example.composite.Shared", LogLevel.TRACE)
        assertEquals(Level.FINEST, shared.level)
        assertTrue(LoggerFactory.getLogger("com.example.composite.Shared").isTraceEnabled)

        backend.setLevel("com.example.composite.Shared", null)
        assertNull(shared.level)
    }

    @Test
    fun `main-only loggers are mirrored to JUL only while the bridge is installed`() {
        backend.setLevel("com.example.composite.MainOnly", LogLevel.WARN)
        assertNull(LogManager.getLogManager().getLogger("com.example.composite.MainOnly"))

        JulBridge.install()
        backend.setLevel("com.example.composite.MainOnly", LogLevel.DEBUG)
        assertEquals(Level.FINE, LogManager.getLogManager().getLogger("com.example.composite.MainOnly")?.level)
    }
}
