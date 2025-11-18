package net.flipper.bridge.connection.feature.common.api

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.collections.set

interface FFeatureInstanceKeeper {
    fun <T : Instance> getOrCreate(key: Any, create: () -> T): T

    interface Instance {
        fun dispose() = Unit
    }
}

inline fun <reified T : FFeatureInstanceKeeper.Instance> FFeatureInstanceKeeper.getOrCreate(
    noinline create: () -> T
): T = getOrCreate(T::class, create)

class SetFFeatureInstanceKeeper : FFeatureInstanceKeeper {
    private val set = HashMap<Any, Any>()
    private val mutex = Mutex()
    override fun <T : FFeatureInstanceKeeper.Instance> getOrCreate(
        key: Any,
        create: () -> T
    ): T {
        return runBlocking {
            mutex.withLock {
                val value = set[key] ?: create.invoke()
                set[key] = value
                value as T
            }
        }
    }
}
