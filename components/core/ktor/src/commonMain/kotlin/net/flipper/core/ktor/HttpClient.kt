package net.flipper.core.ktor

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import net.flipper.busylib.kmp.components.core.buildkonfig.BuildKonfig
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.info
import net.flipper.core.ktor.util.minimizeBodyLogMessage
import kotlin.time.Duration.Companion.seconds

private val ktorTimber = TaggedLogger("Ktor")

fun getHttpClient() = getHttpClient(getPlatformEngineFactory())

fun <T : HttpClientEngineConfig> getHttpClient(
    engineFactory: HttpClientEngineFactory<T>
) = getHttpClient(engineFactory.create())

val WS_PING_INTERVAL = 20.seconds

fun getHttpClient(
    engine: HttpClientEngine
) = HttpClient(engine) {
    val jsonSerializer = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    install(ContentNegotiation) {
        json(jsonSerializer)
    }

    install(DefaultRequest) {
        header(HttpHeaders.ContentType, ContentType.Application.Json)
    }

    install(HttpRequestRetry) {
        retryIf(maxRetries = Int.MAX_VALUE) { _, response ->
            response.status == HttpStatusCode.TooManyRequests
        }
        exponentialDelay(respectRetryAfterHeader = true)
    }
    install(WebSockets) {
        pingInterval = WS_PING_INTERVAL
        contentConverter = LoggingWebsocketConverter(jsonSerializer)
    }

    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                val mappedMessage = minimizeBodyLogMessage(message)
                ktorTimber.info { mappedMessage }
            }
        }
        level = if (BuildKonfig.IS_VERBOSE_LOG_ENABLED) LogLevel.ALL else LogLevel.INFO
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 30.seconds.inWholeMilliseconds
        requestTimeoutMillis = 30.seconds.inWholeMilliseconds
        socketTimeoutMillis = 30.seconds.inWholeMilliseconds
    }
}
