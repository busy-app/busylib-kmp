# `Consumable.tryConsume()` Flow extension holds the consumable's mutex across `awaitClose()`

## Type
infrastructure

**Severity:** high

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktx/src/commonMain/kotlin/net/flipper/core/busylib/ktx/common/Consumable.kt` lines 52–59

## Summary
The convenience extension converts the callback-based `tryConsume(block)` into a `suspend Boolean` via `callbackFlow`/`first()`:

```kotlin
suspend fun Consumable.tryConsume(): Boolean {
    return callbackFlow {
        tryConsume { isConsumedSuccessfully ->
            send(isConsumedSuccessfully)
        }
        awaitClose()                       // ← suspends until the channel is closed
    }.first()
}
```

`MutexConsumable.tryConsume` invokes `block` **inside** `mutex.withLock`. The block here is `send(...) ; awaitClose()` — `awaitClose()` only completes when `first()` cancels the channel. Therefore the mutex **stays held** until the consuming `first()` has finished tearing the flow down and the cancellation propagates back through `awaitClose`. During that window every other caller is blocked on the mutex even though the consumable has already been logically consumed.

## Repro
```kotlin
val consumable = MutexConsumable()
val mark = MutableStateFlow(0)

launch {
    consumable.tryConsume()              // Coroutine A
    mark.value = 1
}
launch {
    consumable.tryConsume()              // Coroutine B – blocked on mutex until A's awaitClose returns
    mark.value = 2
}
```
B sits on `mutex.withLock` until A exits `callbackFlow`'s teardown, which happens after `first()` consumes the value AND the parent's structured-concurrency unwind reaches `awaitClose()`. With slow dispatchers, this is observably long.

## Root Cause
- `MutexConsumable` runs `block(...)` *while* holding the mutex, on the assumption the block returns quickly. The Flow extension violates that assumption: its `block` is essentially "suspend forever".
- `awaitClose()` is meant for ProducerScope cleanup, not as a return signal.

## Impact
- Mutex is held longer than necessary; competing callers wait for the entire consumer's flow tear-down.
- If the caller of `tryConsume()` is itself slow to take the value (`first()` hits a slow dispatcher), all subsequent `tryConsume()` calls suspend until then, effectively serialising a non-blocking operation.
- The pattern obscures intent — readers naturally assume `tryConsume()` returns immediately.

## Suggested Fix
Use a `CompletableDeferred` instead:
```kotlin
suspend fun Consumable.tryConsume(): Boolean {
    val deferred = CompletableDeferred<Boolean>()
    tryConsume { ok -> deferred.complete(ok) }
    return deferred.await()
}
```
`tryConsume(block)` invokes the block synchronously inside the mutex, the deferred completes immediately, and the mutex is released immediately. No `callbackFlow`/`awaitClose` plumbing required.
