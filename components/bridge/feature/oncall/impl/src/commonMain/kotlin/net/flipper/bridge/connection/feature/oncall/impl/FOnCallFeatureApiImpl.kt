package net.flipper.bridge.connection.feature.oncall.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.log.LogTagProvider
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Inject
class FOnCallFeatureApiImpl(
    @Assisted private val rpcFeatureApi: FRpcFeatureApi,
    @Assisted private val scope: CoroutineScope
) : FOnCallFeatureApi, LogTagProvider {

    override val TAG: String = "FOnCallFeatureApi"

    private val singleJobScope = scope.asSingleJobScope()
    private var startJob: Job? = null

    override suspend fun start() {
        startJob = singleJobScope.launch(SingleJobMode.SKIP_IF_RUNNING) {
            try {
                exponentialRetry(
                    initialDelay = INITIAL_RETRY_DELAY,
                    maxDelay = MAX_RETRY_DELAY
                ) {
                    performStartAttempt()
                }
                awaitCancellation()
            } finally {
                withContext(NonCancellable) {
                    withTimeout(3.seconds) {
                        performStopAttempt()
                    }
                }
            }
        }
    }

    override suspend fun stop() {
        val job = startJob
        if (job != null) {
            job.cancelAndJoin()
        } else {
            performStopAttempt()
        }
    }

    private suspend fun performStartAttempt(): Result<Unit> = runCatching {
        rpcFeatureApi
            .uploadAsset(
                appId = OnCallImage.APP_ID,
                file = OnCallImage.IMAGE_NAME,
                content = OnCallImage.content
            ).getOrThrow()

        rpcFeatureApi.displayDraw(createDrawRequest()).getOrThrow()
    }

    private suspend fun performStopAttempt(): Result<Unit> {
        return rpcFeatureApi.removeDraw(appId = OnCallImage.APP_ID).map { }
    }

    private fun createDrawRequest(): DrawRequest {
        return DrawRequest(
            appId = OnCallImage.APP_ID,
            elements = listOf(
                DrawRequest.Element(
                    id = "0",
                    timeout = 24 * 60 * 60,
                    type = DrawRequest.Element.ElementType.IMAGE,
                    x = 0,
                    y = 0,
                    path = OnCallImage.IMAGE_NAME,
                    display = DrawRequest.Display.FRONT
                )
            )
        )
    }

    companion object {
        private val INITIAL_RETRY_DELAY = 1.seconds
        private val MAX_RETRY_DELAY = Duration.INFINITE
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
