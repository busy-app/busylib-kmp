@file:Suppress("FunctionNaming")

package net.flipper.core.busylib.ktx.common

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class RetryKtxTest {

    @Test
    fun GIVEN_delay_WHEN_jitter_applied_THEN_result_is_between_half_and_full() {
        val random = Random(42)

        val jitteredDelays = List(1000) { _ -> 10.seconds.applyJitter(random) }

        jitteredDelays.forEach { jitteredDelay ->
            assertTrue(
                jitteredDelay >= 5.seconds && jitteredDelay < 10.seconds,
                "Jittered delay $jitteredDelay should be in [5s, 10s)"
            )
        }
        assertTrue(jitteredDelays.distinct().size > 1, "Jitter should produce varying delays")
    }
}
