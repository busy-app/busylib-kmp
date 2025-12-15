package net.flipper.bridge.connection.transport.tcp.common.engine

import io.ktor.client.engine.HttpClientEngineFactory
import kotlin.time.Duration.Companion.seconds

val PING_INTERVAL = 2.seconds

expect fun getPlatformEngineFactory(): HttpClientEngineFactory<*>
