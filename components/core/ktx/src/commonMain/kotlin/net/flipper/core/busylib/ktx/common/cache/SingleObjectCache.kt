package net.flipper.core.busylib.ktx.common.cache

import kotlinx.coroutines.flow.Flow

interface SingleObjectCache<T : Any> {
    val flow: Flow<T>

    /**
     * @return either cached request or one executed by [block] if [key] is different from previous
     */
    suspend fun getOrElse(
        key: Any? = null,
        block: suspend () -> T
    ): T

    suspend fun clear()
}
