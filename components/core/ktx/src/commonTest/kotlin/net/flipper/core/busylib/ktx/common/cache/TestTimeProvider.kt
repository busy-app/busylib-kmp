package net.flipper.core.busylib.ktx.common.cache

import kotlin.time.ComparableTimeMark
import kotlin.time.Duration

/**
 * Test implementation of TimeProvider that allows manual control of time.
 *
 * This implementation uses a custom TestTimeMark that tracks elapsed time
 * relative to a virtual time that can be advanced manually for testing.
 */
class TestTimeProvider : TimeProvider {
    private var currentVirtualTime: Duration = Duration.ZERO

    /**
     * Advances the current virtual time by the specified duration
     */
    fun advance(duration: Duration) {
        currentVirtualTime += duration
    }

    /**
     * Resets the virtual time to zero
     */
    fun reset() {
        currentVirtualTime = Duration.ZERO
    }

    override fun markNow(): ComparableTimeMark {
        return TestTimeMark(currentVirtualTime, this)
    }

    /**
     * Custom TimeMark implementation for testing that uses virtual time
     */
    private data class TestTimeMark(
        private val markedAt: Duration,
        private val provider: TestTimeProvider
    ) : ComparableTimeMark {

        override fun elapsedNow(): Duration {
            return provider.currentVirtualTime - markedAt
        }

        override fun plus(duration: Duration): ComparableTimeMark {
            // plus returns a mark that is 'duration' in the future from this mark
            // So if we're at markedAt, plus(d) means we want a mark at markedAt + d
            return TestTimeMark(markedAt + duration, provider)
        }

        override fun minus(other: ComparableTimeMark): Duration {
            require(other is TestTimeMark) { "Can only subtract TestTimeMark from TestTimeMark" }
            return markedAt - other.markedAt
        }

        override fun compareTo(other: ComparableTimeMark): Int {
            require(other is TestTimeMark) { "Can only compare TestTimeMark with TestTimeMark" }
            return markedAt.compareTo(other.markedAt)
        }

        override fun hasPassedNow(): Boolean {
            return provider.currentVirtualTime >= markedAt
        }

        override fun hasNotPassedNow(): Boolean {
            return provider.currentVirtualTime < markedAt
        }
    }
}
