package net.flipper.bsb.watchers.provisioning.fakes

import kotlinx.coroutines.flow.Flow
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.FFeatureStatus
import kotlin.reflect.KClass

internal class FakeFeatureProvider<F : FDeviceFeatureApi>(
    private val featureFlow: Flow<FFeatureStatus<F>>
) : FFeatureProvider {
    @Suppress("UNCHECKED_CAST")
    override fun <T : FDeviceFeatureApi> get(clazz: KClass<T>): Flow<FFeatureStatus<T>> {
        return featureFlow as Flow<FFeatureStatus<T>>
    }

    override suspend fun <T : FDeviceFeatureApi> getSync(clazz: KClass<T>): T? = null
}
