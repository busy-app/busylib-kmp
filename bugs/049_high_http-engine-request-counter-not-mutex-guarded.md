# `FHttpBLEEngine.requestCount` is mutated outside the engine mutex on the parser-error path

## Type
infrastructure

**Severity:** high

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/FHttpBLEEngine.kt` (lines 45, 83-91, 100-113)

## Summary

`FHttpBLEEngine` uses a plain `private var requestCount = 0` and serialises
mutation under `mutex.withLockResult(...)` in the happy `execute` path:

```kotlin
val result = withLockResult(mutex, "execute") {
    checkRequestCountUnsafe()      // mutates requestCount under the lock
    …
    parseRawHttpResponse(channel = channel, …)   // ALSO mutates requestCount, but is called inside the lock
}
```

`parseRawHttpResponse` may execute `serialApi.reset(); requestCount = 0`
when the body is malformed. While that *particular* call is reached from
inside the lock (so safe today), the field itself is non-volatile and is
also referenced by `checkRequestCountUnsafe()` *after* a previous
`parseRawHttpResponse` has just reset it — but only under the same lock
acquisition. The bigger issue is:

- **The field is reachable across threads without `@Volatile`/atomic.** The
  Kotlin Multiplatform compiler cannot give you JMM-style visibility for a
  plain `var Int` on Apple. The engine's `Mutex` is a non-reentrant suspend
  mutex that does not establish JVM/native happens-before across worker
  threads for unrelated reads (the `parseResponse` path internally suspends
  to `dispatch_queue` for I/O), so a write made on worker A inside
  `parseRawHttpResponse` is not guaranteed to be visible to a read on
  worker B in the *next* `checkRequestCountUnsafe()`.
- **Any future caller of `parseRawHttpResponse` that runs outside the
  mutex** (e.g. test code, retry decorators, body-streaming callbacks) will
  silently corrupt the counter. There is no class-level invariant that the
  function is mutex-protected.

In addition, the second `requestCount = 0` reset path inside
`parseRawHttpResponse` does not re-acquire the lock to do so, and after
`serialApi.reset()` the `mutex` is still held by the original `execute`
call — but if `serialApi.reset()` is itself made re-entrant in the future
(`reset()` could legitimately call back into the engine), the lock becomes
a footgun.

## Reproduction

Hard to repro directly today thanks to defensive lock placement, but easy
to introduce a regression: any future refactor that releases the mutex
across the `parseResponse` await call (e.g. to allow concurrent reads)
will silently miscount.

## Root cause

Mutable shared state (`requestCount`) without an explicit atomic/
synchronization primitive that the type system can enforce.

## Impact

- Today: latent data-race risk on Kotlin/Native (no JMM); on JVM it works
  by accident because of `kotlinx.coroutines.sync.Mutex` happens-before
  guarantees.
- Tomorrow: any refactor that breaks the "all access is under `mutex`"
  invariant will produce off-by-one or "received request count > current
  request count" loops that trigger spurious `serialApi.reset()` calls
  (which lock the bus for ~5 seconds).

## Suggested fix

Use `kotlin.concurrent.atomics.AtomicInt` (already used in
`ByteEndlessReadChannel`) and make all reset paths go through a single
helper:

```kotlin
private val requestCount = AtomicInt(0)
…
private suspend fun checkRequestCountUnsafe() {
    val deviceRequestCount = serialApi.getRequestCounterFlow().first()
    if (requestCount.load() < deviceRequestCount) {
        serialApi.reset()
        requestCount.store(0)
    }
    requestCount.incrementAndFetch()
}
```

and ensure `parseRawHttpResponse` only calls a `resetCounter()` helper that
is permitted outside the mutex.
