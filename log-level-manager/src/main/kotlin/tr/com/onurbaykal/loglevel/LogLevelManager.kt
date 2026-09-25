package tr.com.onurbaykal.loglevel

import tr.com.onurbaykal.loglevel.ui.LoggersView
import javafx.application.Platform
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.MenuItem
import javafx.scene.input.KeyCombination
import javafx.stage.Stage
import javafx.stage.Window
import kotlin.reflect.KClass

/**
 * Entry point of the library.
 *
 * ```kotlin
 * // Open the log level window with Ctrl+Shift+L (Cmd+Shift+L on macOS)
 * LogLevelManager.install(scene)
 *
 * // ...or from a menu
 * menu.items += LogLevelManager.createMenuItem()
 *
 * // ...or change levels in code
 * LogLevelManager.setLevel(MyService::class, LogLevel.DEBUG)
 * ```
 */
object LogLevelManager {

    /** Shortcut used by [install] when none is given: Ctrl+Shift+L, or Cmd+Shift+L on macOS. */
    @JvmField
    val DEFAULT_SHORTCUT: KeyCombination = KeyCombination.keyCombination("Shortcut+Shift+L")

    private val tracker = ChangeTracker()
    private val detectedBackend by lazy { LoggingBackends.detect() }

    @Volatile
    private var customBackend: LoggingBackend? = null

    private var stage: Stage? = null

    /** Backend used to read and change levels; detected automatically unless set explicitly. */
    @JvmStatic
    var backend: LoggingBackend
        get() = customBackend ?: detectedBackend
        set(value) {
            customBackend = value
            tracker.clear()
        }

    /** All loggers known to the backend. */
    @JvmStatic
    fun getLoggers(): List<LoggerInfo> = backend.getLoggers()

    @JvmStatic
    fun getLogger(name: String): LoggerInfo = backend.getLogger(name)

    /**
     * Sets the level of the logger with the given name (usually a class or package name).
     * `null` resets the logger so that it inherits its parent's level again.
     */
    @JvmStatic
    fun setLevel(name: String, level: LogLevel?) {
        val backend = backend
        val before = backend.getLogger(name)
        tracker.recordOriginal(before.name, before.configuredLevel)
        backend.setLevel(before.name, level)
        tracker.forgetIfUnchanged(before.name, backend.getLogger(before.name).configuredLevel)
    }

    @JvmStatic
    fun setLevel(type: Class<*>, level: LogLevel?) = setLevel(type.name, level)

    fun setLevel(type: KClass<*>, level: LogLevel?) = setLevel(type.java.name, level)

    /** Whether the logger was changed through this manager and not reverted yet. */
    @JvmStatic
    fun isModified(name: String): Boolean = tracker.isModified(LoggingBackend.normalizeName(name))

    /** Number of loggers changed through this manager and not reverted yet. */
    @JvmStatic
    fun modifiedCount(): Int = tracker.count()

    /** Restores every logger changed through this manager to the level it had before. */
    @JvmStatic
    fun revertAll() {
        val backend = backend
        tracker.drain().entries.reversed().forEach { (name, original) -> backend.setLevel(name, original) }
    }

    /** Creates a new log level view that can be embedded anywhere, e.g. in a `Tab` or `Dialog`. */
    @JvmStatic
    fun createView(): Parent = LoggersView()

    /**
     * Opens the log level window, or brings it to front if it is already open.
     * Can be called from any thread.
     *
     * @param owner window the log level window belongs to; it is used only when the window is first created
     */
    @JvmStatic
    @JvmOverloads
    fun show(owner: Window? = null) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater { show(owner) }
            return
        }
        val stage = stage ?: createStage(owner).also { stage = it }
        (stage.scene.root as LoggersView).refresh()
        if (stage.isIconified) stage.isIconified = false
        stage.show()
        stage.toFront()
    }

    /** Registers [shortcut] on [scene] to open the log level window. */
    @JvmStatic
    @JvmOverloads
    fun install(scene: Scene, shortcut: KeyCombination = DEFAULT_SHORTCUT) {
        scene.accelerators[shortcut] = Runnable { show(scene.window) }
    }

    /** Registers [shortcut] on the stage's current and future scenes to open the log level window. */
    @JvmStatic
    @JvmOverloads
    fun install(stage: Stage, shortcut: KeyCombination = DEFAULT_SHORTCUT) {
        stage.scene?.let { install(it, shortcut) }
        stage.sceneProperty().addListener { _, _, scene -> scene?.let { install(it, shortcut) } }
    }

    /** Creates a menu item that opens the log level window. */
    @JvmStatic
    @JvmOverloads
    fun createMenuItem(text: String = "Log Levels…", shortcut: KeyCombination? = DEFAULT_SHORTCUT): MenuItem =
        MenuItem(text).apply {
            accelerator = shortcut
            setOnAction { show(parentPopup?.ownerWindow?.let(::rootWindow)) }
        }

    private fun rootWindow(window: Window): Window {
        var current = window
        while (current is javafx.stage.PopupWindow) current = current.ownerWindow ?: break
        return current
    }

    private fun createStage(owner: Window?): Stage = Stage().apply {
        title = "Log Levels"
        scene = Scene(LoggersView(), 960.0, 640.0)
        minWidth = 640.0
        minHeight = 360.0
        if (owner != null) {
            initOwner(owner)
            (owner as? Stage)?.icons?.let { icons.setAll(it) }
        }
        focusedProperty().addListener { _, _, focused -> if (focused) (scene.root as LoggersView).refresh() }
    }
}

/** Kotlin shortcut for [LogLevelManager.install]. */
fun Scene.installLogLevelManager(shortcut: KeyCombination = LogLevelManager.DEFAULT_SHORTCUT) =
    LogLevelManager.install(this, shortcut)
