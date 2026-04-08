package net.flipper.bridge.connection.feature.timezone.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate.Formats
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.get
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.timezone.api.model.TimezoneInfo
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.asFlow
import net.flipper.core.busylib.log.LogTagProvider
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

class FTimeZoneFeatureApiImpl(
    private val scope: CoroutineScope,
    private val rpcFeatureApi: FRpcFeatureApi,
    private val fEventsFeatureApi: FEventsFeatureApi?,
) : FTimeZoneFeatureApi, LogTagProvider {
    override val TAG: String = "FTimeZoneFeatureApi"

    private val timeZoneInfoSharedFlow = fEventsFeatureApi
        .get(
            scope = scope,
            initial = { couldConsume ->
                rpcFeatureApi
                    .fRpcTimeZoneApi
                    .getTimeTimezone(couldConsume)
                    .mapCatching { response ->
                        response.toEvent()
                    }
            },
            mapper = { flow ->
                flow.map { event ->
                    TimezoneInfo(
                        name = event.name,
                        offset = event.offset.format(UtcOffset.Formats.ISO),
                        abbr = event.abbreviation
                    )
                }
            }
        )
        .asFlow()
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
        fun provideFeatureFactory(
            factory: Factory
        ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
            return FDeviceFeature.TIME_ZONE to factory
        }
    }
}
