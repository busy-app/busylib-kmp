package net.flipper.bridge.connection.transport.tcp.lan.impl.monitor

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FTransportConnectionStatusListener
import net.flipper.bridge.connection.transport.tcp.lan.FLanDeviceConnectionConfig
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.log.LogTagProvider
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
class FLanConnectionMonitorImpl(
    @Assisted private val listener: FTransportConnectionStatusListener,
    @Suppress("UnusedParameter")
    @Assisted private val config: FLanDeviceConnectionConfig
) : FLanConnectionMonitorApi, LogTagProvider {
    override val TAG: String = "FLanConnectionMonitor"

    override suspend fun startMonitoring(
        scope: CoroutineScope,
        deviceApi: FConnectedDeviceApi
    ) {
        listener.onStatusUpdate(
            FInternalTransportConnectionStatus.Connected(
                scope = scope,
                deviceApi = deviceApi
            )
        )
    }

    override fun stopMonitoring() = Unit

    @Inject
    @ContributesBinding(BusyLibGraph::class, FLanConnectionMonitorApi.Factory::class)
    class InternalFactory(
        private val factory: (
            FTransportConnectionStatusListener,
            FLanDeviceConnectionConfig
        ) -> FLanConnectionMonitorImpl
    ) : FLanConnectionMonitorApi.Factory {
        override operator fun invoke(
            listener: FTransportConnectionStatusListener,
            config: FLanDeviceConnectionConfig
        ): FLanConnectionMonitorApi = factory(listener, config)
    }
}
