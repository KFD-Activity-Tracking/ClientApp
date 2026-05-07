import database.saveAppAction
import database.saveKeyboardAction
import database.saveMouseAction
import database.saveMouseClickAction
import network.startBatchSender
import tracker.InputTrackerFactory
import tracker.LinuxEvdevTracker
import ui.TrackerWindow
import javax.swing.SwingUtilities

fun main(args: Array<String>) {
    database.init(reset = "--reset-db" in args)

    val tracker = InputTrackerFactory.create()
    tracker.onKeyPressed   = { keyCode -> saveKeyboardAction(keyCode) }
    tracker.onMouseClicked = { button, x, y -> saveMouseClickAction(button, x, y) }
    tracker.onMouseMoved   = { dx, dy  -> saveMouseAction(dx, dy) }
    tracker.onAppSwitched  = { appName -> saveAppAction(appName) }

    var senderThread: Thread? = null
    var window: TrackerWindow? = null

    SwingUtilities.invokeLater {
        window = TrackerWindow(
            onStart = { username, password ->
                window?.log("Подключение к серверу...")
                senderThread = startBatchSender(
                    username = username,
                    password = password,
                    onBatchError = { msg -> window?.log("⚠ $msg") },
                    onLogin = {
                        window?.log("Авторизация успешна. Отслеживание запущено.")
                        window?.setTracking(true)
                        tracker.start()
                        if (tracker is LinuxEvdevTracker && !tracker.keyboardAvailable) {
                            window?.log("⚠ Нет доступа к клавиатуре. Добавьте пользователя в группу input: sudo usermod -aG input \$USER")
                        }
                    },
                    onError = { msg ->
                        window?.log("Ошибка: $msg")
                        window?.setStatus(false, "Ошибка подключения")
                        window?.setTracking(false)
                    }
                )
            },
            onStop = {
                tracker.stop()
                senderThread?.interrupt()
                senderThread = null
            }
        )
    }

    Thread.sleep(Long.MAX_VALUE)
}