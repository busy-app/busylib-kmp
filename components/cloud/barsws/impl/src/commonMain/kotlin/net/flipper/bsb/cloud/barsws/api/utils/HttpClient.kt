package net.flipper.bsb.cloud.barsws.api.utils

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import net.flipper.core.ktor.WS_PING_INTERVAL
import net.flipper.core.ktor.getPlatformEngineFactory

fun getHttpClient() = HttpClient(getPlatformEngineFactory()) {
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
}
