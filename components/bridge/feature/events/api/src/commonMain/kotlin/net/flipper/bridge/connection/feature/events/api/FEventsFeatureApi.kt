package net.flipper.bridge.connection.feature.events.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.events.model.BsbUpdateEvent
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.ConsumableUpdateEvent
import net.flipper.core.busylib.ktx.common.merge

interface FEventsFeatureApi : FDeviceFeatureApi {

    fun getBsbUpdateEvents(): Flow<ConsumableUpdateEvent.Bsb>
    fun getBusyLibUpdateEvents(): Flow<ConsumableUpdateEvent.BusyLib<*>>
    fun onBusyLibEvent(event: BusyLibUpdateEvent)
}

fun FEventsFeatureApi.getBsbUpdateFlow(event: BsbUpdateEvent): Flow<ConsumableUpdateEvent.Bsb> {
    return getBsbUpdateEvents()
        .filter { consumableUpdateEvent -> consumableUpdateEvent.bsbUpdateEvent == event }
}

inline fun <reified T : BusyLibUpdateEvent>
    FEventsFeatureApi.getBusyLibUpdateFlow(): Flow<ConsumableUpdateEvent.BusyLib<T>> {
    return getBusyLibUpdateEvents()
        .filterIsInstance<ConsumableUpdateEvent.BusyLib<T>>()
}

/**
 * Receive [BsbUpdateEvent] along with [BusyLibUpdateEvent] if it's supported for this event type
 */
fun FEventsFeatureApi.getUpdateFlow(event: BsbUpdateEvent): Flow<ConsumableUpdateEvent> {
    return when (event) {
        BsbUpdateEvent.BRIGHTNESS -> getBusyLibUpdateFlow<BusyLibUpdateEvent.Brightness>()
            .merge(getBsbUpdateFlow(event))

        BsbUpdateEvent.AUDIO_VOLUME -> getBusyLibUpdateFlow<BusyLibUpdateEvent.Volume>()
            .merge(getBsbUpdateFlow(event))

        BsbUpdateEvent.DEVICE_NAME -> getBusyLibUpdateFlow<BusyLibUpdateEvent.DeviceName>()
            .merge(getBsbUpdateFlow(event))

        else -> getBsbUpdateFlow(event)
    }
}
