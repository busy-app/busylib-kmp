package net.flipper.bridge.connection.transport.ble.impl

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
import io.ktor.http.cio.ParserException
import io.ktor.http.cio.parseResponse
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.core.BytePacketBuilder
import io.ktor.utils.io.core.remaining
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.InternalIoApi
import kotlinx.io.Source
import kotlinx.io.readByteArray
import net.flipper.bridge.connection.transport.ble.impl.exception.BadHttpResponseException
import net.flipper.bridge.connection.transport.ble.impl.serial.FSerialBleApi
import net.flipper.bridge.connection.transport.common.api.serial.attributes.IgnoreRequestTimeoutKey
import net.flipper.bridge.connection.transport.common.utils.toRawHttpRequest
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.ktx.common.withLockResult
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.verbose
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

private val REQUEST_TIMEOUT = 10.seconds

class FHttpBLEEngine(
    private val serialApi: FSerialBleApi,
) : HttpClientEngineBase("ble-serial"), LogTagProvider {
    private var requestCount = 0
    override val TAG = "FHttpBLEEngine"

    override val config = HttpClientEngineConfig()
    private val mutex = Mutex()

    override fun close() {
        super.close()
        // Need to cancel HttpClientEngineBase's scope
        cancel()
    }

    @InternalAPI
    override suspend fun execute(data: HttpRequestData): HttpResponseData {
        val processedRequest = getProcessedRequest(data)
        val requestTime = GMTDate()
        info { "Want to request: ${processedRequest.url}" }
        val result = withLockResult(mutex, "execute") {
            info { "Execute request: ${processedRequest.url}" }
            checkRequestCountUnsafe()
            verbose { "Send request: $processedRequest" }
            val rawBytes = processedRequest.toRawHttpRequest()
            verbose { "Raw data is: ${rawBytes.decodeToString()}" }
            val channel = serialApi.getReceiveByteChannel()

            val withTimeout = data.attributes.getOrNull(IgnoreRequestTimeoutKey)?.not() ?: true

            if (!withTimeout) {
                info { "Warning: execute request without timeout" }
            }

            val result = sendBytes(rawBytes, channel, requestTime, withTimeout = withTimeout)
            return@withLockResult if (result == null) {
                error {
                    "Failed to wait ${REQUEST_TIMEOUT.inWholeSeconds} seconds for response," +
                        " try to make this request again after reset"
                }
                resetSerialApi()
                checkRequestCountUnsafe()
                sendBytes(rawBytes, channel, requestTime, withTimeout = withTimeout)
                    ?: error("Timeout on request ${processedRequest.url}")
            } else {
                result
            }
        }

        return result
    }

    @InternalAPI
    private suspend fun sendBytes(
        bytes: ByteArray,
        channel: ByteReadChannel,
        requestTime: GMTDate,
        withTimeout: Boolean
    ): HttpResponseData? {
        return withContext(NonCancellable) {
            if (withTimeout) {
                withTimeoutOrNull(REQUEST_TIMEOUT) {
                    sendBytesUnsafe(bytes, channel, requestTime)
                }
            } else {
                sendBytesUnsafe(bytes, channel, requestTime)
            }
        }
    }

    @InternalAPI
    private suspend fun sendBytesUnsafe(
        bytes: ByteArray,
        channel: ByteReadChannel,
        requestTime: GMTDate
    ): HttpResponseData? {
        serialApi.send(bytes)
        info { "Waiting for response" }
        val response = parseRawHttpResponse(
            channel = channel,
            requestTime = requestTime,
            callContext = callContext()
        )
        info { "Receive response" }
        return response
    }

    private suspend fun resetSerialApi() {
        serialApi.reset()
        requestCount = 0
    }

    private suspend fun checkRequestCountUnsafe() {
        val deviceRequestCount = serialApi.getRequestCounterFlow().first()
        if (requestCount < deviceRequestCount) {
            error { "Received request count: $deviceRequestCount, but current request count is $requestCount" }
            resetSerialApi()
        }
        requestCount++
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
        val response = runSuspendCatching {
            parseResponse(channel)
        }.onFailure { t ->
            if (t is ParserException) {
                error(t) { "Parser exception, make reset" }
                resetSerialApi()
            }
        }.getOrThrow() ?: throw BadHttpResponseException()
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
    while (currentCoroutineContext().isActive && !isClosedForRead && remaining > 0) {
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
