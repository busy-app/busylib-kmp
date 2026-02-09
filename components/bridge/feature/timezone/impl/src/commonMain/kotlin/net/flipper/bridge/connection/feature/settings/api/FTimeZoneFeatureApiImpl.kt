package net.flipper.bridge.connection.feature.settings.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
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
import net.flipper.bridge.connection.feature.rpc.api.model.TimestampInfo
import net.flipper.bridge.connection.feature.rpc.api.model.TimezoneInfo
import net.flipper.bridge.connection.feature.rpc.api.model.TimezoneListResponse
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

class FTimeZoneFeatureApiImpl(
    private val scope: CoroutineScope,
    private val rpcFeatureApi: FRpcFeatureApi,
    private val fEventsFeatureApi: FEventsFeatureApi?,
) : FTimeZoneFeatureApi, LogTagProvider {
    override val TAG: String = "FSettingsFeatureApi"

    override fun getTimestampInfoFlow(): WrappedFlow<TimestampInfo> {
        return fEventsFeatureApi
            ?.getUpdateFlow(UpdateEvent.TIMESTAMP_CHANGED)
            .orEmpty()
            .merge(flowOf(DefaultConsumable(false)))
            .transformWhileSubscribed(scope = scope) { flow ->
                flow.throttleLatest { consumable ->
                    val couldConsume = consumable.tryConsume()
                    exponentialRetry {
                        rpcFeatureApi.fRpcTimeZoneApi.getTime(couldConsume)
                    }
                }
            }
            .asFlow()
            .wrap()
    }

    override suspend fun setTimestamp(timestampInfo: TimestampInfo): CResult<Unit> {
        return rpcFeatureApi.fRpcTimeZoneApi.postTimeTimestamp(timestampInfo).toCResult()
    }

    override fun getTimeZoneInfoFlow(): WrappedFlow<TimezoneInfo> {
        return fEventsFeatureApi
            ?.getUpdateFlow(UpdateEvent.TIMEZONE_CHANGED)
            .orEmpty()
            .merge(flowOf(DefaultConsumable(false)))
            .transformWhileSubscribed(scope = scope) { flow ->
                flow.throttleLatest { consumable ->
                    val couldConsume = consumable.tryConsume()
                    exponentialRetry {
                        rpcFeatureApi.fRpcTimeZoneApi.getTimeTimezone(couldConsume)
                    }
                }
            }
            .asFlow()
            .wrap()
    }

    override suspend fun setTimezone(timezoneInfo: TimezoneInfo): CResult<Unit> {
        return rpcFeatureApi.fRpcTimeZoneApi.postTimeTimezone(timezoneInfo).toCResult()
    }

    override suspend fun getTimezones(): CResult<TimezoneListResponse> {
        return rpcFeatureApi.fRpcTimeZoneApi.getTimeTzList().toCResult()
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
        fun provideFeatureFactory(
            factory: Factory
        ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
            return FDeviceFeature.TIME_ZONE to factory
        }
    }
}
