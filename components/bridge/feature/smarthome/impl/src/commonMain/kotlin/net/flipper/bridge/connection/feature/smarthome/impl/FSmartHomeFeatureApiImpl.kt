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
import net.flipper.bridge.connection.feature.smarthome.api.FSmartHomeFeatureApi
import net.flipper.bridge.connection.feature.smarthome.model.SmartHomePairCodeData
import net.flipper.bridge.connection.feature.smarthome.model.SmartHomePairCodeTimeLeftData
import net.flipper.bridge.connection.feature.smarthome.model.SmartHomeState
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.WrappedStateFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.log.LogTagProvider
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class FSmartHomeFeatureApiImpl : FSmartHomeFeatureApi, LogTagProvider {
    override val TAG = "FSmartHomeFeatureApi"

    override fun getState(): WrappedStateFlow<SmartHomeState> {
        return MutableStateFlow(SmartHomeState.Disconnected).wrap()
    }

    override suspend fun getPairCode(): CResult<SmartHomePairCodeData> {
        delay(1000L)
        return CResult.success(
            SmartHomePairCodeData(
                code = "1234-567-7899",
                url = "http://10.0.4.20/api/url",
                expiresAfter = Clock.System.now().plus(5.minutes)
            )
        )
    }

    override fun getPairCodeWithTimeLeft(): WrappedFlow<SmartHomePairCodeTimeLeftData?> {
        return flow {
            while (currentCoroutineContext().isActive) {
                emit(null)
                val pairCode = exponentialRetry { getPairCode().toKotlinResult() }
                do {
                    val now = Clock.System.now()
                    val timeLeft = pairCode.expiresAfter.minus(now)
                        .takeIf { duration -> duration > 0.seconds }
                        ?: 0.seconds
                    val timeLeftData = SmartHomePairCodeTimeLeftData(
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
        delay(1000L)
        return CResult.success(Unit)
    }

    @ContributesTo(BusyLibGraph::class)
    interface Component {
        @Inject
        class Factory : FDeviceFeatureApi.Factory {
            override suspend fun invoke(
                unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
                scope: CoroutineScope,
                connectedDevice: FConnectedDeviceApi
            ): FDeviceFeatureApi {
                return FSmartHomeFeatureApiImpl()
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
