package net.flipper.bridge.connection.feature.smarthome.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.UpdateEvent
import net.flipper.bridge.connection.feature.events.api.getUpdateFlow
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcMatterApi
import net.flipper.bridge.connection.feature.rpc.api.model.MatterCommissionedFabrics
import net.flipper.bridge.connection.feature.rpc.api.model.MatterCommissioningPayload
import net.flipper.bridge.connection.feature.smarthome.api.FSmartHomeFeatureApi
import net.flipper.bridge.connection.feature.smarthome.model.MatterCommissioningTimeLeftPayload
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.DefaultConsumable
import net.flipper.core.busylib.ktx.common.asFlow
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.merge
import net.flipper.core.busylib.ktx.common.orEmpty
import net.flipper.core.busylib.ktx.common.throttleLatest
import net.flipper.core.busylib.ktx.common.transformWhileSubscribed
import net.flipper.core.busylib.ktx.common.tryConsume
import net.flipper.core.busylib.log.LogTagProvider
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class FSmartHomeFeatureApiImpl(
    private val scope: CoroutineScope,
    private val fRpcMatterApi1: FRpcMatterApi,
    private val fEventsFeatureApi: FEventsFeatureApi?,
) : FSmartHomeFeatureApi,
    LogTagProvider {
    override val TAG = "FSmartHomeFeatureApi"

    override fun getCommissionedFabricsFlow(): WrappedFlow<MatterCommissionedFabrics> {
        return fEventsFeatureApi
            ?.getUpdateFlow(UpdateEvent.SMART_HOME_STATUS_CHANGED)
            .orEmpty()
            .merge(flowOf(DefaultConsumable(false)))
            .transformWhileSubscribed(scope = scope) { flow ->
                flow.throttleLatest { consumable ->
                    val couldConsume = consumable.tryConsume()
                    exponentialRetry {
                        fRpcMatterApi1.getMatterCommissioning(couldConsume)
                    }
                }
            }
            .asFlow()
            .wrap()
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
                val fEventsFeatureApi = unsafeFeatureDeviceApi
                    .get(FEventsFeatureApi::class)
                    ?.await()
                return FSmartHomeFeatureApiImpl(
                    scope = scope,
                    fRpcMatterApi1 = fRpcMatterApi,
                    fEventsFeatureApi = fEventsFeatureApi
                )
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
