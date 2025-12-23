package net.flipper.bridge.connection.feature.events.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi

interface FEventsFeatureApi : FDeviceFeatureApi {

    fun getUpdatesFlow(): Flow<List<UpdateEvent>>
}

fun FEventsFeatureApi.getUpdateFlow(vararg events: UpdateEvent): Flow<Unit> {
    return getUpdatesFlow()
        .filter { updatesList -> events.any { event -> updatesList.contains(event) } }
        .map { }
}
