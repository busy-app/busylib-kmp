package net.flipper.bridge.connection.transport.tcp.lan.impl

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.tcp.cloud.api.CloudDeviceConnectionApi
import net.flipper.bridge.connection.transport.tcp.cloud.api.FCloudApi
import net.flipper.bridge.connection.transport.tcp.cloud.api.FCloudDeviceConnectionConfig
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.BUSYCloudHttpEngineFactory
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.token.ProxyTokenProviderFactory
import net.flipper.bridge.connection.transport.tcp.lan.impl.metainfo.FCloudStreamingFactory
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(BusyLibGraph::class, CloudDeviceConnectionApi::class)
class CloudDeviceConnectionApiImpl(
    private val proxyTokenProvider: ProxyTokenProviderFactory,
    private val cloudEngineFactory: BUSYCloudHttpEngineFactory,
    private val cloudStreamingFactory: FCloudStreamingFactory
) : CloudDeviceConnectionApi {
    override suspend fun connect(
        scope: CoroutineScope,
        config: FCloudDeviceConnectionConfig,
        listener: FTransportConnectionStatusListener
    ): Result<FCloudApi> = runSuspendCatching {
        val lanApi = FCloudApiImpl(
            listener = listener,
            currentConfig = config,
            tokenProviderFactory = proxyTokenProvider,
            cloudEngineFactory = cloudEngineFactory,
            cloudStreamingFactory = cloudStreamingFactory,
            scope = scope
        )
        return@runSuspendCatching lanApi
    }
}
