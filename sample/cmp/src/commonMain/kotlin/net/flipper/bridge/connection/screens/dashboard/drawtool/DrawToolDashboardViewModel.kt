package net.flipper.bridge.connection.screens.dashboard.drawtool

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.flipper.bridge.connection.feature.drawtool.api.FDrawToolFeatureApi
import net.flipper.bridge.connection.feature.drawtool.api.model.DrawToolDisplaySide
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.screens.dashboard.common.DashboardFeatureViewModel

class DrawToolDashboardViewModel(
    private val featureProvider: FFeatureProvider
) : DashboardFeatureViewModel() {
    private val mutableState = MutableStateFlow(DrawToolDashboardState())
    val state: StateFlow<DrawToolDashboardState> = mutableState

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
}

data class DrawToolDashboardState(
    val lastPreviewSummary: String? = null
)
