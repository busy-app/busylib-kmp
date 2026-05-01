# Android serial subscription `Waiter` has a non-atomic field + buffer-less channel race

## Type
infrastructure

**Severity:** high

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/androidMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/api/serial/FSerialUnsafeApiImpl.kt` (lines 105-121)
- caller: same file, lines 71-87 (`sendBytes`)

## Summary

```kotlin
private class Waiter<T>(initial: T) {
    private var value: T = initial
    private val channel = Channel<T>()        // capacity 0, RENDEZVOUS

    suspend fun set(value: T) {
        this.value = value
        channel.trySend(value)                // dropped if no one is suspended in receive()
    }

    suspend fun waitUntil(block: (T) -> Boolean) {
        while (!block(value)) {
            if (block(channel.receive())) {
                return
            }
        }
    }
}
```

Two problems:

1. **Lost wake-up.** `set(true)` is called inside the rx-subscription
   coroutine in `init { … }`. The first user calling `sendBytes` will reach
   `isSubscribed.waitUntil { it }`. If `set(true)` runs *before* any
   `waitUntil` call (very common — subscription completes long before the
   first send) the rendezvous channel drops the value (no receiver), and
   the very-next reader will block on `channel.receive()` forever, because
   the snapshot loop only re-checks `value` after a `channel.receive()`
   returns. The first read of `value` succeeds (because plain Kotlin
   `var` lacks any cross-thread happens-before guarantee on iOS, but on the
   JVM happens to be visible), so on JVM the bug is masked, but on
   reorderings (e.g. when the suspension point lands the resumption on a
   different worker thread than the writer) it can hang.

2. **Non-atomic `value` field.** It is a plain `var T`. Reads and writes are
   not synchronised — there is no `@Volatile`, no atomic, no mutex. This is
   a data-race per the JMM. Visibility is not guaranteed even for a
   `Boolean`.

3. **`set` has `suspend` but never suspends.** It performs `trySend` and
   returns. Yet the API masquerades as if it might propagate back-pressure —
   future maintainers would think it does.

## Reproduction

1. Create `FSerialUnsafeApiImpl` with a `rxCharacteristic` flow that emits
   the characteristic immediately.
2. Wait for one frame so the rx subscribe completes (`set(true)` runs).
3. Now call `sendBytes(...)` for the first time.
4. On a non-strong memory model platform (or under
   `-Xdebug-mode-coroutines`) the call may hang on `channel.receive()`
   because the only `set(true)` was dropped by the rendezvous channel.

## Root cause

Mismatched primitives: a buffered/`StateFlow`-style "latest known value" is
needed, but the implementation is a value field plus a 0-capacity
`Channel`. On most happy-path runs `block(value)` returns true on the first
iteration, hiding the bug; under concurrent ordering it does not.

## Impact

- Sporadic hangs of the first `sendBytes` after subscription, especially in
  CI / on slower devices.
- Once hung, the FHttpBLEEngine `execute()` request is locked behind the
  mutex and the entire BLE HTTP path is dead.

## Suggested fix

Replace `Waiter` with `MutableStateFlow<T>`:

```kotlin
private val isSubscribed = MutableStateFlow(false)
…
isSubscribed.first { it }  // in sendBytes
isSubscribed.value = true   // in subscribe collect
```

Or drop the `Waiter` entirely and combine the rx flow with the tx flow so
the writer naturally awaits the latest non-null `RemoteCharacteristic`.
