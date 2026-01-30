package net.flipper.bridge.connection.feature.firmwareupdate.impl

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

@Inject
class FFirmwareUpdateFeatureFactoryImpl : FDeviceFeatureApi.Factory {
    override suspend fun invoke(
        unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
        scope: CoroutineScope,
        connectedDevice: FConnectedDeviceApi
    ): FDeviceFeatureApi? {
        val rpcApi = unsafeFeatureDeviceApi
            .get(FRpcFeatureApi::class)
            ?.await()
            ?: return null
        val fEventsFeatureApi = unsafeFeatureDeviceApi
            .get(FEventsFeatureApi::class)
            ?.await()

        return FFirmwareUpdateFeatureApiImpl(
            rpcFeatureApi = rpcApi,
            fEventsFeatureApi = fEventsFeatureApi,
            scope = scope,
        )
    }
}

@ContributesTo(BusyLibGraph::class)
interface FFirmwareUpdateFeatureComponent {
    @Provides
    @IntoMap
    fun provideFFirmwareUpdateFeatureFactory(
        fFirmwareUpdateFeatureFactory: FFirmwareUpdateFeatureFactoryImpl
    ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
        return FDeviceFeature.FIRMWARE_UPDATE to fFirmwareUpdateFeatureFactory
    }
}
