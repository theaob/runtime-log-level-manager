package tr.com.onurbaykal.loglevel.ui

import tr.com.onurbaykal.loglevel.LogLevel
import tr.com.onurbaykal.loglevel.LogLevelManager
import tr.com.onurbaykal.loglevel.LoggerInfo
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.collections.FXCollections
import javafx.collections.transformation.FilteredList
import javafx.css.PseudoClass
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.control.Tooltip
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox

/**
 * Spring Boot Admin style logger list: filter loggers, click a level to set it, reset it to
 * inherit from the parent again, or set the level of any class or package by name.
 */
class LoggersView : BorderPane() {

    private val backend = LogLevelManager.backend
    private val loggers = FXCollections.observableArrayList<LoggerInfo>()
    private val filteredLoggers = FilteredList(loggers)
    private val table = TableView(filteredLoggers)

    private val filterField = TextField()
    private val onlyConfigured = CheckBox("Only configured")
    private val onlyModified = CheckBox("Only changed")
    private val revertButton = Button("Revert changes")
    private val statusLabel = Label()
    private val nameField = TextField()
    private val levelChoice = ComboBox(FXCollections.observableArrayList(backend.supportedLevels))

    init {
        styleClass += "log-level-manager"
        stylesheets += LoggersView::class.java.getResource("log-level-manager.css")!!.toExternalForm()

        top = VBox(createHeader(), createToolbar())
        center = createTable()
        bottom = createFooter()

        refresh()
        sceneProperty().addListener { _, _, scene -> if (scene != null) filterField.requestFocus() }
    }

    /** Reloads all loggers from the backend, keeping the current filter and selection. */
    fun refresh() {
        val selected = table.selectionModel.selectedItem?.name
        loggers.setAll(backend.getLoggers().sortedWith(compareBy<LoggerInfo> { !it.isRoot }.thenBy { it.name }))
        selected?.let(::select)
        updateStatus()
    }

    private fun createHeader(): Region {
        val title = Label("Loggers").apply { styleClass += "title" }
        val backendBadge = Label(backend.name).apply { styleClass += "badge" }
        val spacer = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }

        nameField.promptText = "com.example.MyService or com.example"
        nameField.prefColumnCount = 28
        nameField.setOnAction { applyFromNameField() }
        levelChoice.value = LogLevel.DEBUG.takeIf { it in backend.supportedLevels } ?: backend.supportedLevels.first()
        val setButton = Button("Set level").apply {
            styleClass += "primary"
            setOnAction { applyFromNameField() }
        }

