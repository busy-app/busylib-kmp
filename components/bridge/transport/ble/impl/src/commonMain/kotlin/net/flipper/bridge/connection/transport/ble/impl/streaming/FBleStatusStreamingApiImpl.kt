package net.flipper.bridge.connection.transport.ble.impl.streaming

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi
import net.flipper.bridge.connection.transport.common.api.serial.StatusStreamingEvent

// Packet boundaries are inferred by inactivity, because streaming payload has no length-prefix.
// 250ms was chosen as a practical delimiter:
// - a single frame is expected to be delivered in ~150ms;
// - next frame is expected noticeably later due to sender rate limiting.
private const val PACKET_REASSEMBLE_TIMEOUT_MS = 250L

class FBleStatusStreamingApiImpl(
    subscribeFlow: Flow<ByteArray>,
    scope: CoroutineScope
) : FStatusStreamingApi {

    private val eventsFlow: Flow<StatusStreamingEvent> = subscribeFlow
        .reassembleBytes()
        .map { StatusStreamingEvent.Protobuf(it) }
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(),
            replay = 0
        )

    override fun getEvents(): Flow<StatusStreamingEvent> = eventsFlow

    private fun Flow<ByteArray>.reassembleBytes(): Flow<ByteArray> = channelFlow {
        val packetBuffer = PacketBuffer()
        val inputChunks = this@reassembleBytes
            .map(ByteArray::copyOf)
            .produceIn(this)

        suspend fun flushBufferedPacket() {
            val packet = packetBuffer.drain() ?: return
            send(packet)
        }

        try {
            while (true) {
                select {
                    inputChunks.onReceive { chunk ->
                        packetBuffer.append(chunk)
                    }

                    if (packetBuffer.hasData()) {
                        onTimeout(PACKET_REASSEMBLE_TIMEOUT_MS) {
                            flushBufferedPacket()
                        }
                    }
                }
            }
        } catch (_: ClosedReceiveChannelException) {
            flushBufferedPacket()
        } finally {
            inputChunks.cancel()
        }
    }
}
