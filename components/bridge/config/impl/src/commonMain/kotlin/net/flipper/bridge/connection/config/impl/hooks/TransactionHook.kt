package net.flipper.bridge.connection.config.impl.hooks

import net.flipper.bridge.connection.config.api.PersistedStorageTransactionScope

interface TransactionHook {
    fun PersistedStorageTransactionScope.postTransaction()
}
