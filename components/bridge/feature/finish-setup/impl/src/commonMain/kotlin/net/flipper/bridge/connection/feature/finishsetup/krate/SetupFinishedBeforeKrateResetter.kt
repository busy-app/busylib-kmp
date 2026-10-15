package net.flipper.bridge.connection.feature.finishsetup.krate

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.tools.debug.api.api.LocalStorageResetter

@Inject
@ContributesIntoSet(BusyLibGraph::class, binding<LocalStorageResetter>())
class SetupFinishedBeforeKrateResetter(
    private val krate: SetupFinishedBeforeKrate
) : LocalStorageResetter {
    override suspend fun reset() {
        // krate.reset() would persist the factory value (true), which only masks the loading state
        krate.save(false)
    }
}
