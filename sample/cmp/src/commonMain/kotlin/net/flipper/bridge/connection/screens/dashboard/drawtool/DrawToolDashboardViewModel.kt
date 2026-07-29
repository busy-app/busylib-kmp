package net.flipper.bridge.connection.screens.dashboard.drawtool

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bridge.connection.feature.drawtool.api.FDrawToolFeatureApi
import net.flipper.bridge.connection.feature.drawtool.api.model.DrawToolDisplaySide
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.screens.dashboard.common.DashboardFeatureViewModel
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info
import kotlin.time.Clock

private const val GENERATED_FRAME_COUNT = 2

class DrawToolDashboardViewModel(
    private val featureProvider: FFeatureProvider,
    private val persistedStorage: FDevicePersistedStorage,
    private val collectionSourceResolver: DrawToolCollectionSourceResolver,
    private val statusWriter: DrawToolSampleStatusWriter
) : DashboardFeatureViewModel(), LogTagProvider {
    override val TAG = "DrawToolDashboard"

    private val mutableState = MutableStateFlow(DrawToolDashboardState())
    val state: StateFlow<DrawToolDashboardState> = mutableState

    /** Sends [message] both to the on-screen log and to the console. */
    private fun report(message: String) {
        info { message }
        appendLog(message)
    }

    private suspend fun requireCurrentDeviceUniqueId(): String {
        val currentDevice = persistedStorage.getCurrentDeviceFlow().firstOrNull()
        return requireNotNull(currentDevice) { "No BUSY Bar is selected" }.uniqueId
    }

    private fun showPreview(displaySide: DrawToolDisplaySide) = runAction("draw tool show preview") {
        val drawToolFeatureApi = requireFeature<FDrawToolFeatureApi>(featureProvider, "DrawTool")
        drawToolFeatureApi
            .showPreview(DrawToolSampleImage.bytes(), displaySide)
            .getOrThrow()
        val summary = "Preview shown on ${displaySide.name}"
        mutableState.value = mutableState.value.copy(lastPreviewSummary = summary)
        appendLog(summary)
    }

    fun showPreviewOnFront() = showPreview(DrawToolDisplaySide.FRONT)

    fun showPreviewOnBack() = showPreview(DrawToolDisplaySide.BACK)

    fun hidePreview() = runAction("draw tool hide preview") {
        val drawToolFeatureApi = requireFeature<FDrawToolFeatureApi>(featureProvider, "DrawTool")
        drawToolFeatureApi.hidePreview().getOrThrow()
        mutableState.value = mutableState.value.copy(lastPreviewSummary = "Preview hidden")
        appendLog("Preview hidden")
    }

    fun generateStatus(target: DrawToolStorageTarget) =
        runAction("draw tool generate status on ${target.title}") {
            val uniqueId = requireCurrentDeviceUniqueId()
            val source = collectionSourceResolver.resolve(target, uniqueId)
            val statusId = statusWriter.write(
                source = source,
                frameCount = GENERATED_FRAME_COUNT,
                frameContent = DrawToolSampleImage.bytes(),
                updatedAt = Clock.System.now()
            )
            report("Generated status $statusId in ${source.collectionPath}")
        }

    fun readStatuses(target: DrawToolStorageTarget) =
        runAction("draw tool read statuses from ${target.title}") {
            val uniqueId = requireCurrentDeviceUniqueId()
            val source = collectionSourceResolver.resolve(target, uniqueId)
            val statuses = source.statusesApi.getStatuses(uniqueId).getOrThrow()
            report(
                formatDrawToolStatuses(
                    target = target,
                    collectionPath = source.collectionPath,
                    statuses = statuses
                )
            )
        }
}

data class DrawToolDashboardState(
    val lastPreviewSummary: String? = null
)
