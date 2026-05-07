package net.flipper.bridge.connection.feature.firmwareupdate.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.feature.common.api.FOnDeviceReadyFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.get
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import kotlin.time.Duration.Companion.seconds

private val DELAY = 3.seconds

class FCheckUpdateFeatureOnReady(
    private val rpcFeatureApi: FRpcFeatureApi
) : FOnDeviceReadyFeatureApi, LogTagProvider {
    override val TAG = "FCheckUpdateFeatureOnReady"

    override suspend fun onReady() {
        info { "Start waiting for check update: ${DELAY.inWholeSeconds} seconds" }
        delay(DELAY)
        rpcFeatureApi
            .fRpcUpdaterApi
            .startUpdateCheck()
            .onFailure { throwable ->
                error(throwable) {
                    "#startUpdateCheck could not start update check"
                }
            }
            .map { }
    }


    @Inject
    @ContributesBinding(
        BusyLibGraph::class,
        FOnDeviceReadyFeatureApi.Factory::class,
        multibinding = true
    )
    interface Factory : FOnDeviceReadyFeatureApi.Factory {
        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FOnDeviceReadyFeatureApi? {
            val rpcApi = unsafeFeatureDeviceApi.get<FRpcFeatureApi>()?.await() ?: return null
            return FCheckUpdateFeatureOnReady(rpcApi)
        }
    }
}