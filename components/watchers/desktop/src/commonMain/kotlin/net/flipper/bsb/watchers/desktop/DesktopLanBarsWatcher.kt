package net.flipper.bsb.watchers.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject
import net.flipper.bridge.connection.config.internal.FInternalDevicePersistedStorage
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.bsb.watchers.desktop.hook.DesktopAlwaysLan
import net.flipper.bsb.watchers.desktop.hook.DesktopAutoPurger
import net.flipper.bsb.watchers.desktop.hook.DesktopEmptyFiller
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.core.busylib.ktx.common.SingleJobMode
import net.flipper.core.busylib.ktx.common.asSingleJobScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(BusyLibGraph::class, InternalBUSYLibStartupListener::class, multibinding = true)
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
                DesktopAutoPurger()
            )
            persistedStorage.transaction { } // Activate all hooks
        }
    }
}
