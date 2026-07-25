package net.flipper.core.ktor.util

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetryConfig
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlin.coroutines.cancellation.CancellationException

private const val MAX_TRANSIENT_EXCEPTION_RETRIES = 3

private fun Throwable.isTransientException(): Boolean {
    return when (this) {
        is CancellationException,
        is HttpRequestTimeoutException,
        is ConnectTimeoutException,
        is SocketTimeoutException -> false

        else -> true
    }
}

/**
 * Ktor installs default exception-retry behavior in [HttpRequestRetryConfig] init -> [retryOnExceptionOrServerErrors(3)]
 */
fun HttpRequestRetryConfig.retryOnTransientExceptions() {
    retryOnExceptionIf { _, cause ->
        retryCount <= MAX_TRANSIENT_EXCEPTION_RETRIES && cause.isTransientException()
    }
}


