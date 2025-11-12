package net.flipper.bridge.connection.ble.impl

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.sync.Mutex
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.model.FDeviceBaseModel
import net.flipper.bridge.connection.configbuilder.api.FDeviceConnectionConfigMapper
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
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
    private val deviceConnectionConfigMapper: FDeviceConnectionConfigMapper
) : FDeviceOrchestrator, LogTagProvider {
    override val TAG = "FDeviceOrchestrator"

    private val transportListener = FTransportListenerImpl()
    private var currentDevice: FDeviceHolder<*>? = null
    private val mutex = Mutex()
    override suspend fun connect(config: FDeviceBaseModel) = withLock(mutex, "connect") {
        info { "Request connect for config $config" }

        disconnectInternalUnsafe()

        info { "Create new device" }
        currentDevice = deviceHolderFactory.build(
            config = deviceConnectionConfigMapper.getConnectionConfig(config),
            listener = { transportListener.onStatusUpdate(config, it) },
            onConnectError = {
                transportListener.onErrorDuringConnect(config, it)
                error(it) { "Failed connect" }
            },
            exceptionHandler = CoroutineExceptionHandler { _, exception ->
                transportListener.onErrorDuringConnect(config, exception)
            }
        )
    }

    override fun getState() = transportListener.getState().wrap()

    override suspend fun disconnectCurrent() = withLock(mutex, "disconnect") {
        disconnectInternalUnsafe()
    }

    private suspend fun disconnectInternalUnsafe() {
        val currentDeviceLocal = currentDevice
        if (currentDeviceLocal != null) {
            info { "Found current device, wait until disconnect" }
            currentDeviceLocal.disconnect()
        }
        currentDevice = null
    }
}
