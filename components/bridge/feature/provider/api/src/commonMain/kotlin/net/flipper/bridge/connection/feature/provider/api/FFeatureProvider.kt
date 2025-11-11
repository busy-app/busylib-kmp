package net.flipper.bridge.connection.feature.provider.api

import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import kotlin.reflect.KClass

interface FFeatureProvider {
    fun <T : FDeviceFeatureApi> get(clazz: KClass<T>): Flow<FFeatureStatus<T>>

    suspend fun <T : FDeviceFeatureApi> getSync(clazz: KClass<T>): T?
}

inline fun <reified T : FDeviceFeatureApi> FFeatureProvider.get(): Flow<FFeatureStatus<T>> {
    return get(T::class)
}

suspend inline fun <reified T : FDeviceFeatureApi> FFeatureProvider.getSync(): T? {
    return getSync(T::class)
}
