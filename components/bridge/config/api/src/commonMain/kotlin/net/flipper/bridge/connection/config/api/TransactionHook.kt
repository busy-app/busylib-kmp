package net.flipper.bridge.connection.config.api

interface TransactionHook {
    fun PersistedStorageTransactionScope.postTransaction()
}