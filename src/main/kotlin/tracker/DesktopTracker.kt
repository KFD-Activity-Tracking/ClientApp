package tracker

import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent
import com.github.kwhat.jnativehook.mouse.NativeMouseListener
import com.github.kwhat.jnativehook.mouse.NativeMouseMotionListener
import java.util.logging.Level
import java.util.logging.Logger

abstract class DesktopTracker : InputTracker, NativeKeyListener, NativeMouseListener, NativeMouseMotionListener {

    override var onKeyPressed: ((keyCode: Int) -> Unit)? = null
    override var onMouseMoved: ((deltaX: Int, deltaY: Int) -> Unit)? = null
    override var onMouseClicked: ((button: Int) -> Unit)? = null
    override var onAppSwitched: ((appName: String) -> Unit)? = null

    private var lastMouseX: Int = 0
    private var lastMouseY: Int = 0

    @Volatile private var running = false
    private var appPollThread: Thread? = null

    protected abstract fun getActiveApp(): String?

    override fun start() {
        Logger.getLogger(GlobalScreen::class.java.`package`.name).apply {
            level = Level.OFF
            useParentHandlers = false
        }
        GlobalScreen.registerNativeHook()
        GlobalScreen.addNativeKeyListener(this)
        GlobalScreen.addNativeMouseListener(this)
        GlobalScreen.addNativeMouseMotionListener(this)

        running = true
        appPollThread = Thread {
            var prevApp: String? = null
            while (running) {
                val currentApp = getActiveApp()
                if (prevApp != null && prevApp != currentApp) {
                    onAppSwitched?.invoke(currentApp ?: "unknown")
                }
                prevApp = currentApp
                Thread.sleep(1000)
            }
        }.also { it.isDaemon = true; it.start() }
    }

    override fun stop() {
        running = false
        appPollThread?.interrupt()
        GlobalScreen.removeNativeKeyListener(this)
        GlobalScreen.removeNativeMouseListener(this)
        GlobalScreen.removeNativeMouseMotionListener(this)
        GlobalScreen.unregisterNativeHook()
    }

    override fun nativeKeyPressed(e: NativeKeyEvent) { onKeyPressed?.invoke(e.keyCode) }
    override fun nativeKeyReleased(e: NativeKeyEvent) {}
    override fun nativeKeyTyped(e: NativeKeyEvent) {}

    override fun nativeMouseClicked(e: NativeMouseEvent) {}
    override fun nativeMousePressed(e: NativeMouseEvent) { onMouseClicked?.invoke(e.button) }
    override fun nativeMouseReleased(e: NativeMouseEvent) {}

    override fun nativeMouseMoved(e: NativeMouseEvent) {
        val dx = e.x - lastMouseX
        val dy = e.y - lastMouseY
        lastMouseX = e.x
        lastMouseY = e.y
        onMouseMoved?.invoke(dx, dy)
    }

    override fun nativeMouseDragged(e: NativeMouseEvent) {}
}