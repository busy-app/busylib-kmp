package net.flipper.bridge.connection.feature.screenstreaming.impl.delegates

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.screenstreaming.model.BusyImageFormat
import net.flipper.core.busylib.ktx.common.exponentialRetry

class WebSocketScreenFramesProvider(
    private val rpcFeatureApi: FRpcFeatureApi
) : ScreenFramesProvider {
    override suspend fun getScreens(): Flow<BusyImageFormat> {
        return exponentialRetry {
            rpcFeatureApi.fRpcWebSocketApi
                .getScreenFrames()
        }.map(::BusyImageFormat)
    }
}