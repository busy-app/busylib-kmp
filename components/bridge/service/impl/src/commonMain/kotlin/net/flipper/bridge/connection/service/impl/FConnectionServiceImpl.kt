package net.flipper.bridge.connection.service.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.cache
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.api.model.FDeviceBaseModel
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


sealed interface ExpectedState {
    data object Disconnected : ExpectedState

    data class Connected(val device: FDeviceBaseModel) : ExpectedState
}


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
    private val isForceDisconnectedFlow = MutableStateFlow(false)

    private fun getExpectedState(): Flow<ExpectedState> {
        return combine(
            isForceDisconnectedFlow,
            fDevicePersistedStorage.getCurrentDevice()
        ) { isForceDisconnected, currentDevice ->
            if (isForceDisconnected) {
                return@combine ExpectedState.Disconnected
            }
            if (currentDevice == null) {
                return@combine ExpectedState.Disconnected
            }
            return@combine ExpectedState.Connected(currentDevice)
        }.distinctUntilChanged()
    }


    private fun getConnectionJob(scope: CoroutineScope): Job {
        return combine(
            flow = getExpectedState(),
            flow2 = orchestrator.getState()
        ) { expectedState, realState ->
            when (realState) {
                is FDeviceConnectStatus.Connected -> when (expectedState) {
                    is ExpectedState.Connected -> if (expectedState.device != realState.device) {
                        orchestrator.connect(expectedState.device)
                    }

                    ExpectedState.Disconnected -> orchestrator.disconnectCurrent()
                }

                is FDeviceConnectStatus.Disconnected -> when (expectedState) {
                    is ExpectedState.Connected -> orchestrator.connect(expectedState.device)
                    ExpectedState.Disconnected -> Unit
                }

                is FDeviceConnectStatus.Connecting -> when (expectedState) {
                    is ExpectedState.Connected -> if (expectedState.device != realState.device) {
                        orchestrator.connect(expectedState.device)
                    }

                    ExpectedState.Disconnected -> orchestrator.disconnectCurrent()
                }

                is FDeviceConnectStatus.Disconnecting -> Unit // Transition state
            }
        }.launchIn(scope)
    }

    override fun onApplicationInit() {
        scope.launch {
            if (mutex.isLocked) {
                warn { "#onApplicationInit tried to init connection service again" }
                return@launch
            }
            mutex.withLock {
                val connectionJob = getConnectionJob(this)
                connectionJob.join()
            }
        }
    }

    override fun connectCurrent() {
        scope.launch {
            isForceDisconnectedFlow.emit(false)
        }
    }

    override fun disconnect() {
        scope.launch {
            isForceDisconnectedFlow.emit(true)
        }
    }

    override fun forgetCurrentDevice() {
        scope.launch {
            isForceDisconnectedFlow.emit(true)
            fDevicePersistedStorage.getCurrentDevice()
                .first()
                ?.let { currentDevice ->
                    fDevicePersistedStorage.removeDevice(currentDevice.uniqueId)
                }
        }
    }
}
