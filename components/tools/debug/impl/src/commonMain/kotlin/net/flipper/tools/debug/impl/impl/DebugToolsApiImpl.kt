package net.flipper.tools.debug.impl.impl

import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import net.flipper.bridge.connection.feature.finishsetup.krate.SetupFinishedBeforeKrate
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.core.wrapper.CResult
import net.flipper.busylib.core.wrapper.toCResult
import net.flipper.core.busylib.ktx.common.runSuspendCatching
import net.flipper.tools.debug.api.api.DebugToolsApi

@Inject
@ContributesBinding(BusyLibGraph::class, binding<DebugToolsApi>())
class DebugToolsApiImpl(
    private val setupFinishedBeforeKrate: SetupFinishedBeforeKrate
) : DebugToolsApi {
    override suspend fun resetSetupFinishedBefore(): CResult<Unit> {
        return runSuspendCatching { setupFinishedBeforeKrate.save(false) }.toCResult()
    }
}
