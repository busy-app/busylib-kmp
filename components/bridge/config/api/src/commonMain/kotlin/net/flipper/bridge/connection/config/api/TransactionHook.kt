package net.flipper.bridge.connection.config.api

interface TransactionHook {
    fun getPriority(): HookPriority

    fun PersistedStorageTransactionScope.postTransaction()
}

// The higher the priority, the later the transaction hook will be called
enum class HookPriority {
    LOW,
    NORMAL,
    HIGH
}
