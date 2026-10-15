@file:Suppress("FunctionNaming")

package net.flipper.bridge.connection.feature.finishsetup.krate

import kotlinx.coroutines.test.runTest
import ru.astrainteractive.klibs.kstorage.suspend.StateFlowSuspendMutableKrate
import ru.astrainteractive.klibs.kstorage.suspend.impl.DefaultStateFlowSuspendMutableKrate
import kotlin.test.Test
import kotlin.test.assertFalse

class SetupFinishedBeforeKrateResetterTest {
    @Test
    fun GIVEN_setup_finished_before_WHEN_reset_THEN_setup_is_not_finished_in_cache_and_storage() = runTest {
        var storedValue = true
        // Same factory as the real krate: its reset() would bring back `true`
        val delegate = DefaultStateFlowSuspendMutableKrate(
            factory = { true },
            loader = { storedValue },
            saver = { newValue -> storedValue = newValue }
        )
        val krate = object : SetupFinishedBeforeKrate, StateFlowSuspendMutableKrate<Boolean> by delegate {}

        SetupFinishedBeforeKrateResetter(krate).reset()

        assertFalse(krate.cachedStateFlow.value)
        assertFalse(krate.getValue())
        assertFalse(storedValue)
    }
}
