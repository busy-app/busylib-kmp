package net.flipper.bridge.connection.feature.finishsetup.krate

import com.russhwolf.settings.Settings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import net.flipper.busylib.core.di.BusyLibGraph
import ru.astrainteractive.klibs.kstorage.suspend.StateFlowSuspendMutableKrate
import ru.astrainteractive.klibs.kstorage.suspend.impl.DefaultStateFlowSuspendMutableKrate

private const val KEY = "setup_was_finished_before"

/**
 * Here set true in factory because we don't want to display it when
 * it's in loading state
 */
interface SetupFinishedBeforeKrate : StateFlowSuspendMutableKrate<Boolean>

// Single instance: the debug reset has to hit the cache the finish-setup feature observes
@Inject
@SingleIn(BusyLibGraph::class)
@ContributesBinding(BusyLibGraph::class, binding<SetupFinishedBeforeKrate>())
class SetupFinishedBeforeKrateImpl(
    private val settings: Settings
) : SetupFinishedBeforeKrate,
    StateFlowSuspendMutableKrate<Boolean> by DefaultStateFlowSuspendMutableKrate(
        factory = { true },
        loader = { settings.getBoolean(KEY, false) },
        saver = { settings.putBoolean(KEY, it) }
    )
