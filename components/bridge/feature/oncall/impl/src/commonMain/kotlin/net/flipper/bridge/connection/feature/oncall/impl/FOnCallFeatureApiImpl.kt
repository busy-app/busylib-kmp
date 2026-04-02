package net.flipper.bridge.connection.feature.oncall.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureMapKey
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.oncall.api.FOnCallFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.DrawRequest
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.cancelPrevious
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import dev.zacsweers.metro.ContributesTo
import kotlin.time.Duration.Companion.seconds

class FOnCallFeatureApiImpl(
    private val rpcFeatureApi: FRpcFeatureApi,
    private val scope: CoroutineScope
) : FOnCallFeatureApi, LogTagProvider {

    override val TAG: String = "FOnCallFeatureApi"

    private val singleJobScope = scope.asSingleJobScope()

    override fun start() {
        singleJobScope.launch(SingleJobMode.SKIP_IF_RUNNING) {
            try {
                while (currentCoroutineContext().isActive) {
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
        }
    }

    override fun stop() {
        singleJobScope.cancelPrevious()
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
        private val UPDATE_DELAY = 10.seconds
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
        @FDeviceFeatureMapKey(FDeviceFeature.ON_CALL)
        fun provideFOnCallFeatureFactory(
            featureFactory: FOnCallFeatureFactoryImpl
        ): FDeviceFeatureApi.Factory {
            return featureFactory
        }
    }
}
