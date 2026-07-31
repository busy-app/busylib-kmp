package net.flipper.tools.drawtool.sync.api

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.busylib.core.wrapper.wrap
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.core.busylib.ktx.common.transform
import net.flipper.tools.drawtool.api.DrawToolSyncApi
import net.flipper.tools.drawtool.api.model.DrawToolSyncState
import net.flipper.tools.drawtool.layout.api.DefaultDrawToolStatusDirectoryLayout
import net.flipper.tools.drawtool.storage.api.DrawToolStoragePathProvider
import net.flipper.tools.drawtool.sync.execute.DrawToolSyncPlanExecutor
import net.flipper.tools.drawtool.sync.execute.DrawToolSyncTemporaryCleaner
import net.flipper.tools.drawtool.sync.model.DrawToolSyncTarget
import net.flipper.tools.drawtool.sync.plan.DrawToolSyncPlanFactory
import net.flipper.tools.drawtool.sync.plan.DrawToolSyncTargetResolver

@SingleIn(BusyLibGraph::class)
@Inject
@ContributesBinding(BusyLibGraph::class, binding<DrawToolSyncApi>())
class DefaultDrawToolSyncApi(
    private val drawToolStoragePathProvider: DrawToolStoragePathProvider,
    private val targetResolver: DrawToolSyncTargetResolver,
    private val temporaryCleaner: DrawToolSyncTemporaryCleaner,
    private val planFactory: DrawToolSyncPlanFactory,
    private val planExecutor: DrawToolSyncPlanExecutor,
) : DrawToolSyncApi {
    private val mutex = Mutex()
    private val _state = MutableStateFlow<DrawToolSyncState>(DrawToolSyncState.Idle)

    override val state = _state.asStateFlow().wrap()

    private suspend fun syncWith(target: DrawToolSyncTarget): Result<Unit> {
        return drawToolStoragePathProvider.getPath()
            .map(::DefaultDrawToolStatusDirectoryLayout)
            .transform { localLayout ->
                temporaryCleaner.cleanup(target, localLayout)
                planFactory.create(target, localLayout).transform { plan ->
                    planExecutor.execute(plan, target, localLayout)
                }
            }
    }

    private suspend fun syncInternal(): Result<Unit> {
        return runSuspendCatching { targetResolver.resolve() }
            .transform(::syncWith)
    }

    override suspend fun sync(): CResult<Unit> {
        return mutex.withLock {
            _state.value = DrawToolSyncState.InProgress
            try {
                val result = syncInternal()
                _state.value = result.fold(
                    onSuccess = { _ -> DrawToolSyncState.Idle },
                    onFailure = DrawToolSyncState::Failed
                )
                result.toCResult()
            } finally {
                // Cancellation is the one way out that bypasses the states
                // above; a pass that is gone must not look like a running one.
                if (_state.value == DrawToolSyncState.InProgress) {
                    _state.value = DrawToolSyncState.Idle
                }
            }
        }
    }
}
