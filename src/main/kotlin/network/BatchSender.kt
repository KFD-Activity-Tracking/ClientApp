package network

import database.deleteSentActions
import database.readPendingActions
import kotlinx.coroutines.runBlocking

private const val BATCH_INTERVAL_MS = 30_000L

fun startBatchSender(username: String = "user", password: String = "user") {
    Thread {
        val client = ServerClient(username, password)
        try {
            runBlocking { client.login() }
        } catch (e: Exception) {
            println("Server login failed: ${e.message}")
            return@Thread
        }

        while (!Thread.currentThread().isInterrupted) {
            Thread.sleep(BATCH_INTERVAL_MS)
            try {
                val pending = readPendingActions()
                if (pending.isNotEmpty()) {
                    runBlocking { client.sendBatch(pending.map { it.dto }) }
                    deleteSentActions(pending.map { it.id })
                }
            } catch (e: Exception) {
                println("Batch send failed: ${e.message}")
            }
        }
        client.close()
    }.also { it.isDaemon = true; it.start() }
}