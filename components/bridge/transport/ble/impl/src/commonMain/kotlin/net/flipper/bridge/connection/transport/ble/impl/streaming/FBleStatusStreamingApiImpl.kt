package net.flipper.bridge.connection.transport.ble.impl.streaming

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi
import net.flipper.bridge.connection.transport.common.api.serial.StatusStreamingEvent
import kotlin.time.Duration.Companion.seconds

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
            started = SharingStarted.WhileSubscribed(5.seconds.inWholeMilliseconds),
            replay = 0
        )

    override fun getEvents(): Flow<StatusStreamingEvent> = eventsFlow

    private fun Flow<ByteArray>.reassembleBytes(): Flow<ByteArray> = channelFlow {
        val bufferMutex = Mutex()
        var bufferedPacket = ByteArray(0)
        var flushJob: Job? = null

        suspend fun flushBufferedPacket() {
            val packet = bufferMutex.withLock {
                if (bufferedPacket.isEmpty()) {
                    return@withLock null
                }

                bufferedPacket.also {
                    bufferedPacket = ByteArray(0)
                }
            } ?: return

            send(packet)
        }

        fun restartFlushTimer() {
            flushJob?.cancel()
            flushJob = launch {
                delay(PACKET_REASSEMBLE_TIMEOUT_MS)
                flushBufferedPacket()
            }
        }

        val collectJob = launch {
            try {
                collect { chunk ->
                    if (chunk.isEmpty()) {
                        return@collect
                    }

                    bufferMutex.withLock {
                        bufferedPacket = if (bufferedPacket.isEmpty()) {
                            chunk.copyOf()
                        } else {
                            bufferedPacket + chunk
                        }
                    }
                    restartFlushTimer()
                }
            } finally {
                flushJob?.cancel()
                flushBufferedPacket()
                channel.close()
            }
        }

        awaitClose {
            flushJob?.cancel()
            collectJob.cancel()
        }
    }
}
