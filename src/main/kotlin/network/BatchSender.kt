package network

import database.deleteSentActions
import database.readPendingActions
import kotlinx.coroutines.runBlocking
import metrics.SystemMetricsCollector
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private const val BATCH_INTERVAL_MS = 30_000L
private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss")

private fun now() = LocalTime.now().format(TIME_FMT)

fun startBatchSender(
    username: String = "user",
    password: String = "user",
    onLogin: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    onBatchError: ((String) -> Unit)? = null,
    onLog: ((String) -> Unit)? = null,
): Thread = Thread {
    val client = ServerClient(username, password)
    val metricsCollector = SystemMetricsCollector()

    try {
        onLog?.invoke("[${now()}] Подключение к серверу...")
        runBlocking {
            client.login()
            onLog?.invoke("[${now()}] Авторизация успешна")
            val sessionId = client.startSession()
            onLog?.invoke("[${now()}] Сессия #$sessionId открыта")
        }
        onLogin?.invoke()
        metricsCollector.sample()
    } catch (e: Exception) {
        onError?.invoke(e.message ?: "Ошибка входа")
        client.close()
        return@Thread
    }

    while (!Thread.currentThread().isInterrupted) {
        try {
            Thread.sleep(BATCH_INTERVAL_MS)
        } catch (_: InterruptedException) {
            break
        }

        try {
            val pending = readPendingActions()
            if (pending.isNotEmpty()) {
                runBlocking { client.sendBatch(pending.map { it.dto }) }
                deleteSentActions(pending.map { it.id })
                onLog?.invoke("[${now()}] Отправлено ${pending.size} событий")
            } else {
                onLog?.invoke("[${now()}] Нет новых событий")
            }
        } catch (e: Exception) {
            onBatchError?.invoke("Ошибка отправки: ${e.message}")
        }

        metricsCollector.sample()
        val (cpu, ram, gpu) = metricsCollector.averages()
        onLog?.invoke("[${now()}] CPU ${cpu.fmt()}%  RAM ${ram.fmt()}%  GPU ${gpu.fmt()}%")
    }

    onLog?.invoke("[${now()}] Остановка — отправка оставшихся событий...")
    try {
        val pending = readPendingActions()
        if (pending.isNotEmpty()) {
            runBlocking { client.sendBatch(pending.map { it.dto }) }
            deleteSentActions(pending.map { it.id })
            onLog?.invoke("[${now()}] Отправлено ${pending.size} событий")
        }
    } catch (_: Exception) {}

    val (cpu, ram, gpu) = metricsCollector.averages()
    onLog?.invoke("[${now()}] Завершение сессии (CPU ${cpu.fmt()}%  RAM ${ram.fmt()}%  GPU ${gpu.fmt()}%)")
    try { runBlocking { client.endSession(cpu, ram, gpu) } } catch (_: Exception) {}
    client.close()
    onLog?.invoke("[${now()}] Сессия закрыта")
}.also { it.isDaemon = true; it.start() }

private fun Double.fmt() = "%.1f".format(this)
