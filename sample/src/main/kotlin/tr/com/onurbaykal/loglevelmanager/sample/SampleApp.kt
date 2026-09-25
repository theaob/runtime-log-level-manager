package tr.com.onurbaykal.loglevelmanager.sample

import tr.com.onurbaykal.loglevelmanager.LogLevelManager
import tr.com.onurbaykal.loglevelmanager.sample.billing.InvoiceService
import tr.com.onurbaykal.loglevelmanager.sample.orders.OrderService
import tr.com.onurbaykal.loglevelmanager.sample.orders.OrderRepository
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.util.Duration

class SampleApp : Application() {

    override fun start(stage: Stage) {
        val orders = OrderService(OrderRepository())
        val invoices = InvoiceService()
        Timeline(
            KeyFrame(Duration.seconds(1.0), {
                orders.placeOrder()
                invoices.createInvoice()
            }),
        ).apply {
            cycleCount = Timeline.INDEFINITE
            play()
        }

        val logList = ListView(UiLogAppender.lines)
        UiLogAppender.lines.addListener(javafx.collections.ListChangeListener { logList.scrollTo(UiLogAppender.lines.size - 1) })

        val menuBar = MenuBar(Menu("Tools", null, LogLevelManager.createMenuItem()))
        val openButton = Button("Open log levels").apply { setOnAction { LogLevelManager.show(stage) } }
        val hint = Label("Press Ctrl+Shift+L (Cmd+Shift+L on macOS) or use Tools → Log Levels…")

        val root = BorderPane(logList).apply {
            top = VBox(menuBar, HBox(12.0, openButton, hint).apply { style = "-fx-padding: 8; -fx-alignment: center-left;" })
        }
        stage.scene = Scene(root, 900.0, 500.0)
        LogLevelManager.install(stage.scene)
        stage.title = "Log Level Manager Sample"
        stage.show()
    }
}

fun main(args: Array<String>) = Application.launch(SampleApp::class.java, *args)
