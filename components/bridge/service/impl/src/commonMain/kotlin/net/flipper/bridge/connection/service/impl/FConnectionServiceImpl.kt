package net.flipper.bridge.connection.service.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.DisconnectStatus
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.connection.service.api.FConnectionService
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.warn
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(BusyLibGraph::class)
@Inject
@ContributesBinding(BusyLibGraph::class, FConnectionService::class)
class FConnectionServiceImpl(
    private val orchestrator: FDeviceOrchestrator,
    private val fDevicePersistedStorage: FDevicePersistedStorage
) : FConnectionService, LogTagProvider {
    override val TAG: String = "FConnectionService"

    private val scope = CoroutineScope(SupervisorJob() + FlipperDispatchers.default)
    private val mutex = Mutex()
    private val isForceDisconnected = MutableStateFlow(false)

    private fun getBrokenConnectionReconnectJob(scope: CoroutineScope): Job {
        return orchestrator.getState()
            .onEach { status ->
                if (status !is FDeviceConnectStatus.Disconnected) return@onEach
                when (status.reason) {
                    DisconnectStatus.NOT_INITIALIZED -> return@onEach
                    DisconnectStatus.REPORTED_BY_TRANSPORT -> Unit
                    DisconnectStatus.ERROR_UNKNOWN -> Unit
                }
                if (isForceDisconnected.first()) return@onEach
                val currentDevice = status.device ?: return@onEach
                orchestrator.disconnectCurrent()
                orchestrator.connect(currentDevice)
            }.launchIn(scope)
    }

    private fun getConnectionJob(scope: CoroutineScope): Job {
        return combine(
            flow = fDevicePersistedStorage.getCurrentDevice(),
            flow2 = isForceDisconnected,
            transform = { currentDevice, isForceDisconnected ->
                when {
                    isForceDisconnected -> orchestrator.disconnectCurrent()
                    currentDevice == null -> orchestrator.disconnectCurrent()

                    else -> orchestrator.connect(currentDevice)
                }
            }
        ).launchIn(scope)
    }

    override fun onApplicationInit() {
        scope.launch {
            if (mutex.isLocked) {
                warn { "#onApplicationInit tried to init connection service again" }
                return@launch
            }
            mutex.withLock {
                val brokenConnectionReconnectJob = getBrokenConnectionReconnectJob(this)
                val connectionJob = getConnectionJob(this)
                connectionJob.join()
                brokenConnectionReconnectJob.join()
            }
        }
    }

    override fun forceReconnect() {
        scope.launch { isForceDisconnected.emit(false) }
    }

    override fun disconnect(force: Boolean) {
        scope.launch {
            isForceDisconnected.emit(force)
        }
    }

    override fun forgetCurrentDevice() {
        scope.launch {
            fDevicePersistedStorage.getCurrentDevice()
                .first()
                ?.let { currentDevice ->
                    fDevicePersistedStorage.removeDevice(currentDevice.uniqueId)
                }
        }
    }

    override fun connectIfNotForceDisconnect() {
        scope.launch {
            if (isForceDisconnected.first()) return@launch
            val currentDevice = fDevicePersistedStorage.getCurrentDevice()
                .first()
                ?: return@launch
            orchestrator.connect(currentDevice)
        }
    }
}
