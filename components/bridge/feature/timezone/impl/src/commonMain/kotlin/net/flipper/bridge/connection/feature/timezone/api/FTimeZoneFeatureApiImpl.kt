package net.flipper.bridge.connection.feature.timezone.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureKey
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.get
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.ConsumableUpdateEvent
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.timezone.api.model.TimezoneInfo
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
import net.flipper.core.busylib.ktx.common.throttleLatest
import net.flipper.core.busylib.ktx.common.transformWhileSubscribed
import net.flipper.core.busylib.ktx.common.tryConsume
import net.flipper.core.busylib.log.LogTagProvider
import dev.zacsweers.metro.ContributesTo

class FTimeZoneFeatureApiImpl(
    private val scope: CoroutineScope,
    private val rpcFeatureApi: FRpcFeatureApi,
    private val fEventsFeatureApi: FEventsFeatureApi?,
) : FTimeZoneFeatureApi, LogTagProvider {
    override val TAG: String = "FTimeZoneFeatureApi"

    private val timeZoneInfoSharedFlow = fEventsFeatureApi
        ?.get<BusyLibUpdateEvent.Timezone>()
        .orEmpty()
        .merge(flowOf(ConsumableUpdateEvent.Empty))
        .transformWhileSubscribed(scope = scope) { flow ->
            flow.throttleLatest { consumable ->
                val couldConsume = consumable.tryConsume()
                when (consumable) {
                    ConsumableUpdateEvent.Empty,
                    is ConsumableUpdateEvent.BusyLib<BusyLibUpdateEvent.Timezone> -> {
                        @Suppress("ForbiddenComment")
                        // TODO: https://flipper.atlassian.net/browse/FW-781
                        exponentialRetry {
                            rpcFeatureApi.fRpcTimeZoneApi.getTimeTimezone(couldConsume)
                        }
                    }
                }
            }
        }
        .asFlow()
        .map { rpcTimezoneInfo -> rpcTimezoneInfo.toPublic() }
        .wrap()

    override fun getTimeZoneInfoFlow(): WrappedFlow<TimezoneInfo> {
        return timeZoneInfoSharedFlow
    }

    override suspend fun setTimezone(timezoneInfo: TimezoneInfo): CResult<Unit> {
        return rpcFeatureApi.fRpcTimeZoneApi
            .postTimeTimezone(timezoneInfo.toInternal())
            .toCResult()
    }

    override suspend fun getTimezones(): CResult<List<TimezoneInfo>> {
        return rpcFeatureApi.fRpcTimeZoneApi.getTimeTzList().map { it.toPublic() }.toCResult()
    }

    @Inject
    class Factory : FDeviceFeatureApi.Factory {
        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FDeviceFeatureApi? {
            val fRpcFeatureApi = unsafeFeatureDeviceApi
                .get(FRpcFeatureApi::class)
                ?.await()
                ?: return null
            val fEventsFeatureApi = unsafeFeatureDeviceApi
                .get(FEventsFeatureApi::class)
                ?.await()

            return FTimeZoneFeatureApiImpl(
                rpcFeatureApi = fRpcFeatureApi,
                fEventsFeatureApi = fEventsFeatureApi,
                scope = scope
            )
        }
    }

    @ContributesTo(BusyLibGraph::class)
    interface Component {
        @Provides
        @IntoMap
        @FDeviceFeatureKey(FDeviceFeature.TIME_ZONE)
        fun provideFeatureFactory(
            factory: Factory
        ): FDeviceFeatureApi.Factory {
            return factory
        }
    }
}
