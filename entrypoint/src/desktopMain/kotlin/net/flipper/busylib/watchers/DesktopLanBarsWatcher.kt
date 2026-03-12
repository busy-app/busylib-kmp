package net.flipper.busylib.watchers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.api.FDevicePersistedStorage
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.watchers.hook.DesktopActiveDevice
import net.flipper.busylib.watchers.hook.DesktopAlwaysLan
import net.flipper.busylib.watchers.hook.DesktopEmptyFiller
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(BusyLibGraph::class, InternalBUSYLibStartupListener::class, multibinding = true)
class DesktopLanBarsWatcher(
    scope: CoroutineScope,
    private val persistedStorage: FDevicePersistedStorage
) : InternalBUSYLibStartupListener {
    private val singleJobScope = scope.asSingleJobScope()

    override fun onLaunch() {
        singleJobScope.launch(SingleJobMode.SKIP_IF_RUNNING) {
            persistedStorage.addHook(DesktopEmptyFiller())
            persistedStorage.addHook(DesktopAlwaysLan())
            persistedStorage.addHook(DesktopActiveDevice())
            persistedStorage.transaction { } // Activate all hooks
        }
    }
}
