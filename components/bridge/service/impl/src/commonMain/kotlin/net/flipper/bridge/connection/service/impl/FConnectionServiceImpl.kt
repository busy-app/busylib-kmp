package net.flipper.bridge.connection.service.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import dev.zacsweers.metro.Inject
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.internal.FInternalDevicePersistedStorage
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.bridge.connection.orchestrator.internal.FInternalDeviceOrchestrator
import net.flipper.bridge.connection.service.api.FConnectionService
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.rest.api.BusyCloudBarsApi
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.warn
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.binding
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.SingleIn

@SingleIn(BusyLibGraph::class)
@Inject
@ContributesBinding(BusyLibGraph::class, binding = binding<FConnectionService>())
@ContributesIntoSet(BusyLibGraph::class, binding = binding<InternalBUSYLibStartupListener>())
class FConnectionServiceImpl(
    private val orchestrator: FInternalDeviceOrchestrator,
    private val fDevicePersistedStorage: FInternalDevicePersistedStorage,
    private val barsApi: BusyCloudBarsApi,
    private val principalApi: BUSYLibPrincipalApi
) : FConnectionService, LogTagProvider, InternalBUSYLibStartupListener {
    override val TAG: String = "FConnectionService"

    private val scope = CoroutineScope(SupervisorJob() + FlipperDispatchers.default)
    private val mutex = Mutex()
    private fun getExpectedState(): Flow<ExpectedState> {
        return fDevicePersistedStorage.getCurrentDeviceFlow()
            .map { currentDevice ->
                if (currentDevice == null) {
                    return@map ExpectedState.Disconnected
                }
                return@map ExpectedState.Connected(currentDevice)
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

    override suspend fun forgetDevice(
        device: BUSYBar
    ): CResult<Unit> {
        return fDevicePersistedStorage.transactionInternal {
            val devices = getAllDevices()

            val isDeviceExists = devices
                .firstOrNull { listDevice -> listDevice.uniqueId == device.uniqueId } != null
            if (!isDeviceExists) {
                warn { "#unpairDevice Can't find device $device" }
                return@transactionInternal CResult.success(Unit)
            }
            val deviceId = device.cloud?.deviceId
            if (deviceId != null) {
                val principal = principalApi.getPrincipalFlow()
                    .filter { principal -> principal !is BUSYLibUserPrincipal.Loading }
                    .first() as? BUSYLibUserPrincipal.Token
                if (principal == null) {
                    return@transactionInternal CResult.failure(IllegalStateException("User not authorized"))
                }
                val result = barsApi
                    .unlinkBusyBar(principal, deviceId)
                    .toCResult()
                if (result.isFailure) return@transactionInternal result
            }
            removeDevice(device.uniqueId)

            return@transactionInternal CResult.success(Unit)
        }
    }
}
