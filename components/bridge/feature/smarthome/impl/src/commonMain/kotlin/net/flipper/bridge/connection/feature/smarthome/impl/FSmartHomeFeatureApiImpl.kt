package net.flipper.bridge.connection.feature.smarthome.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcMatterApi
import net.flipper.bridge.connection.feature.rpc.api.model.MatterCommissioningPayload
import net.flipper.bridge.connection.feature.smarthome.api.FSmartHomeFeatureApi
import net.flipper.bridge.connection.feature.smarthome.model.MatterCommissioningTimeLeftPayload
import net.flipper.bridge.connection.feature.smarthome.model.SmartHomeState
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.log.LogTagProvider
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class FSmartHomeFeatureApiImpl(private val fRpcMatterApi1: FRpcMatterApi) :
    FSmartHomeFeatureApi,
    LogTagProvider {
    override val TAG = "FSmartHomeFeatureApi"

    override fun getState(): WrappedStateFlow<SmartHomeState> {
        return MutableStateFlow(SmartHomeState.Disconnected).wrap()
    }

    override suspend fun getPairCode(): CResult<MatterCommissioningPayload> {
        return fRpcMatterApi1.postMatterCommissioning().toCResult()
    }

    override fun getPairCodeWithTimeLeft(): WrappedFlow<MatterCommissioningTimeLeftPayload?> {
        return flow {
            while (currentCoroutineContext().isActive) {
                emit(null)
                val pairCode = exponentialRetry { getPairCode().toKotlinResult() }
                do {
                    val now = Clock.System.now()
                    val timeLeft = pairCode.availableUntil.minus(now)
                        .takeIf { duration -> duration > 0.seconds }
                        ?: 0.seconds
                    val timeLeftData = MatterCommissioningTimeLeftPayload(
                        instance = pairCode,
                        timeLeft = timeLeft
                    )
                    emit(timeLeftData)
                    delay(1.seconds)
                } while (timeLeft > 0.seconds)
            }
        }.wrap()
    }

    override suspend fun forgetAllPairings(): CResult<Unit> {
        return fRpcMatterApi1.deleteMatterCommissioning().toCResult()
    }

    @ContributesTo(BusyLibGraph::class)
    interface Component {
        @Inject
        class Factory : FDeviceFeatureApi.Factory {
            override suspend fun invoke(
                unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
                scope: CoroutineScope,
                connectedDevice: FConnectedDeviceApi
            ): FDeviceFeatureApi? {
                val fRpcMatterApi = unsafeFeatureDeviceApi
                    .get(FRpcFeatureApi::class)
                    ?.await()
                    ?.fRpcMatterApi
                    ?: return null
                return FSmartHomeFeatureApiImpl(fRpcMatterApi)
            }
        }

        @Provides
        @IntoMap
        fun provideSmartHomeFeatureFactory(
            featureFactory: Factory
        ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
            return FDeviceFeature.SMART_HOME to featureFactory
        }
    }
}
