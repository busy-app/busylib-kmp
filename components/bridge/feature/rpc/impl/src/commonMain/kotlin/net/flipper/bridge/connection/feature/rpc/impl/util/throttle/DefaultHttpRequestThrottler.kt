package net.flipper.bridge.connection.feature.rpc.impl.util.throttle

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.util.AttributeKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.flipper.core.busylib.log.LogTagProvider
import net.flipper.core.busylib.log.verbose
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Default [HttpRequestThrottler] that starts with [limit] tokens, and will be refilled every
 * [refillPeriod].
 *
 * @param limit The number of tokens.
 * @param refillPeriod Period after which the tokens get refilled.
 *
 * @author https://github.com/brudaswen/ktor-client-throttle/
 */
internal class DefaultHttpRequestThrottler(
    private var limit: Int = 1,
    private val refillPeriod: Duration = Duration.ZERO
) : HttpRequestThrottler, LogTagProvider {
    override val TAG = "DefaultHttpRequestThrottler"

    private val mutex = Mutex()

    private var remaining = 0

    private var reset: Instant = Instant.DISTANT_PAST

    override suspend fun throttle(request: HttpRequestBuilder) {
        mutex.withLock {
            if (refillPeriod > Duration.ZERO) {
                // Throttle if all slots are blocked
                if (remaining <= 0) {
                    verbose { "Delaying request ${request.url} due to rate limit" }
                    delay(reset - Clock.System.now())

                    // Refill bucket if reset time has passed
                    reset = Clock.System.now() + refillPeriod
                    request.attributes.put(ResetAttribute, reset)

                    remaining = limit
                }

                // Block one slot and continue this request
                remaining--
            }
        }
    }

    override suspend fun onResponse(response: HttpResponse) {
        if (response.request.attributes.getOrNull(ResetAttribute) == reset) {
            // If this was the request that started this bucket, we update the
            // reset time after we receive the response. This ensures that our time is not smaller
            // than the server's reset time
            reset = Clock.System.now() + refillPeriod
        }
    }

    companion object {
        private val ResetAttribute = AttributeKey<Instant>("DefaultHttpRequestThrottler.reset")
    }
}
