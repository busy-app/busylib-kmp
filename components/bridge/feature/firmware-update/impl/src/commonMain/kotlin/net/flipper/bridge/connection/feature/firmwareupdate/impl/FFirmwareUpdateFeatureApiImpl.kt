package net.flipper.bridge.connection.feature.firmwareupdate.impl

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import net.flipper.bridge.connection.feature.events.api.EventsKey
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.UpdateEvent
import net.flipper.bridge.connection.feature.events.api.getUpdateFlow
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbVersionChangelog
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.cache.DefaultSingleObjectCache
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.merge
import net.flipper.core.busylib.ktx.common.orEmpty
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.core.busylib.log.info

@Suppress("UnusedPrivateProperty")
class FFirmwareUpdateFeatureApiImpl(
    private val rpcFeatureApi: FRpcFeatureApi,
    private val fEventsFeatureApi: FEventsFeatureApi?,
) : FFirmwareUpdateFeatureApi, LogTagProvider {
    override val TAG: String = "FFirmwareUpdateFeatureApi"

    private val updateStatusCache = DefaultSingleObjectCache<UpdateStatus>()
    private suspend fun requireUpdateStatus(key: EventsKey?): UpdateStatus {
        return updateStatusCache.getOrElse(key) {
            exponentialRetry {
                rpcFeatureApi.fRpcUpdaterApi
                    .getUpdateStatus()
                    .onFailure { throwable -> error(throwable) { "Failed to get update status" } }
            }
        }
    }

    override fun getUpdateStatusFlow(): WrappedFlow<UpdateStatus> {
        return fEventsFeatureApi
            ?.getUpdateFlow(UpdateEvent.UPDATER_UPDATE_STATUS)
            .orEmpty()
            .merge(flowOf(null))
            .map { key ->
                requireUpdateStatus(key)
            }
            .wrap()
    }

    private suspend fun tryStartInstantUpdate(): Boolean {
        val status = requireUpdateStatus(null)
        when (status.install.action) {
            UpdateStatus.Install.Action.APPLY,
            UpdateStatus.Install.Action.PREPARE,
            UpdateStatus.Install.Action.UNPACK,
            UpdateStatus.Install.Action.SHA_VERIFICATION,
            UpdateStatus.Install.Action.DOWNLOAD -> {
                info { "#tryStartInstantUpdate already downloading" }
                return true
            }

            UpdateStatus.Install.Action.NONE -> Unit
        }
        val version = status
            .check
            .availableVersion
            .takeIf(String::isNotEmpty)
        if (version == null) return false
        info { "#tryStartInstantUpdate got new version $version" }
        return rpcFeatureApi.fRpcUpdaterApi.startUpdateInstall(version).isSuccess
    }

    override suspend fun beginFirmwareUpdate(): Result<Unit> {
        if (tryStartInstantUpdate()) return Result.success(Unit)

        rpcFeatureApi.fRpcUpdaterApi.startUpdateCheck()
            .onFailure { throwable -> error(throwable) { "#tryStartInstantUpdate could not start update check" } }
            .onFailure { throwable -> return Result.failure(throwable) }
        val version = updateStatusCache.flow
            .map { status -> status.check.availableVersion }
            .filter { version -> version.isNotEmpty() }
            .first()
        return rpcFeatureApi.fRpcUpdaterApi.startUpdateInstall(version).map { }
    }

    override suspend fun stopFirmwareUpdate(): Result<Unit> {
        return rpcFeatureApi.fRpcUpdaterApi.startUpdateAbortDownload().map { }
    }

    override suspend fun getNextVersionChangelog(): Result<BsbVersionChangelog> {
        val version = updateStatusCache.flow
            .map { status -> status.check.availableVersion }
            .filter { version -> version.isNotEmpty() }
            .first()
        return rpcFeatureApi.fRpcUpdaterApi
            .getUpdateChangelog(version)
            .map { changelog ->
                BsbVersionChangelog(
                    version = version,
                    changelog = changelog.changelog
                )
            }
    }
}
