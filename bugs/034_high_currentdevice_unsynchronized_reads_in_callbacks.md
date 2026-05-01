# `currentDevice` is read/written without synchronization across coroutines

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceOrchestratorImpl.kt:65, 89, 134, 144`
- `components/bridge/orchestrator/impl/src/commonMain/kotlin/net/flipper/bridge/connection/ble/impl/FDeviceOrchestratorImpl.kt:118-125`

## Summary
`currentDevice: FDeviceHolder<*>?` is a non-volatile, non-atomic mutable field. It is read and
written from multiple coroutines that *almost always* go through the orchestrator's mutex – but
not 100%. In particular:

- `FDeviceOrchestratorImpl` is a singleton (`@SingleIn(BusyLibGraph::class)`).
- The mutex protects `connectIfNot`, `disconnectCurrent`, and the `withLock("disconnect_internal")`
  in `onInternalDisconnect`.
- However, the field is JIT-visible-only-via-mutex on JVM/Kotlin, but on Kotlin/Native (iOS,
  macOS targets), reads and writes of object references across threads without explicit
  synchronization are not guaranteed to be visible in any particular order. The mutex *does* act
  as a happens-before barrier on Native too, but only because acquiring the mutex involves a
  `compareAndSet`. As written, the code happens to be correct on Native by virtue of the mutex,
  but anyone reading the field outside `withLock` (for debugging, logging, etc.) gets undefined
  behavior.

The more concrete issue: the listener and exception-handler callbacks invoked from the device
holder run on the holder's own scope (a separate coroutine). They invoke `onInternalDisconnect`,
which schedules a `globalScope.launch { withLock { ... } }`. Inside that lock, `currentDevice` is
read and compared via `===` to the captured `deviceHolder`. The capture is consistent with the
field that was set inside `connectIfNot`'s mutex critical section, *but only after the mutex
release in `connectIfNot` happens-before the lock acquisition in the cleanup launch*. That is
guaranteed by Mutex's fair acquisition order, so it works.

The fragile case: the listener callback runs *during* `connectIfNot`'s critical section (e.g., the
holder's `async` immediately calls back). The listener path calls `localTransportListener.
onStatusUpdate(config, status)` directly (line 99) for non-Disconnected statuses, which writes to
`localTransportListener.state`. This is a `MutableStateFlow` – thread-safe. So that is fine. But
the assignment of `currentDevice = deviceHolderFactory.build(...)` happens *after* the holder is
constructed and after any synchronous listener invocations that were performed during
construction. If the holder's `init` runs the `async` and that `async` synchronously invokes the
listener (which is unusual but possible if `deviceConnectionHelper.connect` returns instantly),
`listener.invoke(this, ...)` runs with `this = deviceHolder` *before* the orchestrator field
`currentDevice` has been assigned. The listener for the "non-Disconnected" branch is fine. The
`Disconnected` branch calls `onInternalDisconnect(deviceHolder)`, which queues a cleanup; that
cleanup eventually runs after the mutex is released, by which time `currentDevice` *has* been
assigned, and the referential check works.

But the `exceptionHandler` path is asynchronous: a `CoroutineExceptionHandler` is invoked on
whichever dispatcher reports the failure. Its body calls `onInternalDisconnect(deviceHolder)`,
which `globalScope.launch`es. If the exception fires synchronously during construction (e.g., the
`async`'s first suspension throws), then by the time `connectIfNot` returns, `currentDevice` was
already set – and the cleanup launch correctly nukes it.

## Reproduction / scenario
The race that *is* real:

1. `connectIfNot(deviceA)` is running. Holder built, `currentDevice = holder` set, mutex released.
2. Holder's async fires `onConnectError` synchronously (e.g., an immediate exception from
   `deviceConnectionHelper`). The error handler queues `globalScope.launch { withLock(...) }`.
3. The user immediately invokes `connectIfNot(deviceB)`. Mutex acquired (the queued launch from
   step 2 was scheduled *after* the user's `connectIfNot` if the user's call-site runs on the
   same dispatcher as the orchestrator's release).
4. `connectIfNot(deviceB)` proceeds, does `disconnectInternalUnsafe()` on holder A (good), creates
   holder B, sets `currentDevice = holderB`, releases mutex.
5. Now the queued launch from step 2 acquires the mutex. It calls
   `disconnectInternalUnsafe(holderA)`, which checks `currentDevice !== holderA` → true → skips.
   But it also calls `postAction()`, which is `localTransportListener.onErrorDuringConnect(config,
   error)` – on the **old** listener for device A.
6. The old listener's MutableStateFlow updates to `Disconnected(ERROR_UNKNOWN)`. No subscribers –
   silently swallowed by the new listener.

So the *cleanup* path works correctly. But the analysis depends on every code path going through
the mutex, and on referential equality in `disconnectInternalUnsafe` being authoritative. If
anyone in the future reads `currentDevice` outside the mutex (e.g., adding a `fun
isConnected(): Boolean = currentDevice != null` for diagnostics), they will see torn state.

## Impact
- Latent: the code works today only because of the mutex serialization. A future change that adds
  a non-`withLock` read of `currentDevice` will silently introduce data races.
- The `===` check is brittle in a multi-target codebase – any wrapping or copying of `FDeviceHolder`
  reference would invalidate the equality.

## Suggested fix
- Make `currentDevice` private and access it only inside the mutex. Add a single helper
  `private fun currentDeviceOrNull(): FDeviceHolder<*>? = currentDevice` that is `private`, and
  make sure all reads go through `withLock`.
- Or wrap it in `kotlinx.atomicfu.AtomicReference` to make the contract explicit.
