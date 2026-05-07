package tracker

import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.File

class LinuxEvdevTracker : InputTracker {

    override var onKeyPressed: ((keyCode: Int) -> Unit)? = null
    override var onMouseMoved: ((deltaX: Int, deltaY: Int) -> Unit)? = null
    override var onMouseClicked: ((button: Int, x: Float, y: Float) -> Unit)? = null

    private val screenSize by lazy { java.awt.Toolkit.getDefaultToolkit().screenSize }

    private fun getCursorPosition(): Pair<Int, Int>? = try {
        when {
            System.getenv("HYPRLAND_INSTANCE_SIGNATURE") != null -> {
                val out = ProcessBuilder("hyprctl", "cursorpos")
                    .start().inputStream.bufferedReader().readText().trim()
                val parts = out.split(",").mapNotNull { it.trim().toIntOrNull() }
                if (parts.size >= 2) Pair(parts[0], parts[1]) else null
            }
            System.getenv("DISPLAY") != null -> {
                val out = ProcessBuilder("xdotool", "getmouselocation")
                    .start().inputStream.bufferedReader().readText().trim()
                val x = out.substringAfter("x:").substringBefore(" ").toIntOrNull()
                val y = out.substringAfter("y:").substringBefore(" ").toIntOrNull()
                if (x != null && y != null) Pair(x, y) else null
            }
            else -> null
        }
    } catch (e: Exception) { null }
    override var onAppSwitched: ((appName: String) -> Unit)? = null

    private val eventSize = 24
    private val evKey = 1.toShort()
    private val evAbs = 3.toShort()
    private val absX = 0.toShort()
    private val absY = 1.toShort()

    @Volatile private var running = false
    private var threads = mutableListOf<Thread>()
    var keyboardAvailable: Boolean = false
        private set

    private fun findDevices(predicate: (Long) -> Boolean): List<String> {
        val dir = File("/dev/input")
        return dir.listFiles()
            ?.filter { file ->
                if (!file.name.startsWith("event")) return@filter false
                val capFile = File("/sys/class/input/${file.name}/device/capabilities/ev")
                val mask = capFile.readText().trim().toLongOrNull(16) ?: return@filter false
                predicate(mask)
            }
            ?.map { it.absolutePath }
            ?: emptyList()
    }

    override fun start() {
        running = true

        val isKeyboard = { mask: Long ->
            (mask shr 1) and 1L == 1L &&
            (mask shr 3) and 1L == 0L
        }

        // Check keyboard access synchronously before spawning threads
        keyboardAvailable = findDevices(isKeyboard).any { device ->
            try { FileInputStream(device).close(); true } catch (e: Exception) { false }
        }

        val isPointer = { mask: Long ->
            (mask shr 3) and 1L == 1L ||
            (mask shr 2) and 1L == 1L
        }

        findDevices(isKeyboard).forEach { device ->
            Thread {
                try {
                    FileInputStream(device).use { stream ->
                        val buf = ByteArray(eventSize)
                        while (running) {
                            val read = stream.read(buf)
                            if (read < eventSize) continue
                            val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
                            bb.position(16)
                            val type = bb.short
                            val code = bb.short
                            val value = bb.int
                            if (type == evKey && value == 1) {
                                onKeyPressed?.invoke(code.toInt())
                            }
                        }
                    }
                } catch (e: Exception) {
                    System.err.println("LinuxEvdevTracker: cannot open keyboard $device: ${e.message}")
                }
            }.also { it.isDaemon = true; it.start(); threads.add(it) }
        }

        findDevices(isPointer).forEach { device ->
            Thread {
                try {
                    var prevX = -1
                    var prevY = -1
                    FileInputStream(device).use { stream ->
                        val buf = ByteArray(eventSize)
                        while (running) {
                            val read = stream.read(buf)
                            if (read < eventSize) continue
                            val bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
                            bb.position(16)
                            val type = bb.short
                            val code = bb.short
                            val value = bb.int
                            when (type) {
                                evAbs -> {
                                    when (code) {
                                        absX -> {
                                            if (prevX != -1) onMouseMoved?.invoke(value - prevX, 0)
                                            prevX = value
                                        }
                                        absY -> {
                                            if (prevY != -1) onMouseMoved?.invoke(0, value - prevY)
                                            prevY = value
                                        }
                                    }
                                }
                                evKey -> {
                                    if (value == 1) {
                                        val pos = getCursorPosition()
                                        val sw = screenSize.width.coerceAtLeast(1)
                                        val sh = screenSize.height.coerceAtLeast(1)
                                        val nx = (pos?.first?.toFloat() ?: 0f) / sw
                                        val ny = (pos?.second?.toFloat() ?: 0f) / sh
                                        onMouseClicked?.invoke(code.toInt(), nx.coerceIn(0f, 1f), ny.coerceIn(0f, 1f))
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    System.err.println("LinuxEvdevTracker: cannot open pointer $device: ${e.message}")
                }
            }.also { it.isDaemon = true; it.start(); threads.add(it) }
        }

        Thread {
            var prevApp: String? = null
            while (running) {
                val currentApp = getActiveApp()
                if (prevApp != null && prevApp != currentApp) {
                    onAppSwitched?.invoke(currentApp ?: "No active app")
                }
                prevApp = currentApp
                Thread.sleep(1000)
            }
        }.also { it.isDaemon = true; it.start(); threads.add(it) }
    }

    private fun getActiveApp(): String? {
        return try {
            when {
                System.getenv("HYPRLAND_INSTANCE_SIGNATURE") != null -> {
                    val output = ProcessBuilder("hyprctl", "activewindow", "-j")
                        .start()
                        .inputStream.bufferedReader().readText().trim()
                    output.substringAfter("\"class\": \"").substringBefore("\"")
                }
                System.getenv("DISPLAY") != null -> {
                    ProcessBuilder("xdotool", "getactivewindow", "getwindowname")
                        .start()
                        .inputStream.bufferedReader().readText().trim()
                }
                else -> null
            }
        } catch (e: Exception) {
            println("getActiveApp error: ${e.message}")
            null
        }
    }

    override fun stop() {
        running = false
        threads.forEach { it.interrupt() }
        threads.clear()
    }
}