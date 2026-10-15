package net.flipper.tools.debug.impl.impl

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.error
import net.flipper.tools.debug.api.api.DebugToolsApi
import net.flipper.tools.debug.api.api.LocalStorageResetter

@Inject
@ContributesBinding(BusyLibGraph::class, binding<DebugToolsApi>())
class DebugToolsApiImpl(
    private val resetters: Set<LocalStorageResetter>
) : DebugToolsApi, LogTagProvider {
    override val TAG = "DebugToolsApi"

    override suspend fun resetLocalStorage(): CResult<Unit> {
        val failures = resetters.mapNotNull { resetter ->
            runSuspendCatching { resetter.reset() }
                .onFailure { throwable ->
                    error(throwable) { "#resetLocalStorage ${resetter::class.simpleName} failed" }
                }
                .exceptionOrNull()
        }
        val firstFailure = failures.firstOrNull() ?: return CResult.success(Unit)
        return CResult.failure(firstFailure)
    }
}
