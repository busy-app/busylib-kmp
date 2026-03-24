package net.flipper.bridge.connection.feature.rpc.impl.util.throttle

import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpStatusCode.Companion.TooManyRequests

/**
 * [HttpRequestThrottle] Ktor Client Plugin.
 *
 * Can throttle client requests in case the server is rate-limited.
 *
 * Kind of the counterpart to the
 * [Ktor Rate Limiting Server Plugin](https://ktor.io/docs/server-rate-limit.html).
 *
 * The default [HttpRequestThrottler] does not throttle and only takes care of automatic
 * retries. If the server responds with `429` [TooManyRequests] and the `Retry-After` header,
 * then the original request is automatically retried according to the delay defined in the
 * header.
 *
 * Throttling can be configured via the [HttpRequestThrottleConfig.throttler] config property.
 *
 * You can also implement fully custom throttling logic by building a custom [HttpRequestThrottler]
 * logic and assigning.
 *
 * @author https://github.com/brudaswen/ktor-client-throttle/
 */
public val HttpRequestThrottle: ClientPlugin<HttpRequestThrottleConfig> = createClientPlugin(
    name = "ThrottleFeature",
    createConfiguration = ::HttpRequestThrottleConfig,
) {

    on(Send) { request ->
        pluginConfig.throttler.throttle(request)

        var call = proceed(request)
        pluginConfig.throttler.onResponse(call.response)

        return@on call
    }
}
