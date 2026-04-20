package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import net.flipper.core.busylib.log.LogTagProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class TestClosedException(message: String) : Exception(message)

class WrapWebsocketTest : LogTagProvider {
    override val TAG = "WrapWebsocketTest"

    /**
     * Models the real OkHttp/Ktor crash:
     *
     *  1. OkHttp's TaskRunner thread fires a ping → IOException("closed")
     *  2. OkHttp catches it, calls listener.onFailure **on that thread**
     *  3. Ktor's onFailure closes its outgoing channel with the IOException
     *  4. Ktor's writeJob (a `launch` child) receives the closed channel,
     *     fails, and delivers the exception to CoroutineExceptionHandler
     *  5. No handler → Thread.uncaughtExceptionHandler → FATAL
     *
     * We reproduce this by closing a channel with an exception from
     * outside the coroutine that reads it — a child `launch` iterates
     * the channel and fails when it is closed with a cause, same as
     * Ktor's writeJob fails when onFailure closes the outgoing channel.
     *
     * The fix: `coroutineScope` in wrapWebsocket acts as a Job boundary,
     * converting the child launch failure into a regular thrown exception
     * that `runSuspendCatching` catches and retries.
     */
    @Test
    fun GIVEN_channel_closed_with_cause_WHEN_collecting_THEN_no_fatal_crash() = runTest {
        val flow = wrapWebsocket {
            channelFlow {
                // Channel that simulates Ktor's outgoing channel
                val outgoing = Channel<Unit>()
                // Gate so we only close the channel after the message is delivered
                val delivered = CompletableDeferred<Unit>()

                // Simulate Ktor's writeJob: iterates the outgoing channel.
                // When onFailure closes the channel with an exception,
                // this `for` throws, failing the launch — the uncaught
                // exception that causes the real FATAL crash.
                launch {
                    outgoing.consumeEach { /* process frames */ }
                }

                send("message")
                delivered.complete(Unit)

                // Simulate OkHttp's listener.onFailure closing the channel
                // with the ping exception. This is a direct call, not a
                // coroutine — onFailure runs on OkHttp's TaskRunner thread
                // and Channel.close is thread-safe.
                launch {
                    delivered.await()
                    outgoing.close(TestClosedException("closed"))
                }

                awaitCancellation()
            }
        }

        val result = flow.first()
        assertEquals("message", result)
    }

    @Test
    fun GIVEN_channel_closed_with_cause_WHEN_collecting_THEN_parent_survives() = runTest {
        var collected = false

        val job = launch {
            val flow = wrapWebsocket {
                channelFlow {
                    val outgoing = Channel<Unit>()
                    val delivered = CompletableDeferred<Unit>()
                    launch {
                        outgoing.consumeEach { /* */ }
                    }
                    send("value")
                    delivered.complete(Unit)
                    launch {
                        delivered.await()
                        outgoing.close(TestClosedException("closed"))
                    }
                    awaitCancellation()
                }
            }
            flow.first()
            collected = true
        }
        job.join()

        assertTrue(collected, "Parent coroutine should survive channel-close failure")
    }

    @Test
    fun GIVEN_block_throws_directly_WHEN_collecting_THEN_retries() = runTest {
        var attempt = 0

        val flow = wrapWebsocket<String> {
            attempt++
            if (attempt == 1) {
                throw TestClosedException("connection refused")
            }
            channelFlow { send("recovered") }
        }

        val result = flow.first()
        assertEquals("recovered", result)
        assertEquals(2, attempt)
    }

    @Test
    fun GIVEN_normal_flow_WHEN_collecting_THEN_emits_without_retry() = runTest {
        var connectCount = 0

        val flow = wrapWebsocket {
            connectCount++
            channelFlow { send("ok") }
        }

        val result = flow.first()
        assertEquals("ok", result)
        assertEquals(1, connectCount)
    }
}
