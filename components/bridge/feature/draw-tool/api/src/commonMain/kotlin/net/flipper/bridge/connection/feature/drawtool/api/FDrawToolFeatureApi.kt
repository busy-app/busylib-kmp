package net.flipper.bridge.connection.feature.drawtool.api

import net.flipper.bridge.connection.feature.common.api.FDeviceFeatureApi
import net.flipper.bridge.connection.feature.drawtool.api.model.DrawToolDisplaySide
import net.flipper.busylib.core.wrapper.CResult

/**
 * Draw tool feature: shows client-rendered images on the BUSY Bar display.
 *
 * The bar has no API to push raw pixels, so showing is always two-phased:
 * the image is first uploaded to the bar storage and then a draw command
 * referencing the uploaded file is sent. This API encapsulates both phases.
 */
interface FDrawToolFeatureApi : FDeviceFeatureApi {
    /**
     * Uploads [image] to the bar and shows it on the given [displaySide].
     *
     * [image] must be a PNG sized exactly as the bar screen matrix (72x16).
     * Repeated calls overwrite the previous preview.
     *
     * Fails when a work session is active on the bar: the session screen has
     * higher draw priority and the bar rejects the draw command.
     */
    suspend fun showPreview(image: ByteArray, displaySide: DrawToolDisplaySide): CResult<Unit>

    /**
     * Removes the preview from the bar display and cleans up the uploaded file.
     *
     * Idempotent: safe to call when nothing is shown.
     */
    suspend fun hidePreview(): CResult<Unit>
}
