package net.flipper.bridge.connection.feature.rpc.impl.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import net.flipper.core.busylib.log.info
import net.flipper.core.ktor.util.minimizeBodyLogMessage

internal fun getHttpClient(httpClientEngine: HttpClientEngine) = HttpClient(httpClientEngine) {
    install(WebSockets)
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                coerceInputValues = true
            }
        )
    }
    defaultRequest {
        contentType(ContentType.Application.Json)
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                val mappedMessage = minimizeBodyLogMessage(message)
                info { mappedMessage }
            }
        }
        level = LogLevel.ALL
    }
}
