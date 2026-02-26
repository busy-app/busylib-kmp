package net.flipper.bridge.connection.feature.events.impl

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@Inject
class FEventsFeatureFactoryImpl : FDeviceFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FDeviceFeatureApi {
        return FEventsFeatureApiImpl(scope)
    }
}

@ContributesTo(BusyLibGraph::class)
interface FEventsFeatureComponent {
    @Provides
    @IntoMap
    fun provideFEventsFeatureFactory(
        fEventsFeatureFactory: FEventsFeatureFactoryImpl
    ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
        return FDeviceFeature.EVENTS to fEventsFeatureFactory
    }
}
