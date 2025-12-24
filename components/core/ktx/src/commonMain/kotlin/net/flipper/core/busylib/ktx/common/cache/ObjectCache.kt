package net.flipper.core.busylib.ktx.common.cache

import kotlin.reflect.KClass

interface ObjectCache {
    /**
     * @return either cached request or one executed by [block] if [key] is different from previous
     */
    suspend fun <T : Any> getOrElse(
        ignoreCache: Boolean,
        clazz: KClass<T>,
        block: suspend () -> T
    ): T

    suspend fun clear()
}

suspend inline fun <reified T : Any> ObjectCache.getOrElse(
    ignoreCache: Boolean,
    noinline block: suspend () -> T
): T = getOrElse(ignoreCache, T::class, block)
