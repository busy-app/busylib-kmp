package net.flipper.core.ktor

import io.ktor.client.engine.HttpClientEngineFactory

expect fun getPlatformEngineFactory(): HttpClientEngineFactory<*>
