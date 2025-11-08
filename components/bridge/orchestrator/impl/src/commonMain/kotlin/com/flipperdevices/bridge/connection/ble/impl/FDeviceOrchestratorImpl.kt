package com.flipperdevices.bridge.connection.ble.impl

import com.flipperdevices.bridge.connection.config.api.model.FDeviceBaseModel
import com.flipperdevices.bridge.connection.configbuilder.api.FDeviceConnectionConfigMapper
import com.flipperdevices.bridge.connection.orchestrator.api.FDeviceOrchestrator
import com.flipperdevices.busylib.core.di.BusyLibGraph
import com.flipperdevices.core.busylib.ktx.common.withLock
import com.flipperdevices.core.busylib.log.LogTagProvider
import com.flipperdevices.core.busylib.log.error
import com.flipperdevices.core.busylib.log.info
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject
import com.flipperdevices.busylib.core.di.SingleIn

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.sync.Mutex

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

    override fun getState() = transportListener.getState()

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
