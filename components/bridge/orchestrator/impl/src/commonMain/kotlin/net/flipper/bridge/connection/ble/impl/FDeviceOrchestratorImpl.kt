package net.flipper.bridge.connection.ble.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.model.FDeviceCombined
import net.flipper.bridge.connection.configbuilder.api.FDeviceConnectionConfigMapper
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.transport.common.api.FInternalTransportConnectionStatus
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.withLock
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, FDeviceOrchestrator::class)
class FDeviceOrchestratorImpl(
    private val deviceHolderFactory: FDeviceHolderFactory,
    private val deviceConnectionConfigMapper: FDeviceConnectionConfigMapper,
    private val globalScope: CoroutineScope
) : FDeviceOrchestrator, LogTagProvider {
    override val TAG = "FDeviceOrchestrator"

    private val transportListener = FTransportListenerImpl()
    private var currentDevice: FDeviceHolder<*>? = null
    private val mutex = Mutex()

    override suspend fun connectIfNot(config: FDeviceCombined) = withLock(mutex, "connect") {
        info { "Request connect for config $config" }

        val connectionConfig = deviceConnectionConfigMapper.getConnectionConfig(config)
        val currentDeviceHolder = currentDevice

        if (currentDeviceHolder != null && currentDeviceHolder.uniqueId == config.uniqueId) {
            val result = currentDeviceHolder.tryToUpdateConnectionConfig(connectionConfig)
            if (result.isSuccess) {
                info { "Device already connected, so skip connection" }
                return@withLock
            } else {
                info { "Failed to update current connect, request full reconnection: ${result.exceptionOrNull()}" }
            }
        }

        disconnectInternalUnsafe()

        info { "Create new device" }
        currentDevice = deviceHolderFactory.build(
            uniqueId = config.uniqueId,
            config = deviceConnectionConfigMapper.getConnectionConfig(config),
            listener = { deviceHolder, status ->
                if (status is FInternalTransportConnectionStatus.Disconnected) {
                    onInternalDisconnect(deviceHolder) {
                        transportListener.onStatusUpdate(config, status)
                    }
                } else {
                    transportListener.onStatusUpdate(config, status)
                }
            },
            onConnectError = { deviceHolder, error ->
                onInternalDisconnect(deviceHolder) {
                    transportListener.onErrorDuringConnect(config, error)
                }
                error(error) { "Failed connect" }
            },
            exceptionHandler = { deviceHolder, error ->
                onInternalDisconnect(deviceHolder) {
                    transportListener.onErrorDuringConnect(config, error)
                }
                error(error) { "Exception in coroutine" }
            }
        )
    }

    private fun onInternalDisconnect(deviceHolder: FDeviceHolder<*>, postAction: () -> Unit) {
        globalScope.launch { // Self-disconnect and kill coroutine scope
            withLock(mutex, "disconnect_internal") {
                disconnectInternalUnsafe(deviceHolder)
                postAction()
            }
        }
    }

    override fun getState() = transportListener.getState().wrap()

    override suspend fun disconnectCurrent() = withLock(mutex, "disconnect") {
        disconnectInternalUnsafe()
    }

    private suspend fun disconnectInternalUnsafe(configToDisconnect: FDeviceHolder<*>? = null) {
        val currentDeviceLocal = currentDevice
        // Use referential equality (===) to ensure this is the same FDeviceHolder instance
        if (configToDisconnect != null && currentDeviceLocal !== configToDisconnect) {
            info { "Tried to disconnect not current device, skip" }
            return
        }
        if (currentDeviceLocal != null) {
            info { "Found current device, wait until disconnect" }
            currentDeviceLocal.disconnect()
        }
        currentDevice = null
    }
}
