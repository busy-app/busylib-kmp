package net.flipper.core.busylib.ktx.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val MIN_JITTER_FRACTION = 0.5

fun getExponentialDelay(
    retryCount: Int,
    initialDelay: Duration = 1.seconds,
    maxDelay: Duration = 10.seconds,
    factor: Double = 2.0,
    random: Random = Random.Default,
): Duration {
    val resultDelay = initialDelay * factor.pow(retryCount)

    return resultDelay.coerceAtMost(maxDelay) * random.nextDouble(MIN_JITTER_FRACTION, 1.0)
}

suspend fun <T> exponentialRetry(
    retries: Long = Long.MAX_VALUE,
    initialDelay: Duration = 1.seconds,
    maxDelay: Duration = 10.seconds,
    factor: Double = 2.0,
    random: Random = Random.Default,
    block: suspend () -> Result<T>
): T {
    var count = 0
    return flow { emit(block.invoke().getOrThrow()) }
        .retry(retries) {
            val currentDelay = getExponentialDelay(
                retryCount = count++,
                initialDelay = initialDelay,
                maxDelay = maxDelay,
                factor = factor,
                random = random
            )
            delay(currentDelay)
            return@retry true
        }
        .first()
}
