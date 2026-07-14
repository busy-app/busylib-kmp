package net.flipper.bridge.connection.feature.rpc.impl.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import net.flipper.bridge.connection.feature.rpc.impl.util.throttle.HttpRequestThrottle
import net.flipper.busylib.kmp.components.core.buildkonfig.BuildKonfig
import net.flipper.core.busylib.log.info
import net.flipper.core.ktor.util.minimizeBodyLogMessage
import kotlin.time.Duration.Companion.seconds

private const val MAX_RPS = 2 // Limitation for BUSY Bar request

fun getHttpClient(httpClientEngine: HttpClientEngine) = HttpClient(httpClientEngine) {
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
    install(HttpRequestRetry) {
        retryIf(maxRetries = Int.MAX_VALUE) { _, response ->
            response.status == HttpStatusCode.TooManyRequests
        }
        exponentialDelay(respectRetryAfterHeader = true)
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                val mappedMessage = minimizeBodyLogMessage(message)
                info { mappedMessage }
            }
        }
        level = if (BuildKonfig.IS_VERBOSE_LOG_ENABLED) LogLevel.ALL else LogLevel.INFO
    }
    install(HttpRequestThrottle) {
        throttler(
            limit = MAX_RPS,
            refillPeriod = 1.seconds,
        )
    }
}
