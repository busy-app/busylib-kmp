package net.flipper.tools.drawtool.sync.plan

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.getSync
import net.flipper.bridge.connection.feature.storage.api.FStorageFeatureApi
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.tools.drawtool.api.model.DrawToolSyncException
import net.flipper.tools.drawtool.layout.api.DrawToolStatusDirectoryLayoutFactory
import net.flipper.tools.drawtool.sync.model.DrawToolSyncTarget

@Inject
class DrawToolSyncTargetResolver(
    private val orchestrator: FDeviceOrchestrator,
    private val featureProvider: FFeatureProvider,
    private val layoutFactory: DrawToolStatusDirectoryLayoutFactory,
) {
    private suspend fun getConnectedStatusUnsafe(): FDeviceConnectStatus.Connected {
        return orchestrator.getState()
            .first()
            .tryCast<FDeviceConnectStatus.Connected>()
            ?: throw DrawToolSyncException.BarNotConnected()
    }

    private fun getSerialNumberUnsafe(status: FDeviceConnectStatus.Connected): String {
        return status.device
            .serialNumber
            ?: throw DrawToolSyncException.SerialNumberUnknown()
    }

    private suspend fun getBarFileSystemUnsafe(): FStorageFeatureApi {
        return featureProvider.getSync<FStorageFeatureApi>()
            ?: throw DrawToolSyncException.BarNotConnected()
    }

    /**
     * The storage feature always belongs to the bar connected *now*, while the
     * serial number was read earlier. Re-reading the status proves the
     * connection never changed in between, so both describe one bar and the
     * sync memory is never written under a foreign serial.
     */
    private suspend fun requireSameConnectionUnsafe(status: FDeviceConnectStatus.Connected) {
        if (getConnectedStatusUnsafe().deviceApi !== status.deviceApi) {
            throw DrawToolSyncException.BarNotConnected()
        }
    }

    /**
     * @throws DrawToolSyncException.BarNotConnected without a connected bar or its
     * storage, or when the connected bar changed while resolving
     * @throws DrawToolSyncException.SerialNumberUnknown when the bar has no serial number yet
     */
    suspend fun resolveUnsafe(): DrawToolSyncTarget {
        val status = getConnectedStatusUnsafe()
        val serialNumber = getSerialNumberUnsafe(status)
        val barFileSystem = getBarFileSystemUnsafe()
        requireSameConnectionUnsafe(status)
        return DrawToolSyncTarget(
            serialNumber = serialNumber,
            barFileSystem = barFileSystem,
            barLayout = layoutFactory.createBarLayout(),
        )
    }
}
