package net.flipper.core.ktor

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

actual fun getPlatformEngineFactory(): HttpClientEngineFactory<*> {
    return object : HttpClientEngineFactory<OkHttpConfig> {
        override fun create(block: OkHttpConfig.() -> Unit): HttpClientEngine {
            return OkHttp.create {
                block()
                preconfigured = OkHttpClient.Builder()
                    .pingInterval(PING_INTERVAL.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                    .build()
            }
        }
    }
}