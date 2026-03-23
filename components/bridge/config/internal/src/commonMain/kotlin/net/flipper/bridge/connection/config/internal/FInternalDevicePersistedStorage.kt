package net.flipper.bridge.connection.config.internal

import net.flipper.bridge.connection.config.api.FDevicePersistedStorage

/**
 * This is internal BUSY Lib class, don't use it outside BUSY Lib
 */
interface FInternalDevicePersistedStorage : FDevicePersistedStorage {
    suspend fun addHook(vararg hook: TransactionHook)

    suspend fun <T> transactionInternal(block: suspend InternalStorageTransactionScope.() -> T): T
}
