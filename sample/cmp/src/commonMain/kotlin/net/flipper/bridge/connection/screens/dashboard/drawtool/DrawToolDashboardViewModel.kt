package net.flipper.bridge.connection.screens.dashboard.drawtool

import net.flipper.bridge.connection.feature.drawtool.api.FDrawToolFeatureApi
import net.flipper.bridge.connection.feature.drawtool.api.model.DrawToolDisplaySide
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.screens.dashboard.common.DashboardFeatureViewModel
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import net.flipper.tools.drawtool.api.DrawToolStatusesApi
import net.flipper.tools.drawtool.api.model.DrawToolStoredFile

class DrawToolDashboardViewModel(
    private val featureProvider: FFeatureProvider,
    private val clientStatusesApi: DrawToolStatusesApi,
    private val collectionSourceResolver: DrawToolCollectionSourceResolver,
    private val statusWriter: DrawToolSampleStatusWriter
) : DashboardFeatureViewModel(), LogTagProvider {
    override val TAG = "DrawToolDashboard"

    /** Sends [message] both to the on-screen log and to the console. */
    private fun report(message: String) {
        info { message }
        appendLog(message)
    }

    private suspend fun requireLatestClientStatus(): DrawToolStoredFile.Status {
        val files = clientStatusesApi.getDrawToolDirectoryContents().getOrThrow().files
        val statuses = files.filterIsInstance<DrawToolStoredFile.Status>()
        return requireNotNull(statuses.firstOrNull()) {
            "The client collection holds no status, generate one first"
        }
    }

    fun uploadLatestStatus() = runAction("draw tool upload status") {
        val status = requireLatestClientStatus()
        clientStatusesApi.uploadStatus(status).getOrThrow()
        report("Uploaded ${status.path.name} onto the bar")
    }

    fun showPreview(displaySide: DrawToolDisplaySide) = runAction("draw tool show preview") {
        requireFeature<FDrawToolFeatureApi>(featureProvider, "DrawTool")
            .showPreview(DrawToolSampleImage.bytes(), displaySide)
            .getOrThrow()
        report("Preview shown on ${displaySide.name}")
    }

    fun showLatestStatus(displaySide: DrawToolDisplaySide) = runAction("draw tool show status") {
        val status = requireLatestClientStatus()
        clientStatusesApi.showStatus(status, displaySide).getOrThrow()
        report("${status.path.name} shown on ${displaySide.name}")
    }

    fun hidePreview() = runAction("draw tool hide preview") {
        requireFeature<FDrawToolFeatureApi>(featureProvider, "DrawTool")
            .hidePreview()
            .getOrThrow()
        report("Display cleared")
    }

    fun generateStatus(target: DrawToolStorageTarget) =
        runAction("draw tool generate status on ${target.title}") {
            val source = collectionSourceResolver.resolve(target)
            val statusFilePath = statusWriter.write(
                source = source,
                content = DrawToolSampleImage.bytes()
            )
            report("Generated status ${statusFilePath.name} in ${source.collectionPath}")
        }

    fun readStatuses(target: DrawToolStorageTarget) =
        runAction("draw tool read statuses from ${target.title}") {
            val source = collectionSourceResolver.resolve(target)
            val contents = source.statusesApi.getDrawToolDirectoryContents().getOrThrow()
            report(
                formatDrawToolStatuses(
                    target = target,
                    collectionPath = source.collectionPath,
                    contents = contents
                )
            )
        }

    fun deleteStatuses(target: DrawToolStorageTarget) {
        runAction("draw tool delete statuses from ${target.title}") {
            val source = collectionSourceResolver.resolve(target)
            val files = source.statusesApi.getDrawToolDirectoryContents().getOrThrow().files
            val statuses = files.filterIsInstance<DrawToolStoredFile.Status>()
            source.statusesApi.deleteStatuses(statuses).getOrThrow()
            report("Deleted ${statuses.size} status(es) from ${source.collectionPath}")
        }
    }
}
