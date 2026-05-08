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

    val username = argValue(args, "--username") ?: System.getenv("KFD_USERNAME")
    val password = argValue(args, "--password") ?: System.getenv("KFD_PASSWORD")
    val headless = "--headless" in args || (username != null && password != null)

    val tracker = InputTrackerFactory.create()
    tracker.onKeyPressed   = { keyCode -> saveKeyboardAction(keyCode) }
    tracker.onMouseClicked = { button, x, y -> saveMouseClickAction(button, x, y) }
    tracker.onMouseMoved   = { dx, dy  -> saveMouseAction(dx, dy) }
    tracker.onAppSwitched  = { appName -> saveAppAction(appName) }

    if (headless) {
        if (username.isNullOrBlank() || password.isNullOrBlank()) {
            println("Ошибка: укажите --username=<логин> --password=<пароль> или переменные KFD_USERNAME / KFD_PASSWORD")
            return
        }
        println("[KFD] Запуск в консольном режиме, пользователь: $username")
        startBatchSender(
            username = username,
            password = password,
            onLog        = { msg -> println(msg) },
            onBatchError = { msg -> println("⚠ $msg") },
            onLogin = {
                tracker.start()
                if (tracker is LinuxEvdevTracker && !tracker.keyboardAvailable) {
                    println("⚠ Нет доступа к клавиатуре. Добавьте пользователя в группу input: sudo usermod -aG input \$USER")
                }
            },
            onError = { msg -> println("✗ $msg") }
        )
        Runtime.getRuntime().addShutdownHook(Thread { tracker.stop() })
        Thread.sleep(Long.MAX_VALUE)
        return
    }

    var senderThread: Thread? = null
    var window: TrackerWindow? = null

    SwingUtilities.invokeLater {
        window = TrackerWindow(
            onStart = { u, p ->
                window?.log("Подключение к серверу...")
                senderThread = startBatchSender(
                    username = u,
                    password = p,
                    onLog        = { msg -> window?.log(msg) },
                    onBatchError = { msg -> window?.log("⚠ $msg") },
                    onLogin = {
                        window?.setTracking(true)
                        tracker.start()
                        if (tracker is LinuxEvdevTracker && !tracker.keyboardAvailable) {
                            window?.log("⚠ Нет доступа к клавиатуре: sudo usermod -aG input \$USER")
                        }
                    },
                    onError = { msg ->
                        window?.log("✗ $msg")
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

private fun argValue(args: Array<String>, key: String): String? =
    args.firstOrNull { it.startsWith("$key=") }?.removePrefix("$key=")
