package net.flipper.bridge.connection.transport.mock.impl

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import net.flipper.bridge.connection.transport.common.utils.toRawHttpRequestString
import net.flipper.bridge.connection.transport.mock.impl.model.ApiAccountResponse
import net.flipper.bridge.connection.transport.mock.impl.model.ApiBleStatusResponse
import net.flipper.bridge.connection.transport.mock.impl.model.ApiScreenResponse
import net.flipper.bridge.connection.transport.mock.impl.model.ApiStatusPowerResponse
import net.flipper.bridge.connection.transport.mock.impl.model.ApiStatusResponse
import net.flipper.bridge.connection.transport.mock.impl.model.ApiWifiConnectResponse
import net.flipper.bridge.connection.transport.mock.impl.model.ApiWifiDisconnectResponse
import net.flipper.bridge.connection.transport.mock.impl.model.ApiWifiNetworksResponse
import net.flipper.bridge.connection.transport.mock.impl.model.ApiWifiStatusResponse
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.info

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

        ApiAccountResponse.PATH -> {
            delay(DEFAULT_DELAY)
            ApiAccountResponse.getJsonPlainTextResponse()
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
