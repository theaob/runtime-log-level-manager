package tr.com.onurbaykal.loglevelmanager.sample.legacy

import java.util.logging.Level
import java.util.logging.Logger
import kotlin.random.Random

/** Logs through java.util.logging, like JavaFX and parts of the JDK do. */
class LegacyExporter {
    private val log = Logger.getLogger(LegacyExporter::class.java.name)

    fun export() {
        val rows = Random.nextInt(1, 50)
        log.log(Level.FINE, "Exporting {0} rows", rows)
        log.log(Level.INFO, "Export finished, {0} rows", rows)
        if (rows > 45) log.warning("Export took longer than expected")
    }
}