        return HBox(8.0, title, backendBadge, spacer, nameField, levelChoice, setButton).apply {
            styleClass += "header"
            alignment = Pos.CENTER_LEFT
        }
    }

    private fun createToolbar(): Region {
        filterField.promptText = "Filter loggers…"
        filterField.styleClass += "filter"
        HBox.setHgrow(filterField, Priority.ALWAYS)
        filterField.textProperty().addListener { _, _, _ -> updateFilter() }
        filterField.addEventHandler(KeyEvent.KEY_PRESSED) { if (it.code == KeyCode.ESCAPE) filterField.clear() }
        onlyConfigured.selectedProperty().addListener { _, _, _ -> updateFilter() }
        onlyModified.selectedProperty().addListener { _, _, _ -> updateFilter() }

        revertButton.tooltip = Tooltip("Restore every logger changed here to the level it had before")
        revertButton.setOnAction { runAndRefresh { LogLevelManager.revertAll() } }
        val refreshButton = Button("Refresh").apply { setOnAction { refresh() } }

        return HBox(10.0, filterField, onlyConfigured, onlyModified, revertButton, refreshButton).apply {
            styleClass += "toolbar"
            alignment = Pos.CENTER_LEFT
        }
    }

    private fun createTable(): TableView<LoggerInfo> {
        val nameColumn = TableColumn<LoggerInfo, LoggerInfo>("Name").apply {
            setCellValueFactory { ReadOnlyObjectWrapper(it.value) }
            setCellFactory { NameCell() }
            isSortable = false
        }
        val levelColumn = TableColumn<LoggerInfo, LoggerInfo>("Level").apply {
            setCellValueFactory { ReadOnlyObjectWrapper(it.value) }
            setCellFactory { LevelCell() }
            isSortable = false
            val width = 58.0 * backend.supportedLevels.size + 80.0
            minWidth = width
            prefWidth = width
            maxWidth = width
        }
        table.columns.setAll(nameColumn, levelColumn)
        table.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
        table.placeholder = Label("No loggers match the filter")
        table.setRowFactory {
            object : TableRow<LoggerInfo>() {
                override fun updateItem(item: LoggerInfo?, empty: Boolean) {
                    super.updateItem(item, empty)
                    pseudoClassStateChanged(CONFIGURED, item?.isConfigured == true)
                    pseudoClassStateChanged(MODIFIED, item != null && LogLevelManager.isModified(item.name))
                }
            }
        }
        return table
    }

    private fun createFooter(): Region = HBox(statusLabel).apply {
        styleClass += "footer"
        alignment = Pos.CENTER_LEFT
    }

    private fun updateFilter() {
        val tokens = filterField.text.orEmpty().trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val configuredOnly = onlyConfigured.isSelected
        val modifiedOnly = onlyModified.isSelected
        filteredLoggers.setPredicate { logger ->
            val name = logger.name.lowercase()
            tokens.all { it in name } &&
                (!configuredOnly || logger.isConfigured) &&
                (!modifiedOnly || LogLevelManager.isModified(logger.name))
        }
        updateStatus()
    }

    private fun updateStatus(error: String? = null) {
        val modified = LogLevelManager.modifiedCount()
        revertButton.isDisable = modified == 0
        statusLabel.pseudoClassStateChanged(ERROR, error != null)
        statusLabel.text = error ?: buildString {
            append("Showing ${filteredLoggers.size} of ${loggers.size} loggers")
            if (modified > 0) append(" · $modified changed in this session")
        }
    }

    private fun applyFromNameField() {
        val name = nameField.text.orEmpty().trim()
        if (name.isEmpty() || name.any(Char::isWhitespace)) {
            updateStatus("Enter a logger name such as a fully qualified class or package name")
            return
        }
        if (runAndRefresh { LogLevelManager.setLevel(name, levelChoice.value) }) {
            nameField.clear()
            select(LogLevelManager.getLogger(name).name)
        }
    }

    private fun setLevel(name: String, level: LogLevel?) {
        runAndRefresh { LogLevelManager.setLevel(name, level) }
    }

    private fun runAndRefresh(action: () -> Unit): Boolean {
        val result = runCatching(action)
        refresh()
        result.exceptionOrNull()?.let { updateStatus("Could not change level: ${it.message ?: it.javaClass.simpleName}") }
        return result.isSuccess
    }

    private fun select(name: String) {
        var index = filteredLoggers.indexOfFirst { it.name == name }
        if (index < 0 && loggers.any { it.name == name }) {
            filterField.clear()
            onlyConfigured.isSelected = false
            onlyModified.isSelected = false
            index = filteredLoggers.indexOfFirst { it.name == name }
        }
        if (index >= 0) {
            table.selectionModel.select(index)
            table.scrollTo(maxOf(0, index - 3))
        }
    }

    private inner class NameCell : TableCell<LoggerInfo, LoggerInfo>() {
        init {
            styleClass += "name-cell"
            setOnMouseClicked { event ->
                if (event.clickCount == 2) item?.let { Clipboard.getSystemClipboard().setContent(ClipboardContent().apply { putString(it.name) }) }
            }
        }

        override fun updateItem(item: LoggerInfo?, empty: Boolean) {
            super.updateItem(item, empty)
            text = if (empty || item == null) null else item.name
            tooltip = if (empty || item == null) null else Tooltip(
                "${item.name}\nEffective: ${item.effectiveLevel}\nConfigured: ${item.configuredLevel ?: "inherited"}\n(double-click to copy the name)",
            )
        }
    }

    private inner class LevelCell : TableCell<LoggerInfo, LoggerInfo>() {
        private val levels = backend.supportedLevels
        private val buttons = levels.mapIndexed { index, level ->
            Button(level.name).apply {
                styleClass += listOf("level-button", "level-${level.name.lowercase()}")
                if (index == 0) styleClass += "first"
                if (index == levels.lastIndex) styleClass += "last"
                isFocusTraversable = false
                setOnAction { item?.let { setLevel(it.name, level) } }
            }
        }
        private val resetButton = Button("Reset").apply {
            styleClass += "reset-button"
            isFocusTraversable = false
            tooltip = Tooltip("Inherit the level from the parent logger")
            setOnAction { item?.let { setLevel(it.name, null) } }
        }
        private val content = HBox(10.0, HBox(*buttons.toTypedArray()).apply { styleClass += "level-group" }, resetButton).apply {
            alignment = Pos.CENTER_LEFT
        }

        init {
            styleClass += "level-cell"
            contentDisplay = javafx.scene.control.ContentDisplay.GRAPHIC_ONLY
        }

        override fun updateItem(item: LoggerInfo?, empty: Boolean) {
            super.updateItem(item, empty)
            if (empty || item == null) {
                graphic = null
                return
            }
            levels.forEachIndexed { index, level ->
                buttons[index].pseudoClassStateChanged(EFFECTIVE, level == item.effectiveLevel)
                buttons[index].pseudoClassStateChanged(CONFIGURED, level == item.configuredLevel)
            }
            resetButton.isVisible = item.isConfigured && !item.isRoot
            graphic = content
        }
    }

    private companion object {
        val EFFECTIVE: PseudoClass = PseudoClass.getPseudoClass("effective")
        val CONFIGURED: PseudoClass = PseudoClass.getPseudoClass("configured")
        val MODIFIED: PseudoClass = PseudoClass.getPseudoClass("modified")
        val ERROR: PseudoClass = PseudoClass.getPseudoClass("error")
    }
}
