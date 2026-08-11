package net.flipper.bridge.connection.feature.drawtool.impl

import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import net.flipper.bridge.connection.feature.drawtool.api.exception.DrawToolLowPriorityException
import net.flipper.bridge.connection.feature.drawtool.api.model.DrawToolDisplaySide
import net.flipper.bridge.connection.feature.rpc.api.exception.DrawLowPriorityException
import net.flipper.busylib.core.wrapper.CResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FDrawToolFeatureApiImplTest {

    private fun lowPriorityAssetsApi() = FakeFRpcAssetsApi(
        displayDrawResult = Result.failure(DrawLowPriorityException())
    )

    @Test
    fun GIVEN_low_priority_draw_WHEN_showFile_THEN_fails_with_DrawToolLowPriorityException() =
        runTest {
            val api = FDrawToolFeatureApiImpl(lowPriorityAssetsApi())

            val result = api.showFile(Path("image.png"), DrawToolDisplaySide.FRONT)

            assertIs<DrawToolLowPriorityException>(result.exceptionOrNull())
        }

    @Test
    fun GIVEN_low_priority_draw_WHEN_showPreview_THEN_fails_with_DrawToolLowPriorityException() =
        runTest {
            val api = FDrawToolFeatureApiImpl(lowPriorityAssetsApi())

            val result = api.showPreview(ByteArray(1), DrawToolDisplaySide.BACK)

            assertIs<DrawToolLowPriorityException>(result.exceptionOrNull())
        }

    @Test
    fun GIVEN_unrelated_draw_failure_WHEN_showFile_THEN_original_error_is_preserved() = runTest {
        val cause = IllegalStateException("Nothing to display")
        val api = FDrawToolFeatureApiImpl(
            FakeFRpcAssetsApi(displayDrawResult = Result.failure(cause))
        )

        val result = api.showFile(Path("image.png"), DrawToolDisplaySide.FRONT)

        assertEquals(cause, result.exceptionOrNull())
    }

    @Test
    fun GIVEN_upload_failure_WHEN_showPreview_THEN_draw_is_not_attempted() = runTest {
        val cause = IllegalStateException("upload failed")
        val assetsApi = FakeFRpcAssetsApi(uploadResult = Result.failure(cause))
        val api = FDrawToolFeatureApiImpl(assetsApi)

        val result = api.showPreview(ByteArray(1), DrawToolDisplaySide.FRONT)

        assertEquals(cause, result.exceptionOrNull())
        assertTrue(assetsApi.displayDrawRequests.isEmpty())
    }

    @Test
    fun GIVEN_successful_draw_WHEN_showFile_THEN_succeeds() = runTest {
        val api = FDrawToolFeatureApiImpl(FakeFRpcAssetsApi())

        val result = api.showFile(Path("image.png"), DrawToolDisplaySide.FRONT)

        assertIs<CResult.Success<Unit>>(result)
    }

    @Test
    fun GIVEN_successful_draw_WHEN_showPreview_THEN_succeeds() = runTest {
        val api = FDrawToolFeatureApiImpl(FakeFRpcAssetsApi())

        val result = api.showPreview(ByteArray(1), DrawToolDisplaySide.BACK)

        assertIs<CResult.Success<Unit>>(result)
    }

    @Test
    fun GIVEN_low_priority_draw_WHEN_hidePreview_THEN_is_unaffected() = runTest {
        // hidePreview never draws, so it can never be rejected on priority.
        val api = FDrawToolFeatureApiImpl(lowPriorityAssetsApi())

        val result = api.hidePreview()

        assertIs<CResult.Success<Unit>>(result)
    }
}
