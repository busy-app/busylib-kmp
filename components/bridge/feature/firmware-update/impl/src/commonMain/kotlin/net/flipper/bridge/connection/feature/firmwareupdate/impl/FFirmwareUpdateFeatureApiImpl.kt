package net.flipper.bridge.connection.feature.firmwareupdate.impl

import kotlinx.coroutines.flow.flowOf
import net.flipper.bridge.connection.feature.events.api.FEventsFeatureApi
import net.flipper.bridge.connection.feature.events.api.UpdateEvent
import net.flipper.bridge.connection.feature.events.api.getUpdateFlow
import net.flipper.bridge.connection.feature.firmwareupdate.api.FFirmwareUpdateFeatureApi
import net.flipper.bridge.connection.feature.firmwareupdate.model.BsbVersionChangelog
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.UpdateStatus
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.WrappedFlow
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.DefaultConsumable
import net.flipper.core.busylib.ktx.common.exponentialRetry
import net.flipper.core.busylib.ktx.common.merge
import net.flipper.core.busylib.ktx.common.orEmpty
import net.flipper.core.busylib.ktx.common.throttleLatest
import net.flipper.core.busylib.ktx.common.tryConsume
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error

@Suppress("UnusedPrivateProperty")
class FFirmwareUpdateFeatureApiImpl(
    private val rpcFeatureApi: FRpcFeatureApi,
    private val fEventsFeatureApi: FEventsFeatureApi?,
) : FFirmwareUpdateFeatureApi, LogTagProvider {
    override val TAG: String = "FFirmwareUpdateFeatureApi"

    override fun getUpdateStatusFlow(): WrappedFlow<UpdateStatus> {
        return fEventsFeatureApi
            ?.getUpdateFlow(UpdateEvent.UPDATER_UPDATE_STATUS)
            .orEmpty()
            .merge(flowOf(DefaultConsumable(false)))
            .throttleLatest { consumable ->
                val couldConsume = consumable.tryConsume()
                exponentialRetry {
                    rpcFeatureApi.fRpcUpdaterApi
                        .getUpdateStatus(couldConsume)
                        .onFailure { throwable -> error(throwable) { "Failed to get update status" } }
                }
            }
            .wrap()
    }

    override suspend fun startUpdateCheck(): CResult<Unit> {
        return rpcFeatureApi.fRpcUpdaterApi.startUpdateCheck()
            .onFailure { throwable -> error(throwable) { "#startUpdateCheck could not start update check" } }
            .map { }
            .toCResult()
    }

    override suspend fun startVersionInstall(version: String): CResult<Unit> {
        return rpcFeatureApi.fRpcUpdaterApi.startUpdateInstall(version).map { }.toCResult()
    }

    override suspend fun stopFirmwareUpdate(): CResult<Unit> {
        return rpcFeatureApi.fRpcUpdaterApi.startUpdateAbortDownload().map { }.toCResult()
    }

    override suspend fun getVersionChangelog(version: String): CResult<BsbVersionChangelog> {
        return rpcFeatureApi.fRpcUpdaterApi
            .getUpdateChangelog(version)
            .map { changelog ->
                BsbVersionChangelog(
                    version = version,
                    changelog = changelog.changelog
                )
            }.toCResult()
    }
}
