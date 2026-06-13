package net.flipper.bsb.watchers.desktop

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.flipper.bridge.connection.config.internal.FInternalDevicePersistedStorage
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.bsb.watchers.desktop.hook.DesktopAlwaysLan
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope

@Inject
@ContributesIntoSet(BusyLibGraph::class, binding = binding<InternalBUSYLibStartupListener>())
class DesktopLanBarsWatcher(
    scope: CoroutineScope,
    private val persistedStorage: FInternalDevicePersistedStorage
) : InternalBUSYLibStartupListener {
    private val singleJobScope = scope.asSingleJobScope()

    override fun onLaunch() {
        singleJobScope.launch(SingleJobMode.SKIP_IF_RUNNING) {
            persistedStorage.addHook(
                DesktopAlwaysLan(),
            )
            persistedStorage.transaction { } // Activate all hooks
        }
    }
}
