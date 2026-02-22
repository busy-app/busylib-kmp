package net.flipper.bridge.connection.feature.finishsetup.krate

import com.russhwolf.settings.Settings
import me.tatarka.inject.annotations.Inject
import ru.astrainteractive.klibs.kstorage.suspend.StateFlowSuspendMutableKrate
import ru.astrainteractive.klibs.kstorage.suspend.impl.DefaultStateFlowSuspendMutableKrate

private const val KEY = "setup_was_finished_before"

@Inject
class SetupFinishedBeforeKrate(
    private val settings: Settings
) : StateFlowSuspendMutableKrate<Boolean> by DefaultStateFlowSuspendMutableKrate(
    factory = { true },
    loader = { settings.getBoolean(KEY, false) },
    saver = { settings.putBoolean(KEY, it) }
)
