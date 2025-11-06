package com.flipperdevices.bridge.connection.transport.mock.impl.api.http

import com.flipperdevices.bridge.connection.transport.common.utils.toRawHttpRequestString
import com.flipperdevices.bridge.connection.transport.mock.impl.api.http.serial.FSerialBleApi
import com.flipperdevices.bridge.connection.transport.mock.impl.exception.BadHttpResponseException
import com.flipperdevices.core.ktx.common.withLockResult
import com.flipperdevices.core.log.LogTagProvider
import com.flipperdevices.core.log.info
import io.ktor.client.engine.HttpClientEngineBase
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.callContext
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.takeFrom
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.cio.CIOHeaders
import io.ktor.http.cio.parseResponse
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.core.BytePacketBuilder
import io.ktor.utils.io.core.remaining
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.io.InternalIoApi
import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlin.coroutines.CoroutineContext

class FHttpBLEEngine(
    private val serialApi: FSerialBleApi
) : HttpClientEngineBase("ble-serial"), LogTagProvider {
    override val TAG = "FHttpBLEEngine"
    override val config = HttpClientEngineConfig()
    private val mutex = Mutex()

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val processedRequest = getProcessedRequest(data)
        val requestTime = GMTDate()
        info { "Want to request: ${processedRequest.url}" }
        val result = withLockResult(mutex, "execute") {
            info { "Send request: $processedRequest" }
            val rawText = processedRequest.toRawHttpRequestString()
            info { "Raw data is: $rawText" }
            val channel = serialApi.getReceiveByteChannel()

            withContext(NonCancellable) {
                serialApi.send(rawText.encodeToByteArray())

                parseRawHttpResponse(
                    channel = channel,
                    requestTime = requestTime,
                    callContext = callContext()
                )
            }
        }

        return result
    }

    private fun getProcessedRequest(data: HttpRequestData): HttpRequestData {
        val builder = HttpRequestBuilder()
            .takeFrom(data)
        builder.headers.append("Connection", "close")
        return builder.build()
    }

    private suspend fun parseRawHttpResponse(
        channel: ByteReadChannel,
        requestTime: GMTDate,
        callContext: CoroutineContext
    ): HttpResponseData {
        val response = parseResponse(channel) ?: throw BadHttpResponseException()
        val headers: Headers = CIOHeaders(response.headers)

        val contentLength = headers[HttpHeaders.ContentLength]?.toLongOrNull()
        val bodyBytes = if (contentLength != null) {
            channel.awaitContent(contentLength.toInt())
            channel.readRemainingInternal(contentLength).readByteArray()
        } else {
            error("Content-Length is null, it's not allowed in BLE")
        }

        val version = when (response.version.toString()) {
            "HTTP/1.0" -> HttpProtocolVersion.HTTP_1_0
            "HTTP/2", "HTTP/2.0" -> HttpProtocolVersion.HTTP_2_0
            else -> HttpProtocolVersion.HTTP_1_1
        }

        return HttpResponseData(
            statusCode = HttpStatusCode(response.status, response.statusText.toString()),
            requestTime = requestTime,
            headers = headers,
            version = version,
            body = ByteReadChannel(bodyBytes),
            callContext = callContext
        )
    }
}

@OptIn(InternalIoApi::class, InternalAPI::class)
public suspend fun ByteReadChannel.readRemainingInternal(max: Long): Source {
    val result = BytePacketBuilder()
    var remaining = max
    while (!isClosedForRead && remaining > 0) {
        awaitContent()

        if (remaining >= readBuffer.remaining) {
            remaining -= readBuffer.remaining
            readBuffer.transferTo(result)
        } else {
            readBuffer.readTo(result, remaining)
            remaining = 0
        }
    }

    return result.buffer
}
