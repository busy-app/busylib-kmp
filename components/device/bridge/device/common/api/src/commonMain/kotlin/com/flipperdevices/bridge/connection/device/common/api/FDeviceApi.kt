package com.flipperdevices.bridge.connection.device.common.api

import com.flipperdevices.bridge.connection.feature.common.api.FDeviceFeatureApi
import kotlinx.coroutines.Deferred
import kotlin.reflect.KClass

interface FDeviceApi {
    suspend fun <T : FDeviceFeatureApi> get(clazz: KClass<T>): Deferred<T?>?
}

suspend inline fun <reified T : FDeviceFeatureApi> FDeviceApi.get(): Deferred<T?>? {
    return get(T::class)
}
