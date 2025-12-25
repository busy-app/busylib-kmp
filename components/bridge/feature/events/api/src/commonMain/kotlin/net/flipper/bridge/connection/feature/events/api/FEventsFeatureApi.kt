package net.flipper.bridge.connection.feature.events.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi

interface FEventsFeatureApi : FDeviceFeatureApi {

    fun getUpdatesFlow(): Flow<ConsumableUpdateEvent>
}

fun FEventsFeatureApi.getUpdateFlow(event: UpdateEvent): Flow<ConsumableUpdateEvent> {
    return getUpdatesFlow()
        .filter { consumableUpdateEvent -> consumableUpdateEvent.updateEvent == event }
}
