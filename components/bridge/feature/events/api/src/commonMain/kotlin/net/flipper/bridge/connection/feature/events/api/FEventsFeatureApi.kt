package net.flipper.bridge.connection.feature.events.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.core.busylib.ktx.common.DelicateBusyLibApi

interface FEventsFeatureApi : FDeviceFeatureApi {
    /**
     * This flow designed to be used **only as a trigger source inside `shareIn` with RPC requests**
     *
     * Reason:
     * - Each collection subscribes to the underlying device updates stream
     * - Multiple collectors without `shareIn` may cause duplicated RPC calls
     *
     * Use this **correct pattern**
     *
     * ```kotlin
     *     getUpdatesFlow()
     *         .merge(flowOf(Unit))
     *         .filter { ... }
     *         .map { someRpcCall() }
     *         .shareIn(scope, SharingStarted.WhileSubscribed(5.seconds), replay = 1)
     * ```
     */
    @DelicateBusyLibApi
    fun getUpdatesFlow(): Flow<List<UpdateEvent>>
}

/**
 * Use carefully, same as [FEventsFeatureApi.getUpdatesFlow]
 * @see FEventsFeatureApi.getUpdatesFlow
 */
@DelicateBusyLibApi
fun FEventsFeatureApi.getUpdateFlow(vararg events: UpdateEvent): Flow<Unit> {
    return getUpdatesFlow()
        .filter { updatesList -> events.any { updatesList.contains(it) } }
        .map { }
}
