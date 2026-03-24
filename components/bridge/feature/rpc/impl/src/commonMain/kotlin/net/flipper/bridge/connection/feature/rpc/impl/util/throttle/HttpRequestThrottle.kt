package net.flipper.bridge.connection.feature.rpc.impl.util.throttle

import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin

/**
 * [HttpRequestThrottle] Ktor Client Plugin.
 *
 * Can throttle client requests in case the server is rate-limited.
 *
 * Kind of the counterpart to the
 * [Ktor Rate Limiting Server Plugin](https://ktor.io/docs/server-rate-limit.html).
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

        return@on proceed(request)
    }
}
