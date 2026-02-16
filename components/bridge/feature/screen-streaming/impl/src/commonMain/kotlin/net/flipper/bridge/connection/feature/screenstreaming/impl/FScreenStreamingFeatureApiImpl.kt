package net.flipper.bridge.connection.feature.screenstreaming.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.screenstreaming.api.FScreenStreamingFeatureApi
import net.flipper.bridge.connection.feature.screenstreaming.impl.delegates.MetaInfoScreenFramesProvider
import net.flipper.bridge.connection.feature.screenstreaming.impl.delegates.ScreenFramesProvider
import net.flipper.bridge.connection.feature.screenstreaming.impl.delegates.TickFlowScreenFramesProvider
import net.flipper.bridge.connection.feature.screenstreaming.model.BusyImageFormat
import net.flipper.bridge.connection.transport.common.api.meta.FTransportMetaInfoApi
import net.flipper.bridge.connection.transport.common.api.meta.TransportMetaInfoKey
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

class FScreenStreamingFeatureApiImpl(
    private val scope: CoroutineScope,
    private val rpcFeatureApi: FRpcFeatureApi,
    private val metaKeyTransport: FTransportMetaInfoApi?
) : FScreenStreamingFeatureApi, LogTagProvider {
    override val TAG: String = "FScreenStreamingFeatureApi"

    override val busyImageFormatFlow: WrappedFlow<BusyImageFormat> = getProviderFlow()
        .flatMapLatest { it.getScreens() }
        .wrap()

    private fun getProviderFlow(): Flow<ScreenFramesProvider> {
        if (metaKeyTransport == null) {
            info { "Meta key transport is null, so return default provider" }
            return flowOf(TickFlowScreenFramesProvider(scope, rpcFeatureApi))
        }
        return metaKeyTransport.get(TransportMetaInfoKey.WS_EVENT)
            .map { it.getOrNull() }
            .map { flow ->
                if (flow == null) {
                    info { "Web socket events is null, so return default provider" }
                    TickFlowScreenFramesProvider(scope, rpcFeatureApi)
                } else {
                    info { "Received web socket events flow, so return MetaInfoScreenFramesProvider" }
                    MetaInfoScreenFramesProvider(flow)
                }
            }
    }
}
