# `FTransportMetaInfoApiImpl.get` returns `flowOf(Result.success(innerFlow))` — each subscriber spawns a fresh `flatMapLatest`

## Type
infrastructure

**Severity:** low

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/androidMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/meta/FTransportMetaInfoApiImpl.kt` (lines 33-41)

## Summary

```kotlin
override fun get(key: TransportMetaInfoKey): Flow<Result<Flow<TransportMetaInfoData?>>> {
    val address = metaInfoGattMap[key]
        ?: return flowOf(Result.failure(RuntimeException("Can't found provider for $key")))

    val innerFlow = services.flatMapLatest {
        getFlow(it, address)
    }
    return flowOf(Result.success(innerFlow))
}
```

Two issues:

1. The signature `Flow<Result<Flow<…>>>` (a flow of a result of a flow) is
   confusing and only ever emits a single element. A `Flow<Result<Flow<T>>>`
   that always emits exactly one value should just be a `suspend fun
   getValue(...): Result<Flow<T>>`.

2. The returned `innerFlow` is built with `flatMapLatest` over `services`.
   Each call to `get(key)` returns a *new* lazy flow value. If a
   consumer collects the same key twice (e.g. one collector for the UI,
   another for telemetry), every collection drives an *independent*
   `flatMapLatest` and an independent characteristic `read()` /
   `subscribe()` chain. For `read`-only meta characteristics this is fine;
   for `INDICATE`/`NOTIFY`-based ones we end up with two separate
   `subscribe` calls for the same characteristic, which on the Nordic
   client library quietly stacks duplicate notify-enables.

   Worse: after the first collector cancels, the second collector's
   subscription remains active — the underlying `setNotifyValue(true)` is
   never paired with a `setNotifyValue(false)` on disconnect. (The
   notification subscription stays armed across `disconnect()` /
   `connect()` cycles, defeating any attempt to clean up.)

## Reproduction

1. Subscribe to `BATTERY_LEVEL` from two consumers.
2. Disconnect.
3. The Android Bluetooth stack reports two pending notification listeners
   for the same characteristic, and a CCCD write may fail with
   `STATUS_ALREADY_NOTIFYING` on the next reconnect.

## Root cause

API shape encourages independent subscriptions; nothing guards against
duplicate notify-enables.

## Impact

- Wasted GATT bandwidth on duplicate notifies.
- Potential CCCD-write failure on reconnect.

## Suggested fix

- Cache the `innerFlow` per key with `shareIn(scope, WhileSubscribed())`
  so multiple collectors share a single GATT subscription.
- Consider reshaping the API to `suspend fun get(key): CResult<Flow<…>>`
  to remove the spurious outer `Flow<Result<…>>`.
