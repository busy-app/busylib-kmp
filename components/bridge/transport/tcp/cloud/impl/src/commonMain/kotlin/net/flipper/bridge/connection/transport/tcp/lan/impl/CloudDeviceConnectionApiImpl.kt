package net.flipper.bridge.connection.transport.tcp.lan.impl

import kotlinx.coroutines.CoroutineScope
import dev.zacsweers.metro.binding
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.tcp.cloud.api.CloudDeviceConnectionApi
import net.flipper.bridge.connection.transport.tcp.cloud.api.FCloudApi
import net.flipper.bridge.connection.transport.tcp.cloud.api.FCloudDeviceConnectionConfig
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.BUSYCloudHttpEngine
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.token.ProxyTokenProvider
import net.flipper.bridge.connection.transport.tcp.lan.impl.metainfo.FCloudStreamingApi
import net.flipper.bridge.connection.transport.tcp.lan.impl.monitor.CloudDeviceMonitor
import net.flipper.bsb.cloud.barsws.api.CloudWebSocketBarsApi
import net.flipper.busylib.core.di.BusyLibGraph

@Inject
@ContributesBinding(BusyLibGraph::class, binding = binding<CloudDeviceConnectionApi>())
class CloudDeviceConnectionApiImpl(
    private val webSocketBarsApi: CloudWebSocketBarsApi,
    private val proxyTokenProvider: ProxyTokenProvider.Factory,
    private val cloudEngineFactory: BUSYCloudHttpEngine.Factory,
    private val cloudStreamingFactory: FCloudStreamingApi.Factory
) : CloudDeviceConnectionApi {
    override suspend fun connect(
        scope: CoroutineScope,
        config: FCloudDeviceConnectionConfig,
        listener: FTransportConnectionStatusListener
    ): Result<FCloudApi> = runCatching {
        val cloudDeviceMonitorFactory = CloudDeviceMonitor.Factory(
            webSocketBarsApi = webSocketBarsApi,
            scope = scope
        )
        val lanApi = FCloudApiImpl(
            listener = listener,
            currentConfig = config,
            cloudDeviceMonitorFactory = cloudDeviceMonitorFactory,
            tokenProviderFactory = proxyTokenProvider,
            cloudEngineFactory = cloudEngineFactory,
            cloudStreamingFactory = cloudStreamingFactory,
            scope = scope
        )
        return@runCatching lanApi
    }
}
