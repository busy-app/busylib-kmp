package net.flipper.bridge.connection.service.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.connection.service.api.FConnectionService
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.rest.api.BusyCloudRestApi
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.warn
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(BusyLibGraph::class)
@Inject
@ContributesBinding(BusyLibGraph::class, FConnectionService::class)
@ContributesBinding(BusyLibGraph::class, InternalBUSYLibStartupListener::class, multibinding = true)
class FConnectionServiceImpl(
    private val orchestrator: FDeviceOrchestrator,
    private val fDevicePersistedStorage: FDevicePersistedStorage,
    private val busyCloudRestApi: BusyCloudRestApi,
    private val principalApi: BUSYLibPrincipalApi
) : FConnectionService, LogTagProvider, InternalBUSYLibStartupListener {
    override val TAG: String = "FConnectionService"

    private val scope = CoroutineScope(SupervisorJob() + FlipperDispatchers.default)
    private val mutex = Mutex()
    private val isForceDisconnectedFlow = MutableStateFlow(false)

    private fun getExpectedState(): Flow<ExpectedState> {
        return combine(
            flow = isForceDisconnectedFlow,
            flow2 = fDevicePersistedStorage.getCurrentDeviceFlow()
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
            info { "expectedState: $expectedState, realState: $realState" }
            when (realState) {
                is FDeviceConnectStatus.Connected -> when (expectedState) {
                    is ExpectedState.Connected -> if (expectedState.device != realState.device) {
                        orchestrator.connectIfNot(expectedState.device)
                    }

                    ExpectedState.Disconnected -> orchestrator.disconnectCurrent()
                }

                is FDeviceConnectStatus.Disconnected -> when (expectedState) {
                    is ExpectedState.Connected -> orchestrator.connectIfNot(expectedState.device)
                    ExpectedState.Disconnected -> Unit
                }

                is FDeviceConnectStatus.Connecting -> when (expectedState) {
                    is ExpectedState.Connected -> if (expectedState.device != realState.device) {
                        orchestrator.connectIfNot(expectedState.device)
                    }

                    ExpectedState.Disconnected -> orchestrator.disconnectCurrent()
                }

                is FDeviceConnectStatus.Disconnecting -> Unit // Transition state
            }
        }.launchIn(scope)
    }

    override fun onLaunch() {
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

    override suspend fun forgetDevice(
        device: BUSYBar
    ): CResult<Unit> {
        return fDevicePersistedStorage.transaction {
            val devices = getAllDevices()
            val currentDevice = getCurrentDevice()

            if (currentDevice?.uniqueId == device.uniqueId) {
                isForceDisconnectedFlow.emit(true)
            }
            val isDeviceExists = devices
                .firstOrNull { listDevice -> listDevice.uniqueId == device.uniqueId } != null
            if (!isDeviceExists) {
                warn { "#unpairDevice Can't find device $device" }
                return@transaction CResult.success(Unit)
            }
            val deviceId = device.cloud?.deviceId
            if (deviceId != null) {
                val principal = principalApi.getPrincipalFlow()
                    .filter { principal -> principal !is BUSYLibUserPrincipal.Loading }
                    .first() as? BUSYLibUserPrincipal.Token
                if (principal == null) {
                    return@transaction CResult.failure(IllegalStateException("User not authorized"))
                }
                val result = busyCloudRestApi.barsApi
                    .unlinkBusyBar(principal, deviceId)
                    .toCResult()
                if (result.isFailure) return@transaction result
            }
            removeDevice(device.uniqueId)

            return@transaction CResult.success(Unit)
        }
    }
}
