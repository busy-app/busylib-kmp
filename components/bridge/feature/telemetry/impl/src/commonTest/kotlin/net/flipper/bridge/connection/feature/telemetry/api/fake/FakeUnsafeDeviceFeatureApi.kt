package net.flipper.bridge.connection.feature.telemetry.api.fake

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import kotlin.reflect.KClass

internal class FakeUnsafeDeviceFeatureApi(
    private val rpcFeatureApi: FRpcFeatureApi?
) : FUnsafeDeviceFeatureApi {
    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : FDeviceFeatureApi> get(clazz: KClass<T>): Deferred<T?>? {
        if (clazz != FRpcFeatureApi::class) return null
        return CompletableDeferred(rpcFeatureApi as T?)
    }
}
