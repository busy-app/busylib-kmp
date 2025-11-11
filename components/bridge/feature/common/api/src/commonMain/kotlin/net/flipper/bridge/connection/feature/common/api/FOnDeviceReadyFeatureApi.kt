package net.flipper.bridge.connection.feature.common.api

import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi

/**
 * A special kind of FDeviceFeatureApi that is called immediately when the device is connected and is ready to be used
 */
interface FOnDeviceReadyFeatureApi : FDeviceFeatureApi {
    suspend fun onReady()

    fun interface Factory {
        suspend operator fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FOnDeviceReadyFeatureApi?
    }
}
