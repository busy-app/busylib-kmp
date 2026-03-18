package net.flipper.bridge.connection.transport.ble.impl

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.cio.ParserException
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import net.flipper.bridge.connection.transport.ble.impl.serial.FSerialBleApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class FHttpBLEEngineTest {
    @Test
    fun GIVEN_parser_exception_WHEN_engine_resets_serial_api_THEN_local_request_count_is_reset_to_zero() =
        runTest {
            val serialApi = FakeSerialBleApi().apply {
                enqueueResponse(ResponsePlan.ParserFailure)
                enqueueResponse(ResponsePlan.RawHttpResponse(validHttpResponse("OK")))
                requestCounterStateFlow.value = 0
            }
            val engine = FHttpBLEEngine(serialApi)
            val client = HttpClient(engine)

            try {
                assertFails {
                    client.get("http://busy.local/failure")
                }
                assertEquals(1, serialApi.resetCalls)

                serialApi.requestCounterStateFlow.value = 1

                assertEquals("OK", client.get("http://busy.local/success").bodyAsText())
                assertEquals(2, serialApi.resetCalls)
            } finally {
                client.close()
                engine.close()
            }
        }
}

private sealed interface ResponsePlan {
    data object ParserFailure : ResponsePlan
    data class RawHttpResponse(val payload: ByteArray) : ResponsePlan
}

private class FakeSerialBleApi : FSerialBleApi {
    val requestCounterStateFlow = MutableStateFlow(0)
    var resetCalls: Int = 0
        private set

    private val responseQueue = ArrayDeque<ResponsePlan>()
    private var activeWriteChannel: ByteChannel? = null

    fun enqueueResponse(plan: ResponsePlan) {
        responseQueue.addLast(plan)
    }

    override fun getReceiveByteChannel(): ByteReadChannel {
        val channel = ByteChannel(autoFlush = true).also { writableChannel ->
            activeWriteChannel = writableChannel
        }
        return when (checkNotNull(responseQueue.firstOrNull()) { "No queued response plan" }) {
            ResponsePlan.ParserFailure -> ParserFailingReadChannel(channel)
            is ResponsePlan.RawHttpResponse -> channel
        }
    }

    override suspend fun send(data: ByteArray) {
        val channel = checkNotNull(activeWriteChannel) { "No active channel for outgoing send" }
        when (val responsePlan = checkNotNull(responseQueue.removeFirstOrNull()) { "No queued response plan" }) {
            ResponsePlan.ParserFailure -> channel.close()
            is ResponsePlan.RawHttpResponse -> {
                channel.writeFully(responsePlan.payload)
                channel.close()
            }
        }
    }

    override fun getRequestCounterStateFlow(): StateFlow<Int> {
        return requestCounterStateFlow
    }

    override suspend fun reset() {
        resetCalls += 1
        requestCounterStateFlow.value = 0
    }
}

private fun validHttpResponse(body: String): ByteArray {
    val bodyBytes = body.encodeToByteArray()
    val header = """
        HTTP/1.1 200 OK
        Content-Length: ${bodyBytes.size}
        
        
    """.trimIndent()
    return header.encodeToByteArray() + bodyBytes
}

private class ParserFailingReadChannel(
    private val delegate: ByteReadChannel
) : ByteReadChannel by delegate {
    override suspend fun awaitContent(min: Int): Boolean {
        throw ParserException("Malformed response")
    }
}
