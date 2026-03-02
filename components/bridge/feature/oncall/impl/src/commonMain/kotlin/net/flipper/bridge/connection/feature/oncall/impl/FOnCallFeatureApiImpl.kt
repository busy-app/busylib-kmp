package net.flipper.bridge.connection.feature.oncall.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.oncall.api.FOnCallFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.DrawRequest
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.DelicateSingleJobApi
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.time.Duration.Companion.seconds

@Inject
class FOnCallFeatureApiImpl(
    @Assisted private val rpcFeatureApi: FRpcFeatureApi,
    @Assisted private val scope: CoroutineScope
) : FOnCallFeatureApi, LogTagProvider {

    override val TAG: String = "FOnCallFeatureApi"

    private val singleJobScope = scope.asSingleJobScope()
    private var startJob: Job? = null // todo replace with singleJobScope job

    @OptIn(DelicateSingleJobApi::class)
    override suspend fun start() {
        startJob = runCatching {
            singleJobScope.withJobMode(SingleJobMode.SKIP_IF_RUNNING) {
                try {
                    while (true) {
                        rpcFeatureApi
                            .fRpcAssetsApi
                            .displayDraw(createDrawRequest())
                            .onFailure { error(it) { "Failed to display draw" } }
                        delay(UPDATE_DELAY)
                    }
                } finally {
                    withContext(NonCancellable) {
                        withTimeoutOrNull(3.seconds) {
                            performStopAttempt()
                        }
                    }
                }
            }.job
        }.getOrNull()
    }

    override suspend fun stop() {
        val job = startJob
        job?.cancelAndJoin()
    }

    private suspend fun performStopAttempt(): Result<Unit> {
        return rpcFeatureApi.fRpcAssetsApi.removeDraw(appId = APP_ID).map { }
    }

    private fun createDrawRequest(): DrawRequest {
        return DrawRequest(
            appId = APP_ID,
            elements = listOf(
                DrawRequest.Element(
                    id = "0",
                    timeout = ELEMENT_TIMEOUT,
                    priority = ELEMENT_PRIORITY,
                    display = DrawRequest.Display.FRONT,
                    type = DrawRequest.Element.ElementType.ANIM,
                    builtinAnim = BUILTIN_ANIM,
                    section = "loop",
                    loop = true
                )
            )
        )
    }

    companion object {
        private const val APP_ID = "on_call"
        private const val BUILTIN_ANIM = "shared/on_call_72x16"
        private const val ELEMENT_TIMEOUT = 30
        private const val ELEMENT_PRIORITY = 50
        private val UPDATE_DELAY = 3.seconds
    }

    @Inject
    class FOnCallFeatureFactoryImpl : FDeviceFeatureApi.Factory {
        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FDeviceFeatureApi? {
            val fRpcFeatureApi = unsafeFeatureDeviceApi
                .get(FRpcFeatureApi::class)
                ?.await()
                ?: return null

            return FOnCallFeatureApiImpl(
                rpcFeatureApi = fRpcFeatureApi,
                scope = scope
            )
        }
    }

    @ContributesTo(BusyLibGraph::class)
    interface FOnCallFeatureComponent {
        @Provides
        @IntoMap
        fun provideFOnCallFeatureFactory(
            featureFactory: FOnCallFeatureFactoryImpl
        ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
            return FDeviceFeature.ON_CALL to featureFactory
        }
    }
}
