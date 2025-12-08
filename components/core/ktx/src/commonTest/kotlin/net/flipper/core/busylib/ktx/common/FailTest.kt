package net.flipper.core.busylib.ktx.common

import kotlin.test.Test
import kotlin.test.assertTrue

class FailTest {
    @Test
    fun testFail() {
        assertTrue { false }
    }
}