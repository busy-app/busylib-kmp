package net.flipper.bridge.connection.feature.events.proto.api

import BSB_State.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.ConsumableUpdateEvent
import net.flipper.bridge.connection.feature.events.proto.protomapper.BSBProtobufEventMapper
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi
import net.flipper.bridge.connection.transport.common.api.serial.StatusStreamingEvent
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.TaggedLogger
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.verbose

internal class FEventsProtoApi(
    private val scope: CoroutineScope,
    streamingApi: FStatusStreamingApi?
) : LogTagProvider by TaggedLogger("FEventsProtoApi") {
    private val busyLibEventsFlow = MutableSharedFlow<BusyLibUpdateEvent>()

    private fun collectProtobufChanges(streamingApi: FStatusStreamingApi) {
        streamingApi
            .getEvents()
            .onEach { event ->
                when (event) {
                    is StatusStreamingEvent.Protobuf -> onProtobufStatesUpdate(data = event)
                }.onFailure { error(it) { "Failed to process $event" } }
            }.launchIn(scope)
    }

    private suspend fun onProtobufStatesUpdate(
        data: StatusStreamingEvent.Protobuf
    ) = runSuspendCatching {
        val state = State.Companion.ADAPTER.decode(data.data)
        verbose { "Process ${state.updates.size} updates: $state" }
        val updates = state.updates.mapNotNull { update ->
            BSBProtobufEventMapper.map(update)
        }
        updates.forEach { update ->
            busyLibEventsFlow.emit(update)
        }
    }

    fun getBusyLibEventsFlow(): Flow<ConsumableUpdateEvent.BusyLib<*>> {
        return busyLibEventsFlow.map { busyLibUpdateEvent ->
            ConsumableUpdateEvent.BusyLib(busyLibUpdateEvent)
        }
    }

    init {
        if (streamingApi != null) {
            collectProtobufChanges(streamingApi)
        }
    }
}
