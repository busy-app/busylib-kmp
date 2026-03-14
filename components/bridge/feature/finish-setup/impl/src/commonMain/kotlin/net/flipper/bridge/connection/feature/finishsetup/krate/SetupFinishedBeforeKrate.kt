package net.flipper.bridge.connection.feature.finishsetup.krate

import com.russhwolf.settings.Settings
import me.tatarka.inject.annotations.Inject
import net.flipper.busylib.core.di.BusyLibGraph
import ru.astrainteractive.klibs.kstorage.suspend.StateFlowSuspendMutableKrate
import ru.astrainteractive.klibs.kstorage.suspend.impl.DefaultStateFlowSuspendMutableKrate
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

private const val KEY = "setup_was_finished_before"

/**
 * Here set true in factory because we don't want to display it when
 * it's in loading state
 */
interface SetupFinishedBeforeKrate : StateFlowSuspendMutableKrate<Boolean>

@Inject
@ContributesBinding(BusyLibGraph::class, SetupFinishedBeforeKrate::class)
class SetupFinishedBeforeKrateImpl(
    private val settings: Settings
) : SetupFinishedBeforeKrate,
    StateFlowSuspendMutableKrate<Boolean> by DefaultStateFlowSuspendMutableKrate(
        factory = { true },
        loader = { settings.getBoolean(KEY, false) },
        saver = { settings.putBoolean(KEY, it) }
    )
