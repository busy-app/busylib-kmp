package net.flipper.busylib.core.wrapper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import net.flipper.core.busylib.ktx.common.FlipperDispatchers

/**
 * Deliberately not the main thread: Swift hands every value straight to an
 * `AsyncStream` continuation, which is thread safe, and its consumer hops to
 * whatever actor it belongs to on its own. Collecting on the main one would
 * only add a dispatch per value, and would drag every upstream operator of a
 * cold flow onto it.
 */
private fun <T> Flow<T>.subscribe(
    onEach: (T) -> Unit,
    onComplete: () -> Unit,
    onError: (Throwable) -> Unit
): Closeable {
    val scope = CoroutineScope(SupervisorJob() + FlipperDispatchers.default)

    onEach(onEach)
        .catch { e ->
            onError(e)
        }
        .onCompletion {
            onComplete()
            scope.cancel()
        }
        .launchIn(scope)

    return object : Closeable {
        override fun close() {
            scope.cancel()
        }
    }
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
class WrappedFlow<T : Any?>(private val origin: Flow<T>) : Flow<T> by origin {
    fun watch(
        onEach: (T) -> Unit,
        onComplete: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ): Closeable {
        return origin.subscribe(onEach, onComplete, onError)
    }
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
class WrappedStateFlow<T : Any?>(private val origin: StateFlow<T>) : StateFlow<T> by origin {
    fun watch(
        onEach: (T) -> Unit,
        onComplete: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ): Closeable {
        return origin.subscribe(onEach, onComplete, onError)
    }
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
class WrappedSharedFlow<T : Any?>(private val origin: SharedFlow<T>) : SharedFlow<T> by origin {
    fun watch(
        onEach: (T) -> Unit,
        onComplete: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ): Closeable {
        return origin.subscribe(onEach, onComplete, onError)
    }
}

fun <T : Any?> StateFlow<T>.wrap(): WrappedStateFlow<T> = WrappedStateFlow(this)
fun <T : Any?> SharedFlow<T>.wrap(): WrappedSharedFlow<T> = WrappedSharedFlow(this)
fun <T : Any?> Flow<T>.wrap(): WrappedFlow<T> = WrappedFlow(this)

// For explicit type drive without overide method
fun <T : Any?> Flow<T>.wrapFlow(): WrappedFlow<T> = WrappedFlow(this)

interface Closeable {
    fun close()
}
