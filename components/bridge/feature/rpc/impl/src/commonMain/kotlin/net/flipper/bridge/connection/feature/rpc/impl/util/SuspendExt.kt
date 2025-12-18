package net.flipper.bridge.connection.feature.rpc.impl.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun <T> runSafely(
    dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
    block: suspend () -> T
): Result<T> {
    return withContext(dispatcher) {
        runCatching { block.invoke() }
    }
}
