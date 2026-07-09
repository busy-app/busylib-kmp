package net.flipper.tools.oncall.impl.session

import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import net.flipper.bridge.connection.feature.oncall.impl.OnCallDisplayLoop
import net.flipper.bridge.connection.feature.rpc.impl.exposed.FRpcAssetsApiImpl
import net.flipper.bridge.connection.feature.rpc.impl.util.getHttpClient
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.BUSYCloudHttpEngine
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.token.ProxyTokenProvider
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.core.ktor.getPlatformEngineFactory
import kotlin.uuid.Uuid

@AssistedInject
class CloudOnCallSession(
    @Assisted private val deviceId: Uuid,
    private val tokenProviderFactory: ProxyTokenProvider.Factory,
    private val cloudEngineFactory: BUSYCloudHttpEngine.Factory
) : LogTagProvider {
    override val TAG: String = "CloudOnCallSession"

    suspend fun run() {
        info { "Starting cloud on-call session for $deviceId" }
        val platformEngine = getPlatformEngineFactory().create()
        val cloudEngine = cloudEngineFactory(
            platformEngine,
            tokenProviderFactory(deviceId)
        )
        val httpClient = getHttpClient(cloudEngine)
        try {
            val rpcAssetsApi = FRpcAssetsApiImpl(
                httpClient = httpClient,
                dispatcher = FlipperDispatchers.default
            )
            OnCallDisplayLoop(rpcAssetsApi).run()
        } finally {
            withContext(NonCancellable) {
                info { "Closing cloud on-call session for $deviceId" }
                httpClient.close()
                cloudEngine.close()
                platformEngine.close()
            }
        }
    }

    @AssistedFactory
    fun interface Factory {
        operator fun invoke(deviceId: Uuid): CloudOnCallSession
    }
}
