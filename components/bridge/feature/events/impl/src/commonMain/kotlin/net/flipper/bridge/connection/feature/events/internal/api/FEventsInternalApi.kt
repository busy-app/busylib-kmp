package net.flipper.bridge.connection.feature.events.internal.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import net.flipper.bridge.connection.feature.events.model.BsbUpdateEvent
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.ConsumableUpdateEvent

internal class FEventsInternalApi {
    private val internalBsbEventsFlow = MutableSharedFlow<BsbUpdateEvent>()
    private val internalBusyLibEventsFlow = MutableSharedFlow<BusyLibUpdateEvent>()

    fun getBsbEventsFlow(): Flow<ConsumableUpdateEvent.Bsb> {
        return internalBsbEventsFlow
            .map { event -> ConsumableUpdateEvent.Bsb(event, null) }
    }

    suspend fun onBsbEvent(event: BsbUpdateEvent) {
        internalBsbEventsFlow.emit(event)
    }

    fun getBusyLibEventsFlow(): Flow<ConsumableUpdateEvent.BusyLib<*>> {
        return internalBusyLibEventsFlow.map { busyLibUpdateEvent ->
            ConsumableUpdateEvent.BusyLib(busyLibUpdateEvent)
        }
    }

    suspend fun onBusyLibEvent(event: BusyLibUpdateEvent) {
        val subscriptionCount = internalBusyLibEventsFlow.subscriptionCount.first()
        if (subscriptionCount <= 0) return
        internalBusyLibEventsFlow.emit(event)
    }
}
