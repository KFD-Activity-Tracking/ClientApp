package network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val BASE_URL = "http://localhost:8765"

@Serializable
private data class LoginRequest(val username: String, val password: String)

@Serializable
private data class TokenResponse(val token: String)

@Serializable
private data class SessionResponse(val sessionId: Long)

@Serializable
data class SessionMetricsDto(val avgCpu: Double, val avgRam: Double, val avgGpu: Double)

class ServerClient(
    private val username: String = "user",
    private val password: String = "user"
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private var token: String? = null

    suspend fun login() {
        token = client.post("$BASE_URL/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username, password))
        }.body<TokenResponse>().token
    }

    suspend fun sendBatch(actions: List<ActionDto>) {
        val t = token ?: error("Not logged in")
        val response = client.post("$BASE_URL/api/actions/addAll") {
            header(HttpHeaders.Authorization, "Bearer $t")
            contentType(ContentType.Application.Json)
            setBody(actions)
        }
        if (!response.status.isSuccess()) {
            error("Server rejected batch: ${response.status.value}")
        }
    }

    suspend fun startSession(): Long {
        val t = token ?: error("Not logged in")
        return client.post("$BASE_URL/api/statistics/sessions/start") {
            header(HttpHeaders.Authorization, "Bearer $t")
        }.body<SessionResponse>().sessionId
    }

    suspend fun endSession(cpuAvg: Double = 0.0, ramAvg: Double = 0.0, gpuAvg: Double = 0.0) {
        val t = token ?: return
        client.post("$BASE_URL/api/statistics/sessions/end") {
            header(HttpHeaders.Authorization, "Bearer $t")
            contentType(ContentType.Application.Json)
            setBody(SessionMetricsDto(cpuAvg, ramAvg, gpuAvg))
        }
    }

    fun close() = client.close()
}