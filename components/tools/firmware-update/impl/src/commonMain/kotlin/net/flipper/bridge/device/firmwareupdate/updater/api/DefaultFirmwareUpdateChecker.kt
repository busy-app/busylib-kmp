package net.flipper.bridge.device.firmwareupdate.updater.api

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.getSync
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.device.firmwareupdate.updater.model.FirmwareUpdateCheckError
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.toCResult

@Inject
@ContributesBinding(BusyLibGraph::class, binding<FirmwareUpdateChecker>())
class DefaultFirmwareUpdateChecker(
    private val featureProvider: FFeatureProvider
) : FirmwareUpdateChecker {
    override suspend fun checkForUpdates(): CResult<Unit> {
        return featureProvider.getSync<FRpcFeatureApi>()
            ?.fRpcUpdaterApi
            ?.startUpdateCheck()
            ?.toCResult()
            ?: CResult.failure(FirmwareUpdateCheckError.RpcUnavailable())
    }
}
