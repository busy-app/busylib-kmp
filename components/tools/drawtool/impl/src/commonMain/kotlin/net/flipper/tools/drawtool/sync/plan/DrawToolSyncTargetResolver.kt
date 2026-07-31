package net.flipper.tools.drawtool.sync.plan

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.provider.api.getSync
import net.flipper.bridge.connection.feature.storage.api.FStorageFeatureApi
import net.flipper.bridge.connection.orchestrator.api.FDeviceOrchestrator
import net.flipper.bridge.connection.orchestrator.api.model.FDeviceConnectStatus
import net.flipper.core.busylib.ktx.common.tryCast
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.api.model.DrawToolSyncException
import net.flipper.tools.drawtool.layout.api.DefaultDrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.sync.model.DrawToolSyncTarget

@Inject
class DrawToolSyncTargetResolver(
    private val orchestrator: FDeviceOrchestrator,
    private val featureProvider: FFeatureProvider,
) {
    private suspend fun getConnectedSerialNumberUnsafe(): String {
        val status = orchestrator.getState()
            .first()
            .tryCast<FDeviceConnectStatus.Connected>()
            ?: throw DrawToolSyncException.BarNotConnected()
        return status.device
            .serialNumber
            ?: throw DrawToolSyncException.SerialNumberUnknown()
    }

    /**
     * @throws DrawToolSyncException.BarNotConnected without a connected bar or its storage
     * @throws DrawToolSyncException.SerialNumberUnknown when the bar has no serial number yet
     */
    suspend fun resolve(): DrawToolSyncTarget {
        val serialNumber = getConnectedSerialNumberUnsafe()
        val barFileSystem = featureProvider.getSync<FStorageFeatureApi>()
            ?: throw DrawToolSyncException.BarNotConnected()
        return DrawToolSyncTarget(
            serialNumber = serialNumber,
            barFileSystem = barFileSystem,
            barLayout = DefaultDrawToolStatusDirectoryLayout(
                DrawToolStatusDirectoryLayout.BUSYBAR_DRAWTOOL_PATH
            ),
        )
    }
}
