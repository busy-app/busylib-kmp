package net.flipper.tools.drawtool.sync.execute

import dev.zacsweers.metro.Inject
import net.flipper.tools.drawtool.api.DrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.api.model.DrawToolSyncException
import net.flipper.tools.drawtool.sync.model.DrawToolSyncPlan
import net.flipper.tools.drawtool.sync.model.DrawToolSyncTarget
import net.flipper.tools.drawtool.sync.storage.DrawToolSyncStateRepository

/**
 * Applies a [DrawToolSyncPlan] step by step. Every step runs even when others
 * fail; any failure fails the whole pass at the end, so the next pass retries
 * what is left.
 */
@Inject
class DrawToolSyncPlanExecutor(
    private val localEraser: DrawToolLocalStatusEraser,
    private val barEraser: DrawToolBarStatusEraser,
    private val downloader: DrawToolStatusDownloader,
    private val uploader: DrawToolStatusUploader,
    private val stateRepository: DrawToolSyncStateRepository,
) {
    private fun toCompletionResult(stepResults: List<Result<Unit>>): Result<Unit> {
        val failures = stepResults.mapNotNull { stepResult -> stepResult.exceptionOrNull() }
        val firstFailure = failures.firstOrNull() ?: return Result.success(Unit)
        return Result.failure(
            DrawToolSyncException.PartiallyFailed(
                failedOperationsCount = failures.size,
                cause = firstFailure,
            )
        )
    }

    suspend fun execute(
        plan: DrawToolSyncPlan,
        target: DrawToolSyncTarget,
        localLayout: DrawToolStatusDirectoryLayout,
    ): Result<Unit> {
        if (plan.isBarReset) {
            stateRepository.forgetBar(target.serialNumber)
        }
        val stepResults = localEraser.eraseAll(localLayout, plan.deleteLocally) +
            barEraser.eraseAll(target, plan.deleteFromBar) +
            downloader.downloadAll(target, localLayout, plan.downloadFromBar) +
            uploader.uploadAll(target, localLayout, plan.uploadToBar)
        stateRepository.markSynced(target.serialNumber, plan.markInSync)
        return toCompletionResult(stepResults)
    }
}
