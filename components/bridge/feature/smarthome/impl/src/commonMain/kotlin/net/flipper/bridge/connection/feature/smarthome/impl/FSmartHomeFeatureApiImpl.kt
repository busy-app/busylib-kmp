package net.flipper.bridge.connection.feature.smarthome.impl

import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.isActive
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureKey
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.get
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.ConsumableUpdateEvent
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcMatterApi
import net.flipper.bridge.connection.feature.smarthome.api.FSmartHomeFeatureApi
import net.flipper.bridge.connection.feature.smarthome.mapper.toBsbMatterCommissionedFabrics
import net.flipper.bridge.connection.feature.smarthome.mapper.toBsbMatterCommissioningPayload
import net.flipper.bridge.connection.feature.smarthome.model.BsbMatterCommissionedFabrics
import net.flipper.bridge.connection.feature.smarthome.model.BsbMatterCommissioningPayload
import net.flipper.bridge.connection.feature.smarthome.model.BsbMatterCommissioningTimeLeftPayload
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.asFlow
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.merge
import net.flipper.core.busylib.ktx.common.orEmpty
import net.flipper.core.busylib.ktx.common.throttleLatestCached
import net.flipper.core.busylib.ktx.common.transformWhileSubscribed
import net.flipper.core.busylib.ktx.common.tryConsume
import net.flipper.core.busylib.log.LogTagProvider
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class FSmartHomeFeatureApiImpl(
    private val scope: CoroutineScope,
    private val fRpcMatterApi1: FRpcMatterApi,
    private val fEventsFeatureApi: FEventsFeatureApi?,
) : FSmartHomeFeatureApi,
    LogTagProvider {
    override val TAG = "FSmartHomeFeatureApi"

    private val commissionedFabricsSharedFlow = fEventsFeatureApi
        ?.get<BusyLibUpdateEvent.Matter>()
        .orEmpty()
        .merge(flowOf(ConsumableUpdateEvent.Empty))
        .transformWhileSubscribed(scope = scope) { flow ->
            flow.throttleLatestCached { consumable, matter: BsbMatterCommissionedFabrics? ->
                val couldConsume = consumable.tryConsume()
                when (consumable) {
                    is ConsumableUpdateEvent.BusyLib<BusyLibUpdateEvent.Matter> if matter != null -> {
                        matter.copy(fabricCount = consumable.busyLibUpdateEvent.fabricCount)
                    }

                    else -> {
                        exponentialRetry {
                            fRpcMatterApi1.getMatterCommissioning(couldConsume)
                                .map { matterCommissionedFabrics ->
                                    matterCommissionedFabrics
                                        .toBsbMatterCommissionedFabrics()
                                }
                        }
                    }
                }
            }
        }
        .asFlow()
        .wrap()

    override fun getCommissionedFabricsFlow(): WrappedFlow<BsbMatterCommissionedFabrics> {
        return commissionedFabricsSharedFlow
    }

    override suspend fun getPairCode(): CResult<BsbMatterCommissioningPayload> {
        return fRpcMatterApi1.postMatterCommissioning()
            .map { matterCommissioningPayload -> matterCommissioningPayload.toBsbMatterCommissioningPayload() }
            .toCResult()
    }

    override fun getPairCodeWithTimeLeft(): WrappedFlow<BsbMatterCommissioningTimeLeftPayload?> {
        return flow {
            while (currentCoroutineContext().isActive) {
                emit(null)
                val pairCode = exponentialRetry { getPairCode().toKotlinResult() }
                do {
                    val now = Clock.System.now()
                    val timeLeft = pairCode.availableUntil.minus(now)
                        .takeIf { duration -> duration > 0.seconds }
                        ?: 0.seconds
                    val timeLeftData = BsbMatterCommissioningTimeLeftPayload(
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

    @Inject
    @ContributesIntoMap(BusyLibGraph::class, binding = binding<FDeviceFeatureApi.Factory>())
    @FDeviceFeatureKey(FDeviceFeature.SMART_HOME)
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
}
