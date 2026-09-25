package tr.com.onurbaykal.loglevel.sample

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Logback appender that shows the last log lines in the sample window. */
class UiLogAppender : AppenderBase<ILoggingEvent>() {

    override fun append(event: ILoggingEvent) {
        val time = TIME.format(Instant.ofEpochMilli(event.timeStamp))
        val line = "$time %-5s %s - %s".format(event.level, event.loggerName.substringAfterLast('.'), event.formattedMessage)
        Platform.runLater {
            lines += line
            if (lines.size > MAX_LINES) lines.remove(0, lines.size - MAX_LINES)
        }
    }

    companion object {
        private const val MAX_LINES = 500
        private val TIME = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
        val lines: ObservableList<String> = FXCollections.observableArrayList()
    }
}
