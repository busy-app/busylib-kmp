# `ByteEndlessReadChannel.cancel` uses CAS on `AtomicReference<Throwable?>` but discards subsequent causes

## Type
infrastructure

**Severity:** low

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/serial/ByteEndlessReadChannel.kt` (lines 33-75)

## Summary

```kotlin
private val _closedCause = AtomicReference<Throwable?>(null)
override val closedCause: Throwable?
    get() = _closedCause.load()

private val _isClosedForRead = AtomicBoolean(false)
override val isClosedForRead: Boolean
    get() = _isClosedForRead.load()

…

override fun cancel(cause: Throwable?) {
    info { "Cancel channel with cause: $cause" }
    channel.close(cause)
    coroutineContext.cancel(cause as? CancellationException)
    _isClosedForRead.compareAndSet(expectedValue = false, newValue = true)
    _closedCause.compareAndSet(expectedValue = null, newValue = cause)
}
```

Three minor problems:

1. `cancel` is *not* idempotent in a useful way: on a second call with a
   different cause, the new cause is silently discarded (because
   `compareAndSet(null, …)` fails on the second call). Callers cannot
   distinguish "already cancelled with reason X" vs "cancelled twice with
   reason Y, only X is visible" — but `channel.close(cause)` on the second
   call may also fail silently if the channel was already closed.

2. The order of operations is inverted: we close the channel and cancel
   the context *before* publishing `_isClosedForRead = true`. A reader
   running concurrently can observe `awaitContent` returning false (or
   throwing) without ever seeing `isClosedForRead == true`. In Ktor's
   contract, `isClosedForRead` should flip to true *before* readers
   observe the `ClosedReceiveChannelException`.

3. The exposed `coroutineContext` field is `public` (no visibility
   modifier on a `class` member ⇒ `public`). External callers can read
   `coroutineContext.cancel()` to corrupt the channel's lifecycle.

## Reproduction

A test that calls `cancel(IOException("first"))` and then
`cancel(IOException("second"))` will see `closedCause == "first"`, even if
the second cancellation was meaningfully different (e.g. a more specific
error after a generic one).

## Root cause

The atomicity guarantees were added defensively but the publication
ordering and idempotency contract were not specified.

## Impact

- Diagnostic noise (lost root cause) on double-close.
- Brief window where readers cannot tell that the channel is closed.

## Suggested fix

```kotlin
override fun cancel(cause: Throwable?) {
    if (!_isClosedForRead.compareAndSet(false, true)) return
    _closedCause.store(cause)
    channel.close(cause)
    coroutineContext.cancel(cause as? CancellationException)
}
```

Make `coroutineContext` `private` (consumers go via `cancel`).
