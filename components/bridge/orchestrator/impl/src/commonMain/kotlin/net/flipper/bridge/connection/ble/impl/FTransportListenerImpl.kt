package net.flipper.bridge.connection.ble.impl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.orchestrator.api.model.ConnectingStatus
import net.flipper.bridge.connection.orchestrator.api.model.DisconnectStatus
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info

class FTransportListenerImpl : LogTagProvider {
    override val TAG = "FTransportListener"

    private val state = MutableStateFlow<FDeviceConnectStatus>(
        FDeviceConnectStatus.Disconnected(
            device = null,
            reason = DisconnectStatus.NOT_INITIALIZED
        )
    )

    fun getState() = state.asStateFlow()

    fun onErrorDuringConnect(device: BUSYBar, throwable: Throwable) {
        @Suppress("UNUSED_EXPRESSION")
        when (throwable) {
            else -> {
                error(throwable) { "Unknown error from transport layer" }
                state.update {
                    FDeviceConnectStatus.Disconnected(
                        device = device,
                        reason = DisconnectStatus.ERROR_UNKNOWN
                    )
                }
            }
        }
    }

    fun onStatusUpdate(device: BUSYBar, status: FInternalTransportConnectionStatus) {
        val newState = state.updateAndGet { currentStatus ->
            when (status) {
                is FInternalTransportConnectionStatus.Connected -> FDeviceConnectStatus.Connected(
                    device = device,
                    deviceApi = status.deviceApi,
                    scope = status.scope
                )

                FInternalTransportConnectionStatus.Connecting ->
                    FDeviceConnectStatus.Connecting(
                        device = device,
                        status = ConnectingStatus.CONNECTING
                    )

                FInternalTransportConnectionStatus.Disconnected ->
                    if (currentStatus is FDeviceConnectStatus.Disconnected) {
                        currentStatus
                    } else {
                        FDeviceConnectStatus.Disconnected(
                            device = device,
                            reason = DisconnectStatus.REPORTED_BY_TRANSPORT
                        )
                    }

                FInternalTransportConnectionStatus.Disconnecting -> FDeviceConnectStatus.Disconnecting(
                    device
                )

                FInternalTransportConnectionStatus.Pairing ->
                    FDeviceConnectStatus.Connecting(
                        device = device,
                        status = ConnectingStatus.INITIALIZING
                    )
            }
        }
        info { "New state is $newState" }
    }
}
