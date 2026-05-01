# iOS `FIOSResetSerialBleApiImpl` polling flow crashes on transient read failure

## Type
infrastructure

**Severity:** medium

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/iosMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/ios/serial/FIOSResetSerialBleApiImpl.kt` (lines 30-39)

## Summary

```kotlin
private val requestCounterFlow = flow {
    while (currentCoroutineContext().isActive) {
        val counter = fPeripheralApi
            .readValue(config.serialConfig.resetCharUuid)
            .toRequestCounter()
        emit(counter)
        delay(POLLING_RESET_INTERVAL)
    }
}.shareIn(scope, SharingStarted.Eagerly, 1)
```

`fPeripheralApi.readValue` (in `FPeripheralGattIO.readValue`) throws on:

- `WRITE_ACK_TIMEOUT_MS` timeout (`TimeoutCancellationException`),
- `failRead` (`Exception(error.localizedDescription)`),
- `cancelPending` (`CancellationException`).

Any of these tears down the entire poll loop and the `shareIn`'d
`SharedFlow` completes. From that point, `reset()` will hang indefinitely:

```kotlin
override suspend fun reset() {
    fPeripheralApi.writeValue(... = 0.toUInt32ByteArray())
    requestCounterFlow.filter { it == 0 }.first()      // never resumes
    info { "Reset success" }
}
```

The Android sibling (`FResetSerialBleApiImpl`) wraps the read in
`runSuspendCatching` and emits null on failure (filtered downstream), so
it survives transient failures.

## Reproduction

1. Trigger a one-off BLE read failure on the reset characteristic
   (latency burst, encryption renegotiation, etc.) so `failRead` fires.
2. The `requestCounterFlow` rethrows; `shareIn` propagates the exception.
3. Subsequent `reset()` calls never observe a `0` counter — they hang
   until the surrounding coroutine is cancelled.

## Root cause

Missing error handling — feature parity with the Android implementation
was not maintained.

## Impact

- After any single failed reset-counter read, the `FHttpBLEEngine`'s
  `reset()` path becomes a permanent deadlock.
- Combined with `FHttpBLEEngine`'s `serialApi.reset(); requestCount = 0`
  inside `parseRawHttpResponse` on parser error, this means a single
  malformed response can deadlock the BLE HTTP path until the connection
  is rebuilt.

## Suggested fix

Mirror the Android implementation:

```kotlin
private val requestCounterFlow = flow {
    while (currentCoroutineContext().isActive) {
        val counter = runSuspendCatching {
            fPeripheralApi.readValue(config.serialConfig.resetCharUuid)
                .toRequestCounter()
        }.getOrNull()
        if (counter != null) emit(counter)
        delay(POLLING_RESET_INTERVAL)
    }
}
    .shareIn(scope, SharingStarted.Eagerly, 1)
```

Also use `SharingStarted.WhileSubscribed()` so the polling does not run
unconditionally for the lifetime of the connection.
