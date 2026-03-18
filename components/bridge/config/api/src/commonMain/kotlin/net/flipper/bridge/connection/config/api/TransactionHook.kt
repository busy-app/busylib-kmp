package net.flipper.bridge.connection.config.api

interface TransactionHook {
    fun getPriority(): HookOrder

    fun PersistedStorageTransactionScope.postTransaction()
}

enum class HookOrder {
    FIRST,
    NORMAL,
    LAST
}