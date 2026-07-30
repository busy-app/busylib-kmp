package net.flipper.bridge.connection.feature.drawtool.api

import kotlinx.io.files.Path
import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.drawtool.api.model.DrawToolDisplaySide
import net.flipper.busylib.core.wrapper.CResult

/**
 * Draw tool feature: shows client-rendered images on the BUSY Bar display.
 *
 * The bar has no API to push raw pixels — it draws a file of its own storage.
 */
interface FDrawToolFeatureApi : FDeviceFeatureApi {
    /**
     * Shows [path], already on the bar, on [displaySide]. Transfers nothing.
     *
     * [path] must be a file directly inside `/ext/user_assets/draw_tool/`.
     * The drawing is removed by [hidePreview]; the file is left alone.
     */
    suspend fun showFile(path: Path, displaySide: DrawToolDisplaySide): CResult<Unit>

    /**
     * Removes what this feature draws from the bar display.
     *
     * Idempotent: safe to call when nothing is shown.
     */
    suspend fun hidePreview(): CResult<Unit>
}
