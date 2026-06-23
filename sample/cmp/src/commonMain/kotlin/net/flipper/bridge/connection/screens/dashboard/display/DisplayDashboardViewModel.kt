package net.flipper.bridge.connection.screens.dashboard.display

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.flipper.bridge.connection.feature.provider.api.FFeatureProvider
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.DrawRequest
import net.flipper.bridge.connection.screens.dashboard.common.DISPLAY_UNTIL_OFFSET_SECONDS
import net.flipper.bridge.connection.screens.dashboard.common.DashboardFeatureViewModel
import net.flipper.bridge.connection.screens.dashboard.common.MILLIS_PER_SECOND
import net.flipper.bridge.connection.screens.dashboard.common.SAMPLE_ANIMATION_ID
import net.flipper.bridge.connection.screens.dashboard.common.SAMPLE_APP_ID
import net.flipper.bridge.connection.screens.dashboard.common.SAMPLE_DRAW_ID
import net.flipper.bridge.connection.screens.dashboard.common.SAMPLE_DRAW_PRIORITY
import net.flipper.bridge.connection.screens.dashboard.common.SAMPLE_STOCK_ANIMATION_PATH
import kotlin.time.Clock

class DisplayDashboardViewModel(
    private val featureProvider: FFeatureProvider
) : DashboardFeatureViewModel() {
    private val mutableState = MutableStateFlow(DisplayDashboardState())
    val state: StateFlow<DisplayDashboardState> = mutableState

    fun drawSampleText() = runAction("display draw text") {
        val rpcFeatureApi = requireFeature<FRpcFeatureApi>(featureProvider, "RPC")
        rpcFeatureApi.fRpcAssetsApi.displayDraw(createTextDrawRequest()).getOrThrow()
        mutableState.value = mutableState.value.copy(lastDrawSummary = "Text draw posted")
        appendLog("Text draw posted")
    }

    fun drawSampleAnimation() = runAction("display draw animation") {
        val rpcFeatureApi = requireFeature<FRpcFeatureApi>(featureProvider, "RPC")
        rpcFeatureApi.fRpcAssetsApi.displayDraw(createAnimationDrawRequest()).getOrThrow()
        mutableState.value = mutableState.value.copy(
            lastDrawSummary = "Animation draw posted: $SAMPLE_STOCK_ANIMATION_PATH"
        )
        appendLog("Animation draw posted")
    }

    fun clearSampleDraw() = runAction("display draw delete") {
        val rpcFeatureApi = requireFeature<FRpcFeatureApi>(featureProvider, "RPC")
        rpcFeatureApi.fRpcAssetsApi.removeDraw(SAMPLE_APP_ID).getOrThrow()
        mutableState.value = mutableState.value.copy(lastDrawSummary = "Draw cleared")
        appendLog("Draw cleared")
    }

    private fun createTextDrawRequest(): DrawRequest {
        val displayUntil = (
            (Clock.System.now().toEpochMilliseconds() / MILLIS_PER_SECOND) +
                DISPLAY_UNTIL_OFFSET_SECONDS
            ).toString()
        return DrawRequest(
            appId = SAMPLE_APP_ID,
            priority = SAMPLE_DRAW_PRIORITY,
            elements = listOf(
                DrawRequest.Element(
                    id = SAMPLE_DRAW_ID,
                    type = DrawRequest.Element.ElementType.TEXT,
                    text = "RPC contract ok",
                    x = 8,
                    y = 4,
                    font = DrawRequest.Element.Font.MEDIUM,
                    color = "#FFFFFFFF",
                    displayUntil = displayUntil,
                    timeoutSec = null
                )
            )
        )
    }

    private fun createAnimationDrawRequest(): DrawRequest {
        return DrawRequest(
            appId = SAMPLE_APP_ID,
            priority = SAMPLE_DRAW_PRIORITY,
            elements = listOf(
                DrawRequest.Element(
                    id = SAMPLE_ANIMATION_ID,
                    type = DrawRequest.Element.ElementType.ANIMATION,
                    stockPath = SAMPLE_STOCK_ANIMATION_PATH,
                    loop = true,
                    awaitPreviousEnd = false,
                    timeoutSec = null
                )
            )
        )
    }
}

data class DisplayDashboardState(
    val lastDrawSummary: String? = null
)
