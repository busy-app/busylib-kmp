package net.flipper.bridge.connection.feature.events.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.events.model.BsbUpdateEvent
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.ConsumableUpdateEvent
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.merge
import net.flipper.core.busylib.ktx.common.orEmpty
import net.flipper.core.busylib.ktx.common.throttleLatest
import net.flipper.core.busylib.ktx.common.transformWhileSubscribed
import net.flipper.core.busylib.ktx.common.tryConsume

interface FEventsFeatureApi : FDeviceFeatureApi {

    fun getBusyLibUpdateEvents(): Flow<ConsumableUpdateEvent.BusyLib<*>>
    fun onBusyLibEvent(event: BusyLibUpdateEvent)

    @Deprecated("Use BusyLibUpdateEvent instead")
    fun onBsbEvent(event: BsbUpdateEvent)

    @Deprecated("Use BusyLibUpdateEvent instead")
    fun getBsbUpdateEvents(): Flow<ConsumableUpdateEvent.Bsb>
}

@Deprecated("Use BusyLibUpdateEvent instead")
fun FEventsFeatureApi.getBsbUpdateFlow(event: BsbUpdateEvent): Flow<ConsumableUpdateEvent.Bsb> {
    return getBsbUpdateEvents()
        .filter { consumableUpdateEvent -> consumableUpdateEvent.bsbUpdateEvent == event }
}

@Deprecated("Use BusyLibUpdateEvent instead")
inline fun <reified T : BusyLibUpdateEvent> FEventsFeatureApi.getBusyLibUpdateFlow():
    Flow<ConsumableUpdateEvent.BusyLib<T>> {
    return getBusyLibUpdateEvents()
        .filterIsInstance<ConsumableUpdateEvent.BusyLib<T>>()
}

/**
 * Receive [BsbUpdateEvent] along with [BusyLibUpdateEvent] if it's supported for this event type
 */
@Deprecated("Use BusyLibUpdateEvent instead")
fun FEventsFeatureApi.getUpdateFlow(event: BsbUpdateEvent): Flow<ConsumableUpdateEvent> {
    return when (event) {
        BsbUpdateEvent.BRIGHTNESS -> getBusyLibUpdateFlow<BusyLibUpdateEvent.Brightness>()
            .merge(getBsbUpdateFlow(event))

        BsbUpdateEvent.AUDIO_VOLUME -> getBusyLibUpdateFlow<BusyLibUpdateEvent.Volume>()
            .merge(getBsbUpdateFlow(event))

        BsbUpdateEvent.DEVICE_NAME -> getBusyLibUpdateFlow<BusyLibUpdateEvent.DeviceName>()
            .merge(getBsbUpdateFlow(event))

        BsbUpdateEvent.AUTO_UPDATE_CHANGED -> getBusyLibUpdateFlow<BusyLibUpdateEvent.AutoUpdateChanged>()
            .merge(getBsbUpdateFlow(event))

        else -> getBsbUpdateFlow(event)
    }
}

inline fun <reified T : BusyLibUpdateEvent> FEventsFeatureApi.get(): Flow<ConsumableUpdateEvent.BusyLib<T>> {
    return getBusyLibUpdateEvents()
        .filterIsInstance<ConsumableUpdateEvent.BusyLib<T>>()
}

fun <T : BusyLibUpdateEvent, R> FEventsFeatureApi?.get(
    scope: CoroutineScope,
    initial: suspend (couldConsume: Boolean) -> Result<T>,
    mapper: (Flow<T>) -> Flow<R>
): SharedFlow<R> {
    return this?.getBusyLibUpdateEvents()
        ?.filterIsInstance<ConsumableUpdateEvent.BusyLib<T>>()
        .orEmpty()
        .merge(flowOf(ConsumableUpdateEvent.Empty))
        .transformWhileSubscribed(scope = scope) { flow ->
            flow.throttleLatest { consumable ->
                val couldConsume = consumable.tryConsume()
                when (consumable) {
                    ConsumableUpdateEvent.Empty -> {
                        exponentialRetry {
                            initial(couldConsume)
                        }
                    }

                    is ConsumableUpdateEvent.BusyLib<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        consumable.busyLibUpdateEvent as? T
                    }

                    is ConsumableUpdateEvent.Bsb -> TODO()
                }
            }.filterNotNull()
                .run(mapper)
        }
}

fun <T : BusyLibUpdateEvent> FEventsFeatureApi?.get(
    scope: CoroutineScope,
    initial: suspend (couldConsume: Boolean) -> Result<T>
): SharedFlow<T> {
    return get(scope, initial, { it })
}
