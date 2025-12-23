package net.flipper.bridge.connection.feature.events.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.core.busylib.ktx.common.DelicateBusyLibApi

interface FEventsFeatureApi : FDeviceFeatureApi {
    /**
     * This flow designed to be used **only as a trigger source with cached RPC requests**
     *
     * Reason:
     * - Each collection subscribes to the underlying device updates stream
     * - Multiple collectors without `cache` may cause duplicated RPC calls
     */
    @DelicateBusyLibApi
    fun getUpdatesFlow(): Flow<List<UpdateEventData>>
}

/**
 * Use carefully, same as [FEventsFeatureApi.getUpdatesFlow]
 * @see FEventsFeatureApi.getUpdatesFlow
 */
@DelicateBusyLibApi
fun FEventsFeatureApi.getUpdateFlow(vararg events: UpdateEvent): Flow<EventsKey> {
    return getUpdatesFlow()
        .filter { updatesList ->
            events.any { event ->
                updatesList
                    .map(UpdateEventData::event)
                    .contains(event)
            }
        }
        .map { eventData -> eventData.sortedBy { data -> data.event.ordinal }.asEventsKey() }
}
