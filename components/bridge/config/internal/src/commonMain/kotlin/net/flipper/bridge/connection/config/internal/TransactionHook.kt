package net.flipper.bridge.connection.config.internal

interface TransactionHook {
    fun getPriority(): HookPriority

    fun InternalStorageTransactionScope.postTransaction()
}

// The higher the priority, the later the transaction hook will be called
enum class HookPriority {
    LOW,
    NORMAL,
    HIGH
}
