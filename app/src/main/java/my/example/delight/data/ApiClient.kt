package my.example.delight.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.delete
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.parameters
import io.ktor.serialization.gson.GsonWebsocketContentConverter
import io.ktor.serialization.gson.gson
import io.ktor.utils.io.core.Closeable
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import my.example.delight.db.User

class ApiClient(
    private val host: String, private val port: Int = 80, secured: Boolean = true
) : Closeable {
    private val url = "http${if (secured) "s" else ""}://$host:$port"

    private val client = HttpClient {
        install(ContentNegotiation) {
            gson()
        }
        install(WebSockets) {
            contentConverter = GsonWebsocketContentConverter()
        }
    }

    suspend fun users(): List<User> = client.get("$url/users").body()

    suspend fun delete(id: Long) = client.delete("$url/user/$id").status

    suspend fun insert(name: String) =
        client.submitForm("$url/add", parameters {
            append("name", name)
        }).status

    suspend fun update(user: User) =
        client.post("$url/user") {
            contentType(ContentType.Application.Json)
            setBody(user)
        }.status

    data class DataAction <out T>(val action: Action, val data: T)
    enum class Action { Update, Insert, Delete }

    fun realtime() = flow {
        client.webSocket(HttpMethod.Get, host, port, "user") {
            //println("Socket start")
            while (coroutineContext.isActive) {
                runCatching {
                    receiveDeserialized<DataAction<User>>()
                }.onSuccess {
                    emit(it)
                }
            }
            //println("Socket end")
        }
    }

    override fun close() = client.close()
}