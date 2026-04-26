import database.saveAppAction
import database.saveKeyboardAction
import database.saveMouseAction
import database.saveMouseClickAction
import network.startBatchSender
import tracker.InputTrackerFactory

fun main() {
    database.init()
    startBatchSender()

    val tracker = InputTrackerFactory.create()
    tracker.onKeyPressed = { keyCode -> saveKeyboardAction(keyCode) }
    tracker.onMouseClicked = { button -> saveMouseClickAction(button) }
    tracker.onMouseMoved = { dx, dy -> saveMouseAction(dx, dy) }
    tracker.onAppSwitched = { appName -> saveAppAction(appName) }

    tracker.start()
    Thread.sleep(Long.MAX_VALUE)
    tracker.stop()
}