package net.flipper.bridge.connection.feature.events.impl

import BSB_State.State
import com.squareup.wire.internal.ProtocolException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.model.BusyLibUpdateEvent
import net.flipper.bridge.connection.feature.events.model.ConsumableUpdateEvent
import net.flipper.bridge.connection.feature.events.proto.protomapper.BSBProtobufEventMapper
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.serial.FStatusStreamingApi
import net.flipper.bridge.connection.transport.common.api.serial.StatusStreamingEvent
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.verbose
import net.flipper.core.busylib.log.warn
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

class FEventsFeatureApiImpl(
    streamingApi: FStatusStreamingApi?,
    private val scope: CoroutineScope,
    override val TAG: String = "FEventsFeatureApi"
) : FEventsFeatureApi, LogTagProvider {
    private val busyLibUpdateEventFlow = MutableSharedFlow<BusyLibUpdateEvent>()

    override fun onBusyLibEvent(event: BusyLibUpdateEvent) {
        scope.launch { busyLibUpdateEventFlow.emit(event) }
    }

    override fun getBusyLibUpdateEvents(): Flow<ConsumableUpdateEvent.BusyLib<*>> {
        return busyLibUpdateEventFlow
            .map { busyLibUpdateEvent -> ConsumableUpdateEvent.BusyLib(busyLibUpdateEvent) }
    }

    private fun collectProtobufChanges(streamingApi: FStatusStreamingApi) {
        streamingApi
            .getEvents()
            .onEach { event ->
                when (event) {
                    is StatusStreamingEvent.Protobuf -> {
                        onProtobufStatesUpdate(data = event)
                            .onFailure { t ->
                                if (t is ProtocolException) {
                                    val size = event.data.size
                                    val preview = event.data.hexPreview()
                                    warn {
                                        "Skipping malformed protobuf frame (${size}B): $preview | ${t.message}"
                                    }
                                } else {
                                    error(t) { "Failed to process $event" }
                                }
                            }
                    }
                }
            }
            .launchIn(scope)
    }

    private suspend fun onProtobufStatesUpdate(
        data: StatusStreamingEvent.Protobuf
    ) = runSuspendCatching {
        if (data.data.isEmpty() || data.data.isAllZero()) {
            verbose { "Skipping ${data.data.size}B no-op protobuf frame" }
            return@runSuspendCatching
        }
        val state = State.ADAPTER.decode(data.data)
        verbose { "Process ${state.updates.size} updates: $state" }
        val updates = state.updates.mapNotNull { update ->
            BSBProtobufEventMapper.map(update)
        }
        updates.forEach { update ->
            busyLibUpdateEventFlow.emit(update)
        }
    }

    private fun ByteArray.isAllZero(): Boolean = all { byte -> byte == 0.toByte() }

    private fun ByteArray.hexPreview(maxBytes: Int = HEX_PREVIEW_MAX_BYTES): String =
        take(maxBytes).joinToString(" ") { byte -> byte.toUByte().toString(HEX_RADIX).padStart(2, '0') }
            .let { if (size > maxBytes) "$it..." else it }

    init {
        if (streamingApi != null) {
            collectProtobufChanges(streamingApi)
        }
    }

    @Inject
    class Factory : FDeviceFeatureApi.Factory {
        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FDeviceFeatureApi {
            return FEventsFeatureApiImpl(
                scope = scope,
                streamingApi = connectedDevice as? FStatusStreamingApi
            )
        }
    }

    private companion object {
        private const val HEX_PREVIEW_MAX_BYTES = 16
        private const val HEX_RADIX = 16
    }

    @ContributesTo(BusyLibGraph::class)
    interface Component {
        @Provides
        @IntoMap
        fun provideFeatureFactory(
            factory: Factory
        ): Pair<FDeviceFeature, FDeviceFeatureApi.Factory> {
            return FDeviceFeature.EVENTS to factory
        }
    }
}
