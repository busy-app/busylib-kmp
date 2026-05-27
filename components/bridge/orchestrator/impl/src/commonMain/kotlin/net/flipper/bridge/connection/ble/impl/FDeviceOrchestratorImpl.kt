package net.flipper.bridge.connection.ble.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.configbuilder.api.FDeviceConnectionConfigMapper
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.connection.orchestrator.internal.FInternalDeviceOrchestrator
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.withLock
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import kotlin.time.Duration.Companion.seconds

private val CONNECTING_TIMEOUT = 10.seconds

@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, FDeviceOrchestrator::class)
@ContributesBinding(BusyLibGraph::class, FInternalDeviceOrchestrator::class)
class FDeviceOrchestratorImpl(
    private val deviceHolderFactory: FDeviceHolderFactory,
    private val deviceConnectionConfigMapper: FDeviceConnectionConfigMapper,
    private val globalScope: CoroutineScope
) : FInternalDeviceOrchestrator, LogTagProvider {
    override val TAG = "FDeviceOrchestrator"

    private val transportListenerFlow = MutableStateFlow<FTransportListenerImpl?>(null)
    private val rawStateFlow = transportListenerFlow.flatMapLatest {
        it?.getState() ?: flowOf(FTransportListenerImpl.DEFAULT_STATUS)
    }
    private val stateFlow = rawStateFlow
        .transformLatest { status ->
            emit(status)
            if (status is FDeviceConnectStatus.Connecting.InProgress) {
                delay(CONNECTING_TIMEOUT)
                emit(
                    FDeviceConnectStatus.Connecting.Offline(
                        device = status.device,
                        status = status.status,
                        transportTypes = status.transportTypes
                    )
                )
            }
        }.stateIn(
            globalScope,
            SharingStarted.Lazily,
            FTransportListenerImpl.DEFAULT_STATUS
        ).wrap()
    private var currentDevice: FDeviceHolder<*>? = null
    private val mutex = Mutex()

    override suspend fun connectIfNot(newConfig: BUSYBar) = withLock(mutex, "connect") {
        info { "Request connect for config $newConfig" }

        val connectionConfig = deviceConnectionConfigMapper.getConnectionConfig(newConfig)
        val currentDeviceHolder = currentDevice

        if (currentDeviceHolder != null && currentDeviceHolder.uniqueId == newConfig.uniqueId) {
            val result = currentDeviceHolder.tryToUpdateConnectionConfig(newConfig, connectionConfig)
            if (result.isSuccess) {
                info { "Device already connected, so skip connection" }
                return@withLock
            } else {
                info { "Failed to update current connect, request full reconnection: ${result.exceptionOrNull()}" }
            }
        }

        disconnectInternalUnsafe()

        val localTransportListener = FTransportListenerImpl(newConfig, ::onInternalDisconnect)
        transportListenerFlow.emit(localTransportListener)
        info { "Create new device" }
        currentDevice = deviceHolderFactory.build(
            uniqueId = newConfig.uniqueId,
            config = connectionConfig,
            listener = localTransportListener
        )
        info { "New device created successfully" }
    }

    private fun onInternalDisconnect(deviceHolder: FDeviceHolder<*>, postAction: () -> Unit) {
        globalScope.launch { // Self-disconnect and kill coroutine scope
            withLock(mutex, "disconnect_internal") {
                disconnectInternalUnsafe(deviceHolder)
                postAction()
            }
        }
    }

    override fun getState() = stateFlow

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
        currentDevice = null
        if (currentDeviceLocal != null) {
            info { "Found current device, wait until disconnect" }
            currentDeviceLocal.disconnect()
        }
    }
}
