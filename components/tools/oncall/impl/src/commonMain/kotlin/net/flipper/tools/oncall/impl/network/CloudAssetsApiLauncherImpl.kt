package net.flipper.tools.oncall.impl.network

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcAssetsApi
import net.flipper.bridge.connection.feature.rpc.impl.exposed.FRpcAssetsApiImpl
import net.flipper.bridge.connection.feature.rpc.impl.util.getHttpClient
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.BUSYCloudHttpEngine
import net.flipper.bridge.connection.transport.tcp.lan.impl.engine.token.ProxyTokenProvider
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.ktor.getPlatformEngineFactory
import net.flipper.tools.oncall.impl.api.CloudAssetsApiLauncher
import kotlin.uuid.Uuid

@Inject
@ContributesBinding(BusyLibGraph::class, binding<CloudAssetsApiLauncher>())
class CloudAssetsApiLauncherImpl(
    private val tokenProviderFactory: ProxyTokenProvider.Factory,
    private val cloudEngineFactory: BUSYCloudHttpEngine.Factory
) : CloudAssetsApiLauncher, LogTagProvider {
    override val TAG: String = "CloudAssetsApiLauncher"

    private class CloudRoute(
        val platformEngine: HttpClientEngine,
        val cloudEngine: BUSYCloudHttpEngine,
        val httpClient: HttpClient
    )

    private fun openRoute(deviceId: Uuid): CloudRoute {
        val platformEngine = getPlatformEngineFactory().create()
        var cloudEngine: BUSYCloudHttpEngine? = null
        try {
            cloudEngine = cloudEngineFactory(
                platformEngine,
                tokenProviderFactory(deviceId)
            )
            return CloudRoute(platformEngine, cloudEngine, getHttpClient(cloudEngine))
        } catch (t: Throwable) {
            // Whatever was created before the failure would never be closed by anyone else
            cloudEngine?.close()
            platformEngine.close()
            throw t
        }
    }

    private suspend fun closeRoute(deviceId: Uuid, route: CloudRoute) {
        withContext(NonCancellable) {
            info { "Closing cloud on-call session for $deviceId" }
            runSuspendCatching {
                route.httpClient.close()
                route.cloudEngine.close()
                route.platformEngine.close()
            }.onFailure { t -> error(t) { "Failed to close cloud on-call session for $deviceId" } }
        }
    }

    override suspend fun withAssetsApi(
        deviceId: Uuid,
        block: suspend (FRpcAssetsApi) -> Unit
    ): Result<Unit> {
        info { "Starting cloud on-call session for $deviceId" }
        val route = runSuspendCatching { openRoute(deviceId) }
            .getOrElse { t -> return Result.failure(t) }
        return try {
            runSuspendCatching {
                block(
                    FRpcAssetsApiImpl(
                        httpClient = route.httpClient,
                        dispatcher = FlipperDispatchers.default
                    )
                )
            }
        } finally {
            closeRoute(deviceId, route)
        }
    }
}
