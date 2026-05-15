package net.flipper.bridge.connection.transport.ble.impl.streaming

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi
import net.flipper.bridge.connection.transport.common.api.serial.StatusStreamingEvent
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error

class FBleStatusStreamingApiImpl(
    subscribeFlow: Flow<ByteArray>,
    scope: CoroutineScope
) : FStatusStreamingApi, LogTagProvider {

    override val TAG: String = "FBleStatusStreamingApi"

    private val eventsFlow: Flow<StatusStreamingEvent> = subscribeFlow
        .reassembleByHeader()
        .map { StatusStreamingEvent.Protobuf(it) }
        .shareIn(
            scope = scope,
            started = SharingStarted.Eagerly, // To drop events even if no subscriber
            replay = 0
        )

    override fun getEvents(): Flow<StatusStreamingEvent> = eventsFlow

    private fun Flow<ByteArray>.reassembleByHeader(): Flow<ByteArray> = flow {
        val parser = BleStatusStreamingPacketParser()

        collect { chunk ->
            runSuspendCatching {
                parser.consume(chunk)?.let { emit(it) }
            }.onFailure { error ->
                error(error = error) { "Failed to parse packet " }
            }
        }
    }
}
