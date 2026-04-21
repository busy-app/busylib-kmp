package net.flipper.bridge.connection.transport.ble.impl.streaming

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi
import net.flipper.bridge.connection.transport.common.api.serial.StatusStreamingEvent

private const val HEADER_SIZE = 6

class FBleStatusStreamingApiImpl(
    subscribeFlow: Flow<ByteArray>,
    scope: CoroutineScope
) : FStatusStreamingApi {

    private val eventsFlow: Flow<StatusStreamingEvent> = subscribeFlow
        .reassembleByHeader()
        .map { StatusStreamingEvent.Protobuf(it) }
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(),
            replay = 0
        )

    override fun getEvents(): Flow<StatusStreamingEvent> = eventsFlow

    private fun Flow<ByteArray>.reassembleByHeader(): Flow<ByteArray> = flow {
        val chunks = mutableListOf<ByteArray>()

        collect { chunk ->
            if (chunk.size < HEADER_SIZE) return@collect

            val num = chunk.getUShortLE(0)
            val count = chunk.getUShortLE(2)
            val size = chunk.getUShortLE(4)

            if (count == 0 || size > chunk.size - HEADER_SIZE) return@collect

            if (num == 0 && chunks.isNotEmpty()) chunks.clear()

            chunks.add(chunk.copyOfRange(HEADER_SIZE, HEADER_SIZE + size))

            if (num == count - 1) {
                emit(chunks.fold(ByteArray(0)) { acc, b -> acc + b })
                chunks.clear()
            }
        }
    }
}

private fun ByteArray.getUShortLE(offset: Int): Int =
    (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)
