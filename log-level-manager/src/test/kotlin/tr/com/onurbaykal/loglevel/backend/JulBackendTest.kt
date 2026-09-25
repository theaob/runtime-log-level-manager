package tr.com.onurbaykal.loglevel.backend

import tr.com.onurbaykal.loglevel.LogLevel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.logging.Level
import java.util.logging.Logger

class JulBackendTest {
    private val backend = JulBackend()

    @AfterEach
    fun cleanUp() {
        backend.setLevel("com.example.jul", null)
        backend.setLevel("com.example.jul.Service", null)
    }

    @Test
    fun `level survives garbage collection and can be reset`() {
        backend.setLevel("com.example.jul.Service", LogLevel.DEBUG)
        System.gc()
        assertEquals(Level.FINE, Logger.getLogger("com.example.jul.Service").level)
        assertTrue(Logger.getLogger("com.example.jul.Service").isLoggable(Level.FINE))

        backend.setLevel("com.example.jul.Service", null)
        assertNull(backend.getLogger("com.example.jul.Service").configuredLevel)
    }

    @Test
    fun `effective level is inherited from the parent`() {
        val child = Logger.getLogger("com.example.jul.Child")
        backend.setLevel("com.example.jul", LogLevel.ERROR)
        val info = backend.getLogger(child.name)
        assertNull(info.configuredLevel)
        assertEquals(LogLevel.ERROR, info.effectiveLevel)
        assertTrue(backend.getLoggers().any { it.name == "ROOT" })
    }
}
