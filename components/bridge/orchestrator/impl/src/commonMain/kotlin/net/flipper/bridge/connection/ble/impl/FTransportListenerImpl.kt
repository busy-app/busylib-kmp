package net.flipper.bridge.connection.ble.impl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.orchestrator.api.model.ConnectingStatus
import net.flipper.bridge.connection.orchestrator.api.model.DisconnectStatus
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.connection.transport.common.api.FInternalDisconnectedReason.OTHER
import net.flipper.bridge.connection.transport.common.api.FInternalDisconnectedReason.REQUIRES_REPAIRING
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FailedPairingConnectException
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.data.map
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info

class FTransportListenerImpl(config: BUSYBar) : LogTagProvider {
    override val TAG = "FTransportListener-${config.uniqueId}"

    private val state = MutableStateFlow<FDeviceConnectStatus>(DEFAULT_STATUS)

    fun getState() = state.asStateFlow()

    fun onErrorDuringConnect(device: BUSYBar, throwable: Throwable) {
        @Suppress("UNUSED_EXPRESSION")
        when (throwable) {
            is FailedPairingConnectException -> {
                error(throwable) { "Not recoverable error from transport layer" }
                state.update {
                    FDeviceConnectStatus.Disconnected(
                        device = device,
                        reason = DisconnectStatus.REQUIRES_REPAIRING
                    )
                }
            }
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
        val newState = state.updateAndGet {
            when (status) {
                is FInternalTransportConnectionStatus.Connected -> FDeviceConnectStatus.Connected(
                    device = device,
                    deviceApi = status.deviceApi,
                    scope = status.scope,
                    transportType = status.connectionTypes.maxBy { it.priority }.toPublic()
                )

                is FInternalTransportConnectionStatus.Connecting -> FDeviceConnectStatus.Connecting.InProgress(
                    device = device,
                    status = ConnectingStatus.CONNECTING,
                    transportTypes = status.connectionTypes.map { it.toPublic() }.wrap()
                )

                is FInternalTransportConnectionStatus.Disconnected -> {
                    val reason = when (status.reason) {
                        REQUIRES_REPAIRING -> DisconnectStatus.REQUIRES_REPAIRING
                        OTHER -> DisconnectStatus.REPORTED_BY_TRANSPORT
                    }
                    FDeviceConnectStatus.Disconnected(
                        device = device,
                        reason = reason
                    )
                }
                FInternalTransportConnectionStatus.Disconnecting -> FDeviceConnectStatus.Disconnecting(
                    device
                )
            }
        }
        info { "New state is $newState" }
    }

    companion object {
        val DEFAULT_STATUS = FDeviceConnectStatus.Disconnected(
            device = null,
            reason = DisconnectStatus.NOT_INITIALIZED
        )
    }
}
