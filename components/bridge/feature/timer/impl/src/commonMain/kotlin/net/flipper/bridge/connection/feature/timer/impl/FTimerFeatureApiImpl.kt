package net.flipper.bridge.connection.feature.timer.impl

import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureKey
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.get
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.timer.api.FTimerFeatureApi
import net.flipper.bridge.connection.feature.timer.api.model.BusyProfileSlot
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedSharedFlow
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.log.LogTagProvider

class FTimerFeatureApiImpl(
    private val scope: CoroutineScope,
    private val rpcFeatureApi: FRpcFeatureApi,
    private val fEventsFeatureApi: FEventsFeatureApi?,
) : FTimerFeatureApi, LogTagProvider {
    override val TAG: String = "FTimerFeatureApi"

    private val snapshotsFlow = fEventsFeatureApi
        .get<BusyLibUpdateEvent.Timer, String>(
            scope = scope,
            initial = { _ ->
                rpcFeatureApi.fRpcBusyApi
                    .getBusySnapshot()
                    .map(BusyLibUpdateEvent::Timer)
            },
            mapper = { flow -> flow.map { event -> event.json } }
        )
        .wrap()

    private val profileFlows: Map<BusyProfileSlot, WrappedSharedFlow<String>> =
        BusyProfileSlot.entries.associateWith { slot -> createProfileFlow(slot) }

    override fun getSnapshotsFlow(): WrappedSharedFlow<String> = snapshotsFlow

    override fun getProfilesFlow(slot: BusyProfileSlot): WrappedSharedFlow<String> {
        return profileFlows.getValue(slot)
    }

    override suspend fun setSnapshot(rawJson: String): CResult<Unit> {
        return rpcFeatureApi.fRpcBusyApi.setBusySnapshot(rawJson).toCResult()
    }

    override suspend fun setProfile(slot: BusyProfileSlot, rawJson: String): CResult<Unit> {
        return rpcFeatureApi.fRpcBusyApi.setBusyProfile(slot.slug, rawJson).toCResult()
    }

    private fun createProfileFlow(slot: BusyProfileSlot): WrappedSharedFlow<String> {
        return fEventsFeatureApi
            .get<BusyLibUpdateEvent.Profiles, String>(
                scope = scope,
                initial = { _ ->
                    rpcFeatureApi.fRpcBusyApi
                        .getBusyProfile(slot.slug)
                        .map { json -> BusyLibUpdateEvent.Profiles(mapOf(slot.slug to json)) }
                },
                mapper = { flow -> flow.mapNotNull { event -> event.byName[slot.slug] } }
            )
            .wrap()
    }

    @Inject
    @ContributesIntoMap(BusyLibGraph::class, binding<FDeviceFeatureApi.Factory>())
    @FDeviceFeatureKey(FDeviceFeature.TIMER)
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

            return FTimerFeatureApiImpl(
                scope = scope,
                rpcFeatureApi = fRpcFeatureApi,
                fEventsFeatureApi = fEventsFeatureApi,
            )
        }
    }
}
