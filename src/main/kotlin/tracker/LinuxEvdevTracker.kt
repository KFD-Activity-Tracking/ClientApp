package tracker

import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.File

class LinuxEvdevTracker : InputTracker {

    override var onKeyPressed: ((keyCode: Int) -> Unit)? = null
    override var onMouseMoved: ((deltaX: Int, deltaY: Int) -> Unit)? = null
    override var onMouseClicked: ((button: Int) -> Unit)? = null
    override var onAppSwitched: ((appName: String) -> Unit)? = null

    private val eventSize = 24
    private val evKey = 1.toShort()
    private val evAbs = 3.toShort()
    private val absX = 0.toShort()
    private val absY = 1.toShort()

    @Volatile private var running = false
    private var threads = mutableListOf<Thread>()

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

        val isPointer = { mask: Long ->
            (mask shr 3) and 1L == 1L ||
            (mask shr 2) and 1L == 1L
        }

        findDevices(isKeyboard).forEach { device ->
            Thread {
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
            }.also { it.isDaemon = true; it.start(); threads.add(it) }
        }

        findDevices(isPointer).forEach { device ->
            Thread {
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
                                if (value == 1) onMouseClicked?.invoke(code.toInt())
                            }
                        }
                    }
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