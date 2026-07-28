package net.flipper.bridge.connection.feature.drawtool.impl

import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.flipper.bridge.connection.feature.common.api.FDeviceFeature
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureKey
import net.flipper.bridge.connection.feature.common.api.FUnsafeDeviceFeatureApi
import net.flipper.bridge.connection.feature.drawtool.api.FDrawToolFeatureApi
import net.flipper.bridge.connection.feature.drawtool.api.model.DrawToolDisplaySide
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcAssetsApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcFeatureApi
import net.flipper.bridge.connection.feature.rpc.api.exposed.FRpcStorageApi
import net.flipper.bridge.connection.feature.rpc.api.model.DrawRequest
import net.flipper.bridge.connection.transport.common.api.FConnectedDeviceApi
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.verbose

class FDrawToolFeatureApiImpl(
    private val storageApi: FRpcStorageApi,
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

    private fun buildPreviewDrawRequest(displaySide: DrawToolDisplaySide): DrawRequest {
        return DrawRequest(
            appId = APPLICATION_NAME,
            priority = DRAW_PRIORITY,
            elements = listOf(
                DrawRequest.Element(
                    id = ELEMENT_ID,
                    timeoutSec = NO_TIMEOUT_SEC,
                    type = DrawRequest.Element.ElementType.IMAGE,
                    path = PREVIEW_RELATIVE_PATH,
                    x = ORIGIN,
                    y = ORIGIN,
                    align = DrawRequest.Element.Alignment.TOP_LEFT,
                    display = displaySide.toRpcDisplay()
                )
            )
        )
    }

    private suspend fun ensurePreviewDirectoriesExist() {
        PREVIEW_DIRECTORY_PATHS.forEach { directoryPath ->
            storageApi
                .createDirectory(directoryPath)
                .onFailure { error ->
                    verbose { "Skip mkdir of $directoryPath, most likely it already exists: $error" }
                }
        }
    }

    private suspend fun sendPreviewDrawCommand(displaySide: DrawToolDisplaySide): CResult<Unit> {
        return assetsApi
            .displayDraw(buildPreviewDrawRequest(displaySide))
            .fold(
                onSuccess = { _ -> CResult.success(Unit) },
                onFailure = { error -> CResult.failure(error) }
            )
    }

    private suspend fun removePreviewFile() {
        storageApi
            .removeFile(PREVIEW_FILE_PATH)
            .onFailure { error ->
                verbose { "Skip preview file cleanup, most likely nothing was uploaded: $error" }
            }
    }

    override suspend fun showPreview(
        image: ByteArray,
        displaySide: DrawToolDisplaySide
    ): CResult<Unit> = mutex.withLock {
        ensurePreviewDirectoriesExist()
        storageApi
            .writeFile(PREVIEW_FILE_PATH, image)
            .fold(
                onSuccess = { _ -> sendPreviewDrawCommand(displaySide) },
                onFailure = { error -> CResult.failure(error) }
            )
    }

    override suspend fun hidePreview(): CResult<Unit> = mutex.withLock {
        assetsApi
            .removeDraw(APPLICATION_NAME)
            .fold(
                onSuccess = { _ ->
                    removePreviewFile()
                    CResult.success(Unit)
                },
                onFailure = { error -> CResult.failure(error) }
            )
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
                storageApi = fRpcFeatureApi.fRpcStorageApi,
                assetsApi = fRpcFeatureApi.fRpcAssetsApi,
            )
        }
    }

    companion object {
        /**
         * Must be at most 9 characters to fit the bar storage path limit.
         */
        private const val APPLICATION_NAME = "busy_draw"

        /**
         * Overlaps built-in bar screens (priority 10) but is rejected
         * during an active work session (priority 90).
         */
        private const val DRAW_PRIORITY = 40

        /**
         * Stable element id: a repeated draw request with the same
         * application name and id replaces the shown element in place.
         */
        private const val ELEMENT_ID = "draw_status"

        private const val NO_TIMEOUT_SEC = 0
        private const val ORIGIN = 0

        private const val PREVIEW_RELATIVE_PATH = "preview/current.png"
        private const val PREVIEW_FILE_PATH = "/ext/user_assets/busy_draw/preview/current.png"

        private val PREVIEW_DIRECTORY_PATHS = listOf(
            "/ext/user_assets",
            "/ext/user_assets/busy_draw",
            "/ext/user_assets/busy_draw/preview"
        )
    }
}
