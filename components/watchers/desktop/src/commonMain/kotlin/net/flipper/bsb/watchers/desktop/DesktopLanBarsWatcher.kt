package net.flipper.bsb.watchers.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import dev.zacsweers.metro.Inject
import net.flipper.bridge.connection.config.internal.FInternalDevicePersistedStorage
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.bsb.watchers.desktop.hook.DesktopActiveDevice
import net.flipper.bsb.watchers.desktop.hook.DesktopAlwaysLan
import net.flipper.bsb.watchers.desktop.hook.DesktopAutoPurger
import net.flipper.bsb.watchers.desktop.hook.DesktopEmptyFiller
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import dev.zacsweers.metro.ContributesIntoSet

@Inject
@ContributesIntoSet(BusyLibGraph::class)
class DesktopLanBarsWatcher(
    scope: CoroutineScope,
    private val persistedStorage: FInternalDevicePersistedStorage
) : InternalBUSYLibStartupListener {
    private val singleJobScope = scope.asSingleJobScope()

    override fun onLaunch() {
        singleJobScope.launch(SingleJobMode.SKIP_IF_RUNNING) {
            persistedStorage.addHook(
                DesktopEmptyFiller(),
                DesktopAlwaysLan(),
                DesktopActiveDevice(),
                DesktopAutoPurger()
            )
            persistedStorage.transaction { } // Activate all hooks
        }
    }
}
