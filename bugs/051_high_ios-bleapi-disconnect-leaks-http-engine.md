# `FIOSBleApiImpl.disconnect()` does not close the embedded `FHttpBLEEngine`

## Type
infrastructure

**Severity:** high

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/iosMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/ios/api/FIOSBleApiImpl.kt` (lines 89-91, vs. Android lines 114-117)

## Summary

`FAndroidBleApiImpl.disconnect()` properly tears down both:

```kotlin
override suspend fun disconnect() {
    peripheral.disconnect()
    bleHttpEngine.close()
}
```

`FIOSBleApiImpl.disconnect()` only calls the externally-provided
`onDisconnect` callback (which routes to
`centralManager.disconnect(peripheral.identifier)`) and **never closes the
HTTP engine**:

```kotlin
override suspend fun disconnect() {
    onDisconnect()
}
```

`FHttpBLEEngine.close()` cancels the `HttpClientEngineBase` parent scope.
Without it, every `bleApi.disconnect()` on iOS leaks the engine: any
in-flight `execute(...)` continues holding the mutex, the consumer's
`HttpClient` sees the engine as alive, and the next `connect()` builds a
*new* `FHttpBLEEngine` while the old one still queues responses on the
(now closed) `ByteEndlessReadChannel` from the previous session.

## Reproduction

1. iOS connect → start an HTTP request → `await disconnect()`.
2. Old `FHttpBLEEngine` instance survives because nothing cancels its
   `HttpClientEngineBase` scope.
3. Reconnect → the SDK builds a new engine; the old one's coroutine scope
   is still active, holding references to the previous `FSerialBleApi` and
   `Mutex`.

## Root cause

Asymmetric implementations between Android and iOS — the iOS version was
not updated to mirror the Android cleanup contract.

## Impact

- Resource / coroutine-scope leak per disconnect on iOS.
- Pending HTTP `execute(...)` calls on a disconnected device do not get
  freed; they stay parked on the closed channel.

## Suggested fix

```kotlin
override suspend fun disconnect() {
    onDisconnect()
    bleHttpEngine.close()
}
```

(It is safe to call `close` after the underlying serial channel is closed
— `close()` only cancels the engine's scope.)
