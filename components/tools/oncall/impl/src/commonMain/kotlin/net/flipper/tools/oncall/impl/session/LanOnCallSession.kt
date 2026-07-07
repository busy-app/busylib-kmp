package net.flipper.tools.oncall.impl.session

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import net.flipper.bridge.connection.feature.oncall.impl.OnCallDisplayLoop
import net.flipper.bridge.connection.feature.rpc.impl.exposed.FRpcAssetsApiImpl
import net.flipper.bridge.connection.feature.rpc.impl.util.getHttpClient
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.BUSYBarHttpEngine
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.core.ktor.getPlatformEngineFactory

internal class LanOnCallSession(
    private val host: String
) : LogTagProvider {
    override val TAG: String = "LanOnCallSession"

    suspend fun run() {
        info { "Starting LAN on-call session at $host" }
        val platformEngine = getPlatformEngineFactory().create()
        val lanEngine = BUSYBarHttpEngine(platformEngine, host)
        val httpClient = getHttpClient(lanEngine)
        try {
            val rpcAssetsApi = FRpcAssetsApiImpl(
                httpClient = httpClient,
                dispatcher = FlipperDispatchers.default
            )
            OnCallDisplayLoop(rpcAssetsApi).run()
        } finally {
            withContext(NonCancellable) {
                info { "Closing LAN on-call session at $host" }
                httpClient.close()
                lanEngine.close()
                platformEngine.close()
            }
        }
    }
}
