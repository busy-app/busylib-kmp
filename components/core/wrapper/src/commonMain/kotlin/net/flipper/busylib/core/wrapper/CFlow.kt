package net.flipper.busylib.core.wrapper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

private fun <T> Flow<T>.onEach(
    onEach: (T) -> Unit,
    onComplete: () -> Unit = {},
    onError: (Throwable) -> Unit = {}
): Closeable {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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
    ): Closeable = origin.onEach(onEach, onComplete, onError)
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
class WrappedStateFlow<T : Any?>(private val origin: StateFlow<T>) : StateFlow<T> by origin {
    fun watch(
        onEach: (T) -> Unit,
        onComplete: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ): Closeable = origin.onEach(onEach, onComplete, onError)
}

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
class WrappedSharedFlow<T : Any?>(private val origin: SharedFlow<T>) : SharedFlow<T> by origin {
    fun watch(
        onEach: (T) -> Unit,
        onComplete: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ): Closeable = origin.onEach(onEach, onComplete, onError)
}

fun <T : Any?> StateFlow<T>.wrap(): WrappedStateFlow<T> = WrappedStateFlow(this)
fun <T : Any?> SharedFlow<T>.wrap(): WrappedSharedFlow<T> = WrappedSharedFlow(this)
fun <T : Any?> Flow<T>.wrap(): WrappedFlow<T> = WrappedFlow(this)

interface Closeable {
    fun close()
}
