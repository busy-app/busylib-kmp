package net.flipper.bridge.connection.feature.drawtool.impl

import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.files.Path
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureKey
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.drawtool.api.FDrawToolFeatureApi
import net.flipper.bridge.connection.feature.drawtool.api.exception.DrawToolLowPriorityException
import net.flipper.bridge.connection.feature.drawtool.api.model.DrawToolDisplaySide
import net.flipper.bridge.connection.feature.rpc.api.exception.DrawLowPriorityException
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcAssetsApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.model.DrawRequest
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.core.busylib.log.LogTagProvider

class FDrawToolFeatureApiImpl(
    private val assetsApi: FRpcAssetsApi,
) : FDrawToolFeatureApi, LogTagProvider {
    override val TAG: String = "FDrawToolFeatureApi"

    private val mutex = Mutex()

    private fun DrawToolDisplaySide.toRpcDisplay(): DrawRequest.Display {
        return when (this) {
            DrawToolDisplaySide.FRONT -> DrawRequest.Display.FRONT
            DrawToolDisplaySide.BACK -> DrawRequest.Display.BACK
        }
    }

    private fun buildDrawRequest(
        fileName: String,
        displaySide: DrawToolDisplaySide
    ): DrawRequest {
        return DrawRequest(
            appId = APP_ID,
            priority = DRAW_PRIORITY,
            elements = listOf(
                DrawRequest.Element(
                    id = APP_ID,
                    timeoutSec = NO_TIMEOUT_SEC,
                    type = DrawRequest.Element.ElementType.IMAGE,
                    path = fileName,
                    x = ORIGIN,
                    y = ORIGIN,
                    align = DrawRequest.Element.Alignment.TOP_LEFT,
                    display = displaySide.toRpcDisplay()
                )
            )
        )
    }

    private suspend fun drawFile(
        fileName: String,
        displaySide: DrawToolDisplaySide
    ): CResult<Unit> {
        return assetsApi
            .displayDraw(buildDrawRequest(fileName, displaySide))
            .fold(
                onSuccess = { CResult.success(Unit) },
                onFailure = { error ->
                    if (error is DrawLowPriorityException) {
                        CResult.failure(DrawToolLowPriorityException())
                    } else {
                        CResult.failure(error)
                    }
                }
            )
    }

    override suspend fun showPreview(
        image: ByteArray,
        displaySide: DrawToolDisplaySide
    ): CResult<Unit> = mutex.withLock {
        assetsApi
            .uploadAsset(appId = APP_ID, file = PREVIEW_FILE_NAME, content = image)
            .fold(
                onSuccess = { _ -> drawFile(PREVIEW_FILE_NAME, displaySide) },
                onFailure = { error -> CResult.failure(error) }
            )
    }

    override suspend fun showFile(
        path: Path,
        displaySide: DrawToolDisplaySide
    ): CResult<Unit> = mutex.withLock {
        drawFile(path.name, displaySide)
    }

    override suspend fun hidePreview(): CResult<Unit> = mutex.withLock {
        assetsApi
            .removeDraw(APP_ID)
            .map { }
            .toCResult()
    }

    @Inject
    @ContributesIntoMap(BusyLibGraph::class, binding<FDeviceFeatureApi.Factory>())
    @FDeviceFeatureKey(FDeviceFeature.DRAW_TOOL)
    class Factory : FDeviceFeatureApi.Factory {
        override suspend fun invoke(
            unsafeFeatureDeviceApi: FUnsafeDeviceFeatureApi,
            scope: CoroutineScope,
            connectedDevice: FConnectedDeviceApi
        ): FDeviceFeatureApi? {
            val fRpcFeatureApi = unsafeFeatureDeviceApi
                .get(FRpcFeatureApi::class)
                ?.await()
                ?: return null

            return FDrawToolFeatureApiImpl(
                assetsApi = fRpcFeatureApi.fRpcAssetsApi,
            )
        }
    }

    companion object {
        /**
         * Overlaps built-in bar screens (priority 10) but is rejected
         * during an active work session (priority 90).
         */
        private const val DRAW_PRIORITY = 40

        /**
         * Application name of every draw and the stable id of its single
         * element, so a repeated request replaces the drawing in place.
         */
        private const val APP_ID = "draw_tool"

        /** The preview slot in the bar assets, overwritten on every show. */
        private const val PREVIEW_FILE_NAME = "temp.png"

        private const val NO_TIMEOUT_SEC = 0
        private const val ORIGIN = 0
    }
}
