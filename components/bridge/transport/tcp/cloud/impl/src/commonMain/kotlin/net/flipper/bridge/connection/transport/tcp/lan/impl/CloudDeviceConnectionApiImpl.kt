package net.flipper.bridge.connection.transport.tcp.lan.impl

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.tcp.cloud.api.CloudDeviceConnectionApi
import net.flipper.bridge.connection.transport.tcp.cloud.api.FCloudApi
import net.flipper.bridge.connection.transport.tcp.cloud.api.FCloudDeviceConnectionConfig
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.BUSYCloudHttpEngineFactory
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.token.ProxyTokenProvider
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.token.ProxyTokenProviderFactory
import net.flipper.bridge.connection.transport.tcp.lan.impl.monitor.CloudDeviceMonitor
import net.flipper.bsb.cloud.barsws.api.CloudWebSocketBarsApi
import net.flipper.busylib.core.di.BusyLibGraph
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(BusyLibGraph::class, CloudDeviceConnectionApi::class)
class CloudDeviceConnectionApiImpl(
    private val webSocketBarsApi: CloudWebSocketBarsApi,
    private val proxyTokenProvider: ProxyTokenProviderFactory,
    private val cloudEngineFactory: BUSYCloudHttpEngineFactory
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
            cloudEngineFactory = cloudEngineFactory
        )
        return@runCatching lanApi
    }
}
