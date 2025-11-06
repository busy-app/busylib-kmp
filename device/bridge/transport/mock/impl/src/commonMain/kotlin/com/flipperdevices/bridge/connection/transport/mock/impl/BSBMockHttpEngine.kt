package com.flipperdevices.bridge.connection.transport.mock.impl

import com.flipperdevices.bridge.connection.transport.common.utils.toRawHttpRequestString
import com.flipperdevices.bridge.connection.transport.mock.impl.model.ApiBleStatusResponse
import com.flipperdevices.bridge.connection.transport.mock.impl.model.ApiScreenResponse
import com.flipperdevices.bridge.connection.transport.mock.impl.model.ApiStatusPowerResponse
import com.flipperdevices.bridge.connection.transport.mock.impl.model.ApiStatusResponse
import com.flipperdevices.bridge.connection.transport.mock.impl.model.ApiWifiConnectResponse
import com.flipperdevices.bridge.connection.transport.mock.impl.model.ApiWifiDisconnectResponse
import com.flipperdevices.bridge.connection.transport.mock.impl.model.ApiWifiNetworksResponse
import com.flipperdevices.bridge.connection.transport.mock.impl.model.ApiWifiStatusResponse
import com.flipperdevices.core.log.TaggedLogger
import com.flipperdevices.core.log.info
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.delay

private val logger = TaggedLogger("BSBMockHttpEngine")

const val SMALL_DELAY = 500L
const val DEFAULT_DELAY = 1000L

fun getBSBMockHttpEngine() = MockEngine { request ->
    logger.info {
        request.toRawHttpRequestString()
    }

    val response = when (request.url.encodedPath) {
        ApiStatusResponse.PATH -> ApiStatusResponse.getJsonPlainTextResponse()
        ApiStatusPowerResponse.PATH -> {
            delay(DEFAULT_DELAY)
            ApiStatusPowerResponse.getJsonPlainTextResponse()
        }

        ApiWifiNetworksResponse.PATH -> {
            delay(DEFAULT_DELAY)
            ApiWifiNetworksResponse.getJsonPlainTextResponse()
        }

        ApiWifiConnectResponse.PATH -> {
            delay(SMALL_DELAY)
            ApiWifiConnectResponse.getJsonPlainTextResponse()
        }

        ApiWifiDisconnectResponse.PATH -> {
            delay(SMALL_DELAY)
            ApiWifiDisconnectResponse.getJsonPlainTextResponse()
        }

        ApiWifiStatusResponse.PATH -> {
            delay(DEFAULT_DELAY)
            ApiWifiStatusResponse.getJsonPlainTextResponse()
        }

        ApiBleStatusResponse.PATH -> {
            delay(DEFAULT_DELAY)
            ApiBleStatusResponse.getJsonPlainTextResponse()
        }

        ApiScreenResponse.PATH -> {
            delay(DEFAULT_DELAY)
            ApiScreenResponse.getJsonPlainTextResponse()
        }

        else -> return@MockEngine respond(
            content = "",
            status = HttpStatusCode.NotFound
        )
    }

    respond(
        content = response,
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json")
    )
}
