package network

import database.deleteSentActions
import database.readPendingActions
import kotlinx.coroutines.runBlocking
import metrics.SystemMetricsCollector

private const val BATCH_INTERVAL_MS = 30_000L

fun startBatchSender(
    username: String = "user",
    password: String = "user",
    onLogin: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    onBatchError: ((String) -> Unit)? = null,
): Thread = Thread {
    val client = ServerClient(username, password)
    val metricsCollector = SystemMetricsCollector()
    try {
        runBlocking {
            client.login()
            client.startSession()
        }
        onLogin?.invoke()
    } catch (e: Exception) {
        onError?.invoke(e.message ?: "login failed")
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
            }
        } catch (e: Exception) {
            onBatchError?.invoke("Ошибка отправки: ${e.message}")
        }
        metricsCollector.sample()
    }
    val (cpu, ram, gpu) = metricsCollector.averages()
    try { runBlocking { client.endSession(cpu, ram, gpu) } } catch (_: Exception) {}
    client.close()
}.also { it.isDaemon = true; it.start() }