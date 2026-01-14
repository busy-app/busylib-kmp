package net.flipper.core.busylib.ktx.common.cache

import kotlin.time.ComparableTimeMark
import kotlin.time.TimeSource

/**
 * Interface for providing time marks to enable testing of time-dependent code
 */
interface TimeProvider {
    /**
     * Returns a time mark representing the current point in time
     */
    fun markNow(): ComparableTimeMark
}

/**
 * Default implementation that uses the real monotonic time source
 */
class SystemTimeProvider : TimeProvider {
    override fun markNow(): ComparableTimeMark {
        return TimeSource.Monotonic.markNow()
    }
}
