package net.flipper.bridge.connection.feature.oncall.impl

import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureKey
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
import kotlin.time.Duration.Companion.seconds

@AssistedInject
class FOnCallFeatureApiImpl(
    @Assisted private val rpcFeatureApi: FRpcFeatureApi,
    @Assisted private val scope: CoroutineScope
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
            priority = DRAW_PRIORITY,
            elements = listOf(
                DrawRequest.Element(
                    id = ANIM_ID,
                    display = DrawRequest.Display.FRONT,
                    type = DrawRequest.Element.ElementType.ANIMATION,
                    stockPath = ANIM_PATH,
                    loop = true,
                    timeoutSec = DISPLAY_TIMEOUT.inWholeSeconds.toInt()
                )
            )
        )
    }

    companion object {
        private const val APP_ID = "busy_lib_on_call"
        private const val DRAW_PRIORITY = 50
        private const val ANIM_ID = "busy_lib_on_call_anim"
        private const val ANIM_PATH = "shared/on_call_72x16.anim"
        private val DISPLAY_TIMEOUT = 30.seconds
        private val UPDATE_DELAY = 10.seconds
    }

    @Inject
    @ContributesIntoMap(BusyLibGraph::class, binding<FDeviceFeatureApi.Factory>())
    @FDeviceFeatureKey(FDeviceFeature.ON_CALL)
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
}
