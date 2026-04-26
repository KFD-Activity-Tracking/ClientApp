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
        client.post("$BASE_URL/api/actions/addAll") {
            header(HttpHeaders.Authorization, "Bearer $t")
            contentType(ContentType.Application.Json)
            setBody(actions)
        }
    }

    fun close() = client.close()
}