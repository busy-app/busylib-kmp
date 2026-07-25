package net.flipper.busylib

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import net.flipper.bsb.watchers.api.InternalBUSYLibStartupListener
import net.flipper.busylib.core.di.BusyLibGraph
import net.flipper.busylib.kmp.components.core.buildkonfig.BuildKonfig
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.info

@Inject
@ContributesIntoSet(BusyLibGraph::class, binding<InternalBUSYLibStartupListener>())
class BUSYLibVersionLogger : InternalBUSYLibStartupListener, LogTagProvider {
    override val TAG = "BUSYLib"

    override fun onLaunch() {
        info { "BUSYLib version: ${BuildKonfig.VERSION}" }
    }
}
