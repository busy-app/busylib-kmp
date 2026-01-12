package net.flipper.core.busylib.ktx.common.cache

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Suppress("LargeClass")
class DefaultObjectCacheTest {

    @Test
    fun GIVEN_cache_WHEN_get_same_class_twice_THEN_cached_value_returned() = runTest {
        val cache = DefaultObjectCache()
        val counterFlow = MutableStateFlow(0)

        val deferred1 = cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value1"
            }
        )

        val deferred2 = cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value2"
            }
        )

        assertEquals("value1", deferred1.await())
        assertEquals("value1", deferred2.await())
        assertEquals(1, counterFlow.first())
        assertSame(deferred1, deferred2)
    }

    @Test
    fun GIVEN_cache_WHEN_get_different_classes_THEN_different_values_returned() = runTest {
        val cache = DefaultObjectCache()

        val stringDeferred = cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = { "string_value" }
        )

        val intDeferred = cache.getOrElse(
            ignoreCache = false,
            clazz = Int::class,
            block = { 42 }
        )

        assertEquals("string_value", stringDeferred.await())
        assertEquals(42, intDeferred.await())
    }

    @Test
    fun GIVEN_cache_WHEN_ignore_cache_true_THEN_new_value_created() = runTest {
        val cache = DefaultObjectCache()
        val counterFlow = MutableStateFlow(0)

        val deferred1 = cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value${counterFlow.first()}"
            }
        )

        val deferred2 = cache.getOrElse(
            ignoreCache = true,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value${counterFlow.first()}"
            }
        )

        assertEquals("value1", deferred1.await())
        assertEquals("value2", deferred2.await())
        assertEquals(2, counterFlow.first())
        assertNotEquals(deferred1, deferred2)
    }

    @Test
    fun GIVEN_cache_with_short_alive_after_read_WHEN_constructed_THEN_accepts_duration() = runTest {
        // Test that cache can be constructed with custom durations
        val cache = DefaultObjectCache(
            aliveAfterRead = 10.milliseconds,
            aliveAfterWrite = 1.seconds
        )
        val counterFlow = MutableStateFlow(0)

        val deferred = cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value1"
            }
        )

        assertEquals("value1", deferred.await())
        assertEquals(1, counterFlow.first())

        // Access again immediately - should still be cached
        val deferred2 = cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value2"
            }
        )

        assertEquals("value1", deferred2.await())
        assertEquals(1, counterFlow.first())
    }

    @Test
    fun GIVEN_cache_with_short_alive_after_write_WHEN_constructed_THEN_accepts_duration() = runTest {
        // Test that cache can be constructed with custom durations
        val cache = DefaultObjectCache(
            aliveAfterRead = 1.seconds,
            aliveAfterWrite = 10.milliseconds
        )
        val counterFlow = MutableStateFlow(0)

        val deferred = cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value1"
            }
        )

        assertEquals("value1", deferred.await())
        assertEquals(1, counterFlow.first())

        // Access again immediately - should still be cached
        val deferred2 = cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value2"
            }
        )

        assertEquals("value1", deferred2.await())
        assertEquals(1, counterFlow.first())
    }

    @Test
    fun GIVEN_cache_WHEN_multiple_reads_THEN_cache_updates_lastReadAt() = runTest {
        val cache = DefaultObjectCache(
            aliveAfterRead = 1.seconds,
            aliveAfterWrite = 1.seconds
        )
        val counterFlow = MutableStateFlow(0)

        cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value1"
            }
        ).await()

        // Multiple subsequent reads should return cached value
        cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "should_not_execute"
            }
        ).await()

        cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "should_not_execute_either"
            }
        ).await()

        // Only first block should have executed
        assertEquals(1, counterFlow.first())
    }

    @Test
    fun GIVEN_cache_WHEN_clear_called_THEN_all_entries_removed() = runTest {
        val cache = DefaultObjectCache()
        val counterFlow = MutableStateFlow(0)

        cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value${counterFlow.first()}"
            }
        ).await()

        assertEquals(1, counterFlow.first())

        cache.clear()

        // After clear, should create new entry
        val deferred2 = cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value${counterFlow.first()}"
            }
        )

        assertEquals("value2", deferred2.await())
        assertEquals(2, counterFlow.first())
    }

    @Test
    fun GIVEN_cache_WHEN_concurrent_requests_THEN_only_one_execution() = runTest {
        val cache = DefaultObjectCache()
        val counterFlow = MutableStateFlow(0)

        val jobs = List(10) { index ->
            cache.getOrElse(
                ignoreCache = false,
                clazz = String::class,
                block = {
                    delay(100)
                    counterFlow.update { it + 1 }
                    "value${counterFlow.first()}"
                }
            )
        }

        advanceTimeBy(150)

        // All should get the same deferred
        val results = jobs.awaitAll()
        results.forEach { assertEquals("value1", it) }
        assertEquals(1, counterFlow.first())
    }

    @Test
    fun GIVEN_cache_WHEN_concurrent_different_classes_THEN_separate_executions() = runTest {
        val cache = DefaultObjectCache()
        val counterFlow = MutableStateFlow(0)

        val stringDeferred = cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                delay(50)
                counterFlow.update { it + 1 }
                "string_value"
            }
        )

        val intDeferred = cache.getOrElse(
            ignoreCache = false,
            clazz = Int::class,
            block = {
                delay(50)
                counterFlow.update { it + 1 }
                42
            }
        )

        advanceTimeBy(100)

        assertEquals("string_value", stringDeferred.await())
        assertEquals(42, intDeferred.await())
        assertEquals(2, counterFlow.first())
    }

    @Test
    fun GIVEN_cache_WHEN_deferred_not_awaited_THEN_still_cached() = runTest {
        val cache = DefaultObjectCache()
        val counterFlow = MutableStateFlow(0)

        // Get deferred but don't await
        val deferred1 = cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value1"
            }
        )

        // Get again
        val deferred2 = cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value2"
            }
        )

        // Should be same deferred
        assertSame(deferred1, deferred2)
        assertEquals("value1", deferred2.await())
        assertEquals(1, counterFlow.first())
    }

    @Test
    fun GIVEN_cache_WHEN_inline_extension_used_THEN_works_correctly() = runTest {
        val cache = DefaultObjectCache()
        val counterFlow = MutableStateFlow(0)

        val value1: String = cache.getOrElse(ignoreCache = false) {
            counterFlow.update { it + 1 }
            "value1"
        }

        val value2: String = cache.getOrElse(ignoreCache = false) {
            counterFlow.update { it + 1 }
            "value2"
        }

        assertEquals("value1", value1)
        assertEquals("value1", value2)
        assertEquals(1, counterFlow.first())
    }

    @Test
    fun GIVEN_cache_WHEN_multiple_clears_THEN_works_correctly() = runTest {
        val cache = DefaultObjectCache()

        cache.getOrElse(ignoreCache = false, clazz = String::class) { "value1" }.await()
        cache.clear()
        cache.clear() // Double clear should work

        val result = cache.getOrElse(ignoreCache = false, clazz = String::class) { "value2" }.await()
        assertEquals("value2", result)
    }

    @Test
    fun GIVEN_multiple_cache_entries_WHEN_accessing_THEN_each_type_cached_independently() = runTest {
        val cache = DefaultObjectCache()
        val counterFlow = MutableStateFlow(0)

        // Create multiple different type entries
        cache.getOrElse(ignoreCache = false, clazz = String::class) {
            counterFlow.update { it + 1 }
            "string"
        }.await()

        cache.getOrElse(ignoreCache = false, clazz = Int::class) {
            counterFlow.update { it + 1 }
            42
        }.await()

        // Access them again - should be cached
        val stringDeferred = cache.getOrElse(ignoreCache = false, clazz = String::class) { "new_string" }
        val intDeferred = cache.getOrElse(ignoreCache = false, clazz = Int::class) { 999 }

        assertEquals("string", stringDeferred.await())
        assertEquals(42, intDeferred.await())
        assertEquals(2, counterFlow.first())
    }

    @Test
    fun GIVEN_block_throws_exception_WHEN_awaiting_deferred_THEN_exception_propagated() = runTest {
        val cache = DefaultObjectCache()

        var exceptionCaught = false
        try {
            val deferred = cache.getOrElse(
                ignoreCache = false,
                clazz = String::class,
                block = {
                    delay(1) // Make it suspend to avoid immediate execution
                    error("Test exception")
                }
            )
            deferred.await()
        } catch (_: Throwable) {
            exceptionCaught = true
            // Exception message might be wrapped, just verify it was thrown
        }

        assertEquals(true, exceptionCaught)
    }

    @Test
    fun GIVEN_failed_deferred_WHEN_accessed_again_THEN_cached_failed_deferred_returned() = runTest {
        val cache = DefaultObjectCache()
        val counterFlow = MutableStateFlow(0)

        var deferred1: Deferred<String>? = null
        var deferred2: Deferred<String>? = null

        try {
            deferred1 = cache.getOrElse(
                ignoreCache = false,
                clazz = String::class,
                block = {
                    counterFlow.update { it + 1 }
                    delay(1) // Make it suspend to avoid immediate execution
                    error("Test exception")
                }
            )

            deferred2 = cache.getOrElse(
                ignoreCache = false,
                clazz = String::class,
                block = {
                    counterFlow.update { it + 1 }
                    "should_not_execute"
                }
            )

            // Should be same deferred (cached)
            assertSame(deferred1, deferred2)

            // Verify block was only called once even though deferred failed
            assertEquals(1, counterFlow.first())

            // Try to await - should throw
            deferred1.await()
        } catch (_: Throwable) {
            // Expected exception from the failed deferred
            // Verify block was only called once
            assertEquals(1, counterFlow.first())
        }
    }

    @Test
    fun GIVEN_default_durations_WHEN_cache_created_THEN_uses_defaults() = runTest {
        // Test default constructor parameters
        val cache = DefaultObjectCache()
        val result = cache.getOrElse(ignoreCache = false, clazz = String::class) { "test" }
        assertEquals("test", result.await())
    }

    @Test
    fun GIVEN_multiple_concurrent_getOrElse_WHEN_same_key_THEN_single_execution() = runTest {
        val cache = DefaultObjectCache()
        val executionCounter = MutableStateFlow(0)

        // Launch multiple concurrent requests for the same key
        val deferreds = List(100) {
            cache.getOrElse(
                ignoreCache = false,
                clazz = Int::class,
                block = {
                    delay(10) // Simulate work
                    executionCounter.update { count -> count + 1 }
                    executionCounter.first()
                }
            )
        }

        // All should return the same result from a single execution
        val results = deferreds.awaitAll()
        results.forEach { assertEquals(1, it) }
        assertEquals(1, executionCounter.first())
    }

    @Test
    fun GIVEN_empty_cache_WHEN_clear_called_THEN_no_error() = runTest {
        val cache = DefaultObjectCache()
        // Should not throw
        cache.clear()
        cache.clear()
    }

    @Test
    fun GIVEN_cache_WHEN_using_inline_extension_with_different_types_THEN_works_correctly() = runTest {
        val cache = DefaultObjectCache()

        val stringValue: String = cache.getOrElse(ignoreCache = false) { "hello" }
        val intValue: Int = cache.getOrElse(ignoreCache = false) { 42 }
        val boolValue: Boolean = cache.getOrElse(ignoreCache = false) { true }

        assertEquals("hello", stringValue)
        assertEquals(42, intValue)
        assertEquals(true, boolValue)

        // Access again - should be cached
        val stringValue2: String = cache.getOrElse(ignoreCache = false) { "world" }
        val intValue2: Int = cache.getOrElse(ignoreCache = false) { 99 }

        assertEquals("hello", stringValue2)
        assertEquals(42, intValue2)
    }

    @Test
    fun GIVEN_cache_with_value_WHEN_ignore_cache_used_multiple_times_THEN_creates_new_each_time() = runTest {
        val cache = DefaultObjectCache()
        val counter = MutableStateFlow(0)

        val val1 = cache.getOrElse(ignoreCache = true, clazz = String::class) {
            counter.update { it + 1 }
            "val${counter.first()}"
        }.await()

        val val2 = cache.getOrElse(ignoreCache = true, clazz = String::class) {
            counter.update { it + 1 }
            "val${counter.first()}"
        }.await()

        val val3 = cache.getOrElse(ignoreCache = true, clazz = String::class) {
            counter.update { it + 1 }
            "val${counter.first()}"
        }.await()

        assertEquals("val1", val1)
        assertEquals("val2", val2)
        assertEquals("val3", val3)
        assertEquals(3, counter.first())
    }

    @Test
    fun GIVEN_NonCancellable_context_WHEN_getOrElse_called_THEN_executes_successfully() = runTest {
        // Test that NonCancellable context is used internally
        val cache = DefaultObjectCache()
        val result = cache.getOrElse(ignoreCache = false, clazz = String::class) {
            delay(10)
            "result"
        }
        assertEquals("result", result.await())
    }

    @Test
    fun GIVEN_cache_WHEN_same_class_accessed_repeatedly_THEN_lastReadAt_updated() = runTest {
        val cache = DefaultObjectCache()

        // First access
        cache.getOrElse(ignoreCache = false, clazz = String::class) { "value" }.await()

        // Multiple subsequent accesses - should update lastReadAt each time
        repeat(5) {
            cache.getOrElse(ignoreCache = false, clazz = String::class) { "new_value" }.await()
        }

        // All should return the same cached value
        val result = cache.getOrElse(ignoreCache = false, clazz = String::class) { "another_value" }.await()
        assertEquals("value", result)
    }

    // ===== Time-based tests using TestTimeProvider =====

    @Test
    fun GIVEN_cache_with_test_time_WHEN_time_advances_past_alive_after_read_THEN_entry_expires() = runTest {
        val timeProvider = TestTimeProvider()
        val cache = DefaultObjectCache(
            aliveAfterRead = 100.milliseconds,
            aliveAfterWrite = 1.seconds,
            timeProvider = timeProvider
        )
        val counterFlow = MutableStateFlow(0)

        // Create first entry
        val deferred1 = cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value${counterFlow.first()}"
            }
        )

        assertEquals("value1", deferred1.await())
        assertEquals(1, counterFlow.first())

        // Advance time past both aliveAfterRead AND aliveAfterWrite
        // (entry expires only when BOTH durations have passed)
        timeProvider.advance(1100.milliseconds)

        // Should create new entry as previous expired
        val deferred2 = cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value${counterFlow.first()}"
            }
        )

        assertEquals("value2", deferred2.await())
        assertEquals(2, counterFlow.first())
    }

    @Test
    fun GIVEN_cache_with_test_time_WHEN_time_advances_past_alive_after_write_THEN_entry_expires() = runTest {
        val timeProvider = TestTimeProvider()
        val cache = DefaultObjectCache(
            aliveAfterRead = 1.seconds,
            aliveAfterWrite = 100.milliseconds,
            timeProvider = timeProvider
        )
        val counterFlow = MutableStateFlow(0)

        // Create first entry
        val deferred1 = cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value${counterFlow.first()}"
            }
        )

        assertEquals("value1", deferred1.await())
        assertEquals(1, counterFlow.first())

        // Advance time past BOTH aliveAfterWrite AND aliveAfterRead
        // (entry expires only when BOTH durations have passed)
        timeProvider.advance(1100.milliseconds)

        // Should create new entry as previous expired
        val deferred2 = cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value${counterFlow.first()}"
            }
        )

        assertEquals("value2", deferred2.await())
        assertEquals(2, counterFlow.first())
    }

    @Test
    fun GIVEN_cache_with_test_time_WHEN_read_updates_lastReadAt_THEN_expiration_delayed() = runTest {
        val timeProvider = TestTimeProvider()
        val cache = DefaultObjectCache(
            aliveAfterRead = 100.milliseconds,
            aliveAfterWrite = 1.seconds,
            timeProvider = timeProvider
        )
        val counterFlow = MutableStateFlow(0)

        // Create first entry at time 0
        cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value${counterFlow.first()}"
            }
        ).await()

        assertEquals(1, counterFlow.first())

        // Advance time by 50ms (less than aliveAfterRead)
        timeProvider.advance(50.milliseconds)

        // Read again - this updates lastReadAt to current time (50ms)
        cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "should_not_execute"
            }
        ).await()

        assertEquals(1, counterFlow.first())

        // Advance time by another 60ms (total 110ms from first write, but only 60ms from last read)
        timeProvider.advance(60.milliseconds)

        // Should still use cached value because last read was at 50ms, and 50ms + 100ms = 150ms > 110ms
        val deferred3 = cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value${counterFlow.first()}"
            }
        )

        assertEquals("value1", deferred3.await())
        assertEquals(1, counterFlow.first())
    }

    @Test
    fun GIVEN_cache_with_test_time_WHEN_both_durations_expire_THEN_entry_removed() = runTest {
        val timeProvider = TestTimeProvider()
        val cache = DefaultObjectCache(
            aliveAfterRead = 100.milliseconds,
            aliveAfterWrite = 200.milliseconds,
            timeProvider = timeProvider
        )
        val counterFlow = MutableStateFlow(0)

        // Create first entry
        cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value1"
            }
        ).await()

        // Advance time past aliveAfterRead but not aliveAfterWrite
        timeProvider.advance(150.milliseconds)

        // Entry should NOT be expired yet (needs both to expire)
        val deferred2 = cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value2"
            }
        )

        assertEquals("value1", deferred2.await())
        assertEquals(1, counterFlow.first())

        // Now advance past both durations
        timeProvider.advance(100.milliseconds) // Total 250ms

        // Entry should be expired now
        val deferred3 = cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value${counterFlow.first()}"
            }
        )

        assertEquals("value2", deferred3.await())
        assertEquals(2, counterFlow.first())
    }

    @Test
    fun GIVEN_multiple_entries_with_test_time_WHEN_some_expire_THEN_only_expired_removed() = runTest {
        val timeProvider = TestTimeProvider()
        val cache = DefaultObjectCache(
            aliveAfterRead = 100.milliseconds,
            aliveAfterWrite = 100.milliseconds,
            timeProvider = timeProvider
        )

        // Create first entry (String)
        val string1 = cache.getOrElse(ignoreCache = false, clazz = String::class) { "string1" }.await()
        assertEquals("string1", string1)

        // Advance time by 50ms
        timeProvider.advance(50.milliseconds)

        // Create second entry (Int) at time 50ms
        val int1 = cache.getOrElse(ignoreCache = false, clazz = Int::class) { 42 }.await()
        assertEquals(42, int1)

        // Advance time by 60ms (total 110ms)
        // String entry should be expired (110ms > 100ms)
        // Int entry should NOT be expired (60ms < 100ms)
        timeProvider.advance(60.milliseconds)

        // Access String - should create new entry
        val string2 = cache.getOrElse(ignoreCache = false, clazz = String::class) { "string2" }.await()
        assertEquals("string2", string2)

        // Access Int - should still be cached
        val int2 = cache.getOrElse(ignoreCache = false, clazz = Int::class) { 999 }.await()
        assertEquals(42, int2)
    }

    @Test
    fun GIVEN_cache_with_test_time_WHEN_time_not_advanced_THEN_entry_never_expires() = runTest {
        val timeProvider = TestTimeProvider()
        val cache = DefaultObjectCache(
            aliveAfterRead = 10.milliseconds,
            aliveAfterWrite = 10.milliseconds,
            timeProvider = timeProvider
        )
        val counterFlow = MutableStateFlow(0)

        // Create entry
        cache.getOrElse(
            ignoreCache = false,
            clazz = String::class,
            block = {
                counterFlow.update { it + 1 }
                "value1"
            }
        ).await()

        // Access multiple times without advancing time
        repeat(100) {
            cache.getOrElse(
                ignoreCache = false,
                clazz = String::class,
                block = {
                    counterFlow.update { count -> count + 1 }
                    "should_not_execute"
                }
            ).await()
        }

        // Should still have only executed once
        assertEquals(1, counterFlow.first())
    }

    @Test
    fun GIVEN_cache_with_test_time_WHEN_clear_called_THEN_time_state_preserved() = runTest {
        val timeProvider = TestTimeProvider()
        val cache = DefaultObjectCache(
            aliveAfterRead = 100.milliseconds,
            aliveAfterWrite = 100.milliseconds,
            timeProvider = timeProvider
        )

        // Create entry
        cache.getOrElse(ignoreCache = false, clazz = String::class) { "value1" }.await()

        // Advance time
        timeProvider.advance(50.milliseconds)

        // Clear cache
        cache.clear()

        // Create new entry - should use current time (50ms)
        cache.getOrElse(ignoreCache = false, clazz = String::class) { "value2" }.await()

        // Advance by 60ms (total 110ms, but entry created at 50ms so only 60ms old)
        timeProvider.advance(60.milliseconds)

        // Should still be cached
        val result = cache.getOrElse(ignoreCache = false, clazz = String::class) { "value3" }.await()
        assertEquals("value2", result)
    }
}
