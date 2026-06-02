package net.flipper.bridge.connection.feature.provider.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import kotlin.reflect.KClass

interface FFeatureProvider {
    fun <T : FDeviceFeatureApi> get(clazz: KClass<T>): Flow<FFeatureStatus<T>>

    fun <T : FDeviceFeatureApi> getFiltered(
        status: FDeviceConnectStatus.Connected,
        clazz: KClass<T>
    ): Flow<FFeatureStatus<T>>

    suspend fun <T : FDeviceFeatureApi> getSync(clazz: KClass<T>): T?
}

inline fun <reified T : FDeviceFeatureApi> FFeatureProvider.get(): Flow<FFeatureStatus<T>> {
    return get(T::class)
}

inline fun <reified T : FDeviceFeatureApi> FFeatureProvider.getFiltered(
    status: FDeviceConnectStatus.Connected
): Flow<FFeatureStatus<T>> {
    return getFiltered(status, T::class)
}

inline fun <reified T : FDeviceFeatureApi> FFeatureProvider.getFilteredFeature(
    status: FDeviceConnectStatus
): Flow<Pair<T, FDeviceConnectStatus.Connected>?> {
    val flow = if (status is FDeviceConnectStatus.Connected) {
        getFiltered<T>(status)
            .map { featureStatus ->
                if (featureStatus is FFeatureStatus.Supported<*> && featureStatus.featureApi is T) {
                    featureStatus.featureApi to status
                } else {
                    null
                }
            }
    } else {
        flowOf(null)
    }
    return flow.distinctUntilChanged()
}

suspend inline fun <reified T : FDeviceFeatureApi> FFeatureProvider.getSync(): T? {
    return getSync(T::class)
}
