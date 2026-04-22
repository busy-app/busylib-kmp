package net.flipper.bsb.watchers.provisioning.fakes

import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import kotlin.reflect.KClass

internal class FakeFeatureProvider(
    private val rpcFlow: Flow<FFeatureStatus<FRpcFeatureApi>>
) : FFeatureProvider {
    @Suppress("UNCHECKED_CAST")
    override fun <T : FDeviceFeatureApi> get(clazz: KClass<T>): Flow<FFeatureStatus<T>> {
        return rpcFlow as Flow<FFeatureStatus<T>>
    }

    override suspend fun <T : FDeviceFeatureApi> getSync(clazz: KClass<T>): T? = null
}
