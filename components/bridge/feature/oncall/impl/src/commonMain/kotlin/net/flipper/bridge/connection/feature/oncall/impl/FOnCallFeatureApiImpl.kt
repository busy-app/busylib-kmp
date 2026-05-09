package net.flipper.bridge.connection.feature.oncall.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
import net.flipper.bridge.connection.feature.rpc.generated.model.DisplayElements
import net.flipper.bridge.connection.feature.rpc.generated.model.DisplayElementsElementsInner
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.cancelPrevious
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

    override fun start() {
        singleJobScope.launch(SingleJobMode.SKIP_IF_RUNNING) {
            try {
                while (currentCoroutineContext().isActive) {
                    rpcFeatureApi
                        .fRpcAssetsApi
                        .drawOnDisplay(createDrawRequest())
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
        return rpcFeatureApi.fRpcAssetsApi.deleteAppAssets(applicationName = APP_ID).map { }
    }

    private fun createDrawRequest(): DisplayElements {
        return DisplayElements(
            applicationName = APP_ID,
            priority = DRAW_PRIORITY,
            elements = listOf(
                DisplayElementsElementsInner(
                    id = ANIM_ID,
                    display = DisplayElementsElementsInner.Display.FRONT,
                    type = DisplayElementsElementsInner.Type.ANIMATION,
                    stockPath = ANIM_PATH,
                    loop = true
                )
            )
        )
    }

    companion object {
        private const val APP_ID = "busy_lib_on_call"
        private const val DRAW_PRIORITY = 50
        private const val ANIM_ID = "busy_lib_on_call_anim"
        private const val ANIM_PATH = "shared/on_call_72x16.anim"
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
        fun provideFOnCallFeatureFactory(
            featureFactory: FOnCallFeatureFactoryImpl
        ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
            return FDeviceFeature.ON_CALL to featureFactory
        }
    }
}
