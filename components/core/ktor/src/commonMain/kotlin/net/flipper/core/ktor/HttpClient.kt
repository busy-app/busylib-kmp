package net.flipper.core.ktor

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.info

private val ktorTimber = TaggedLogger("Ktor")

fun getHttpClient() = getHttpClient(getPlatformEngineFactory())

fun <T : HttpClientEngineConfig> getHttpClient(
    engineFactory: HttpClientEngineFactory<T>
) = getHttpClient(engineFactory.create())

fun getHttpClient(
    engine: HttpClientEngine
) = HttpClient(engine) {
    val jsonSerializer = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    install(ContentNegotiation) {
        json(jsonSerializer)
    }

    install(DefaultRequest) {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
    }

    install(WebSockets) {
        pingInterval = WS_PING_INTERVAL
        contentConverter = LoggingWebsocketConverter(jsonSerializer)
    }

    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                ktorTimber.info { message }
            }
        }
        level = LogLevel.ALL
    }
}
