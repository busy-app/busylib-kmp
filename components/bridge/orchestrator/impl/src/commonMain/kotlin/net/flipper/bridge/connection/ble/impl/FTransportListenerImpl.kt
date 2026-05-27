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
import net.flipper.bridge.connection.transport.common.api.FInternalDisconnectedReason.PAIRING_FAILED
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.bridge.connection.transport.common.api.FailedPairingConnectException
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.data.map
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info

class FTransportListenerImpl(
    initConfig: BUSYBar,
    private val onInternalDisconnect: (deviceHolder: FDeviceHolder<*>, postAction: () -> Unit) -> Unit
) : LogTagProvider {
    override val TAG = "FTransportListener-${initConfig.uniqueId}"
    private var config = initConfig

    private val state = MutableStateFlow<FDeviceConnectStatus>(DEFAULT_STATUS)

    fun getState() = state.asStateFlow()

    fun updateConfig(newConfig: BUSYBar) {
        info { "Update config $config -> $newConfig" }
        config = newConfig
    }

    fun onErrorDuringConnect(deviceHolder: FDeviceHolder<*>, throwable: Throwable) {
        onInternalDisconnect(deviceHolder) {
            onErrorDuringConnectInternal(throwable)
        }
    }

    private fun onErrorDuringConnectInternal(throwable: Throwable) {
        @Suppress("UNUSED_EXPRESSION")
        when (throwable) {
            is FailedPairingConnectException -> {
                error(throwable) { "Not recoverable error from transport layer" }
                state.update {
                    FDeviceConnectStatus.Disconnected(
                        device = config,
                        reason = DisconnectStatus.PAIRING_FAILED
                    )
                }
            }

            else -> {
                error(throwable) { "Unknown error from transport layer" }
                state.update {
                    FDeviceConnectStatus.Disconnected(
                        device = config,
                        reason = DisconnectStatus.ERROR_UNKNOWN
                    )
                }
            }
        }
    }

    fun onStatusUpdate(deviceHolder: FDeviceHolder<*>, status: FInternalTransportConnectionStatus) {
        info { "Received status update for device ${deviceHolder.uniqueId}: $status" }
        if (status is FInternalTransportConnectionStatus.Disconnected) {
            onInternalDisconnect(deviceHolder) {
                onStatusUpdateInternal(status)
            }
        } else {
            onStatusUpdateInternal(status)
        }
    }

    private fun onStatusUpdateInternal(status: FInternalTransportConnectionStatus) {
        val newState = state.updateAndGet { currentStatus ->
            when (status) {
                is FInternalTransportConnectionStatus.Connected -> FDeviceConnectStatus.Connected(
                    device = config,
                    deviceApi = status.deviceApi,
                    scope = status.scope,
                    transportType = status.connectionTypes.maxBy { it.priority }.toPublic()
                )

                is FInternalTransportConnectionStatus.Connecting ->
                    FDeviceConnectStatus.Connecting.InProgress(
                        device = config,
                        status = ConnectingStatus.CONNECTING,
                        transportTypes = status.connectionTypes.map { it.toPublic() }.wrap()
                    )

                is FInternalTransportConnectionStatus.Disconnected -> {
                    currentStatus as? FDeviceConnectStatus.Disconnected
                        ?: FDeviceConnectStatus.Disconnected(
                            device = config,
                            reason = when (status.reason) {
                                PAIRING_FAILED -> DisconnectStatus.PAIRING_FAILED
                                OTHER -> DisconnectStatus.REPORTED_BY_TRANSPORT
                            }
                        )
                }

                FInternalTransportConnectionStatus.Disconnecting -> FDeviceConnectStatus.Disconnecting(
                    config
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
