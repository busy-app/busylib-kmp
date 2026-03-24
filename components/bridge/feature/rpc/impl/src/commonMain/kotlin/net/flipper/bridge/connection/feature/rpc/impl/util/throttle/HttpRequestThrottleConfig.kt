package net.flipper.bridge.connection.feature.rpc.impl.util.throttle

import io.ktor.utils.io.KtorDsl
import kotlin.time.Duration

/**
 * [HttpRequestThrottle] Ktor Client Plugin configuration.
 *
 * @author https://github.com/brudaswen/ktor-client-throttle/
 */
@KtorDsl
public class HttpRequestThrottleConfig {
    /**
     * The [HttpRequestThrottler] used for request throttling.
     */
    public var throttler: HttpRequestThrottler = DefaultHttpRequestThrottler()

    /**
     * Default [HttpRequestThrottler] that starts with [limit] tokens, and will be refilled every
     * [refillPeriod].
     *
     * @param limit The number of tokens.
     * @param refillPeriod Period after which the tokens get refilled.
     */
    public fun throttler(
        limit: Int,
        refillPeriod: Duration
    ) {
        throttler = DefaultHttpRequestThrottler(
            limit = limit,
            refillPeriod = refillPeriod
        )
    }
}
