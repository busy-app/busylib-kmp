package net.flipper.bridge.connection.transport.tcp.lan.impl

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.tcp.cloud.api.CloudDeviceConnectionApi
import net.flipper.bridge.connection.transport.tcp.cloud.api.FCloudApi
import net.flipper.bridge.connection.transport.tcp.cloud.api.FCloudDeviceConnectionConfig
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.BUSYCloudHttpEngine
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.token.ProxyTokenProvider
import net.flipper.bridge.connection.transport.tcp.lan.impl.metainfo.FCloudStreamingApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.runSuspendCatching

@Inject
@ContributesBinding(BusyLibGraph::class, binding<CloudDeviceConnectionApi>())
class CloudDeviceConnectionApiImpl(
    private val proxyTokenProvider: ProxyTokenProvider.Factory,
    private val cloudEngineFactory: BUSYCloudHttpEngine.Factory,
    private val cloudStreamingFactory: FCloudStreamingApi.Factory
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
