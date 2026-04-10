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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.model.BUSYBar
import net.flipper.bridge.connection.config.internal.FInternalDevicePersistedStorage
import net.flipper.bridge.connection.orchestrator.internal.FInternalDeviceOrchestrator
import net.flipper.bridge.connection.service.api.FConnectionService
import net.flipper.bridge.connection.service.mapper.ConnectionStatusMapper
import net.flipper.bridge.connection.service.model.ConnectionAction
import net.flipper.bridge.connection.service.model.ExpectedState
import net.flipper.bridge.connection.service.model.ForgetDeviceResult
import net.flipper.bsb.auth.principal.api.BUSYLibPrincipalApi
import net.flipper.bsb.auth.principal.api.BUSYLibUserPrincipal
import net.flipper.bsb.cloud.rest.api.BusyCloudBarsApi
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.core.busylib.ktx.common.FlipperDispatchers
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info
import net.flipper.core.busylib.log.warn
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(BusyLibGraph::class)
@Inject
@ContributesBinding(BusyLibGraph::class, FConnectionService::class)
@ContributesBinding(BusyLibGraph::class, InternalBUSYLibStartupListener::class, multibinding = true)
class FConnectionServiceImpl(
    private val orchestrator: FInternalDeviceOrchestrator,
    private val fDevicePersistedStorage: FInternalDevicePersistedStorage,
    private val barsApi: BusyCloudBarsApi,
    private val principalApi: BUSYLibPrincipalApi,
) : FConnectionService, LogTagProvider, InternalBUSYLibStartupListener {
    override val TAG: String = "FConnectionService"

    private val scope = CoroutineScope(SupervisorJob() + FlipperDispatchers.default)
    private val mutex = Mutex()
    private fun getExpectedState(): Flow<ExpectedState> {
        return fDevicePersistedStorage.getCurrentDeviceFlow()
            .map { currentDevice ->
                info { "#getExpectedState currentDevice: $currentDevice" }
                if (currentDevice == null) {
                    return@map ExpectedState.Disconnected
                }
                return@map ExpectedState.Connected(currentDevice)
            }
            .distinctUntilChanged()
    }

    private fun getConnectionJob(scope: CoroutineScope): Job {
        val actionFlow = combine(
            flow = getExpectedState(),
            flow2 = orchestrator.getState(),
            transform = { expectedState, realState ->
                ConnectionStatusMapper.mapAction(expectedState, realState)
            }
        )
        return actionFlow
            .distinctUntilChanged()
            .onEach { action ->
                info { "#getConnectionJob action: $action" }
                when (action) {
                    is ConnectionAction.Connect -> orchestrator.connectIfNot(action.device)
                    ConnectionAction.Disconnect -> orchestrator.disconnectCurrent()
                    ConnectionAction.Skip -> Unit
                }
            }
            .launchIn(scope)
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
    ): ForgetDeviceResult {
        info { "#forgetDevice" }
        return fDevicePersistedStorage.transactionInternal {
            val isDeviceExists = getAllDevices()
                .firstOrNull { listDevice -> listDevice.uniqueId == device.uniqueId } != null
            if (!isDeviceExists) {
                warn { "#forgetDevice Can't find device $device" }
                return@transactionInternal ForgetDeviceResult.DEVICE_NOT_FOUND
            }
            val cloudDeviceId = device.cloud?.deviceId

            if (cloudDeviceId != null) {
                val principal = principalApi.getPrincipalFlow()
                    .filter { principal -> principal !is BUSYLibUserPrincipal.Loading }
                    .first() as? BUSYLibUserPrincipal.Token
                if (principal == null) {
                    info { "#forgetDevice user not authorized" }
                    return@transactionInternal ForgetDeviceResult.NOT_AUTHORIZED
                }
                val cloudBarsList = barsApi.getBarsList(principal)
                    .onFailure { t -> error(t) { "#forgetDevice Could not get bars list" } }
                    .getOrNull()
                if (cloudBarsList == null) {
                    return@transactionInternal ForgetDeviceResult.COULD_NOT_GET_CLOUD_BARS_LIST
                }
                val isBarOnCloud = cloudBarsList
                    .firstOrNull { busyCloudBar -> busyCloudBar.id == "$cloudDeviceId" } != null
                if (isBarOnCloud) {
                    val result = barsApi
                        .unlinkBusyBar(principal, cloudDeviceId)
                        .onFailure { t -> error(t) { "#forgetDevice Could not unlink from bars api" } }
                        .toCResult()
                    if (result.isFailure) {
                        return@transactionInternal ForgetDeviceResult.COULD_NOT_UNLINK_CLOUD_ACCOUNT
                    }
                }
            }
            removeDevice(device.uniqueId)

            info { "#forgetDevice device forgotten" }
            return@transactionInternal ForgetDeviceResult.SUCCESS
        }
    }
}
