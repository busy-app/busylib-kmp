package com.flipperdevices.bridge.connection.feature.rpc.impl.util

import com.flipperdevices.core.log.info
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal fun getHttpClient(httpClientEngine: HttpClientEngine) = HttpClient(httpClientEngine) {
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
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                info { message }
            }
        }
        level = LogLevel.ALL
    }
}
