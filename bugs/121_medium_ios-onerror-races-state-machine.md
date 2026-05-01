# `FPeripheral.onError` launches state transitions on `scope` that race with `onDisconnect`

## Type
infrastructure

**Severity:** medium

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/iosMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/ios/peripheral/FPeripheral.kt` (lines 151-199)

## Summary

```kotlin
override fun onError(error: NSError) {
    …
    when (domain) {
        "CBATTErrorDomain" -> handleCBATTError(code)
        "CBErrorDomain" -> handleCBError(code)
    }
}

private fun handleCBError(code: Long) {
    scope.launch {
        when (code) {
            CBErrorPeerRemovedPairingInformation ->
                _stateStream.emit(FPeripheralState.INVALID_PAIRING)
            CBErrorEncryptionTimedOut ->
                _stateStream.emit(FPeripheralState.INVALID_PAIRING)
        }
    }
}
```

Two issues:

1. The state transition is fire-and-forget on `scope.launch`. By the time
   it runs, `FCentralManager.didFailToConnect` may already have
   completed `_connectedStream.update { it - peripheral.identifier }` and
   nothing prevents `_stateStream` from emitting `INVALID_PAIRING` *after*
   `onDisconnect` has emitted `DISCONNECTED` (or vice versa). The order
   in which `onDisconnect` and `onError` run is determined by the
   `delegate.events` channel ordering, but `onError` jumps onto a
   different coroutine via `scope.launch`, escaping that ordering.

2. `onError` is invoked from `FPeripheralValueRouter.didUpdateValue` (a
   delegate callback path, *not* via `delegate.events`), so it executes
   on the CB delegate queue. The `scope.launch` then dispatches to the
   coroutine context attached to the bleApi scope — which is fine — but
   the resulting state ordering is still
   `DISCONNECTED → INVALID_PAIRING` (or vice versa) depending on
   scheduler whim.

The visible symptom is `FPeripheralLifecycleTest:64-76` having to use
`first { it == ... }` to await the transition: the test acknowledges that
the state transition is asynchronous to the call, which already implies
that fast-following calls (e.g. a `disconnect` issued before the launched
coroutine runs) will overtake it.

## Reproduction

1. Trigger an authentication error: device deletes pairing.
2. The router calls `onError` ⇒ schedules `INVALID_PAIRING` emit.
3. Concurrently `FCentralManager.didDisconnect` runs on the events
   channel, which calls `device.onDisconnect()`.
4. `onDisconnect` checks
   `stateStream.value == PAIRING_FAILED || INVALID_PAIRING` and bails out
   *only if* the launched coroutine in (2) has already run. There is no
   `await` — the order is non-deterministic.

If `onDisconnect` wins, it clears state to `DISCONNECTED`, `metaInfoKeysStream`,
channels, etc., and then `INVALID_PAIRING` arrives later, "resurrecting"
the meta map (no — meta is already cleared) but flipping state
non-monotonically.

## Root cause

State transitions for error events use a different scheduling path
(`scope.launch`) than other peripheral state transitions
(`emit`-from-delegate-event-channel).

## Impact

- Non-deterministic state ordering: consumers may see
  `INVALID_PAIRING` *after* `DISCONNECTED`, breaking the assumption that
  `INVALID_PAIRING` is a terminal failure mode of the connect path.

## Suggested fix

Route `onError` through the same `delegate.events` channel as other
events so ordering is preserved, **or** make `onError` a `suspend fun`
and call it directly without `scope.launch`. A simpler option:

```kotlin
override fun onError(error: NSError) {
    val target = when (...) { ... } ?: return
    _stateStream.update { current ->
        if (current == FPeripheralState.DISCONNECTED) current else target
    }
}
```

(`MutableStateFlow.update` is non-suspending and atomic — no scope hop
needed.)
