package net.flipper.bridge.connection.feature.rpc.impl.util.throttle

import io.ktor.client.request.HttpRequestBuilder
import kotlinx.coroutines.delay

/**
 * Throttle logic for the [HttpRequestThrottle] Ktor Client Plugin.
 *
 * Can throttle client requests in case the server is rate-limited.
 *
 * Kind of the counterpart to the
 * [Ktor Rate Limiting Server Plugin](https://ktor.io/docs/server-rate-limit.html).
 *
 * @author https://github.com/brudaswen/ktor-client-throttle/
 */
public interface HttpRequestThrottler {
    /**
     * Throttle this [request] by calling [delay] or return immediately if this
     * request should not be throttled.
     */
    public suspend fun throttle(request: HttpRequestBuilder)
}
