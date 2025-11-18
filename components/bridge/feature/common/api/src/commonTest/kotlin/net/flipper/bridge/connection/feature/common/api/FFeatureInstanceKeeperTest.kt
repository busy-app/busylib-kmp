package net.flipper.bridge.connection.feature.common.api

import kotlin.test.Test
import kotlin.test.assertContentEquals

class FFeatureInstanceKeeperTest {

    private data class IntDataClass(val value: Int) : FFeatureInstanceKeeper.Instance

    @Test
    fun GIVEN_keeper_with_different_factories_WHEN_create_multiple_times_THEN_equals_first() {
        val instanceKeeper: FFeatureInstanceKeeper = SetFFeatureInstanceKeeper()
        assertContentEquals(
            listOf(
                instanceKeeper.getOrCreate { IntDataClass(0) },
                instanceKeeper.getOrCreate { IntDataClass(1) },
                instanceKeeper.getOrCreate { IntDataClass(2) },
            ),
            List(3) { IntDataClass(0) }
        )
    }
}
