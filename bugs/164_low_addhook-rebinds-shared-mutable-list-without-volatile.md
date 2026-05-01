# `FDevicePersistedStorageImpl.hooks` is a non-volatile `var` mutated under a Mutex but read without lock

## Type
infrastructure

**Severity:** low

**Files:**
- `components/bridge/config/impl/src/commonMain/kotlin/net/flipper/bridge/connection/config/impl/FDevicePersistedStorageImpl.kt` (lines 37–86)

## Summary

```kotlin
private var hooks = listOf<TransactionHook>(...).sortedBy { it.getPriority() }

override suspend fun addHook(vararg hook: TransactionHook) {
    withLock(mutex, "add_hook") {
        hooks = hooks.plus(hook).sortedBy { it.getPriority() }
    }
}

override suspend fun <T> transactionInternal(...): T = withLockResult(mutex, "transaction") {
    ...
    hooks.forEach { hook -> with(hook) { scope.postTransaction() } }
    ...
}
```

Both writes and the read in `transactionInternal` happen inside `withLock(mutex)`, so the read-during-
transaction is safe. However, KMP/JVM does not guarantee visibility of `var` writes across threads in the
absence of `@Volatile` (or `@Synchronized`/locking on the read side). Although the Mutex provides a happens-
before relationship for code paths that always lock, any read of `hooks` outside the mutex is not
guaranteed to see the latest list. Today no such read exists, but the API does not enforce that.

Additionally, `addHook` is the only `suspend`-ing API on `FInternalDevicePersistedStorage`. Callers must
remember to invoke it before any transaction. There is no read-through queue / event mechanism, so
contributors can construct an instance and call `transaction { }` immediately, missing hooks they registered
asynchronously elsewhere.

## Suggested fix

- Mark `hooks` as `@Volatile` (or move it into a `MutableStateFlow<List<TransactionHook>>` for
  observability).
- Document that `addHook` must complete before transactions are fired, or take a snapshot at construction
  time and disallow `addHook`.
