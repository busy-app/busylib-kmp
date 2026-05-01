# Reconnect loop introduces a 1 s delay on the very first attempt and on every successful reconnect cycle, briefly flapping combined status to a lower priority

## Severity
medium

## Type
lack-of-feature

## Files
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/connections/AutoReconnectConnection.kt:44-77`

## Summary
After every `Disconnected` event observed by `AutoReconnectConnection`, the loop calls:

```kotlin
connection.disconnect()
delay(getExponentialDelay(retryCount)) // initialDelay = 1.seconds, retryCount=0 -> 1s
retryCount++
```

`retryCount` is reset to `0` whenever the wrapped connection becomes `Connected`, so the **first reconnect after a clean drop always waits 1 s** before constructing a new wrapped connection. During that 1 s window the child's `stateFlow` value is `Disconnected` and no other `Connecting`/`Connected` is published.

For combined transports, this means the merged status temporarily falls to whatever the **other** transports report. If only one transport is configured, the combined status drops to `Disconnected` (priority 0) for ~1 s on every drop.

Note also: the very first iteration of the loop (initial connect) does **not** delay — the body of the `while (isActive)` runs `withLockResult { … }` and then enters the `.first()` wait — but **on every successful connect→drop transition** the `delay(1.seconds)` fires before the next attempt.

## Repro
1. Single-transport combined config (e.g. only LAN). Connect successfully.
2. LAN drops.
3. Observe listener: `Connected → Disconnected (1s) → Connecting → Connected`.

Compared with a "no-delay first retry" scheme, this introduces 1 s of avoidable downtime per drop event for every BUSY Bar consumer.

## Root Cause
`getExponentialDelay(retryCount = 0, initialDelay = 1.seconds, factor = 2.0)` returns `1.seconds * 2^0 = 1.seconds`. The reset to `retryCount = 0` happens correctly, but the formula evaluates to a non-zero floor.

## Impact
- 1 s of perceived downtime on every reconnect, even for healthy transports that drop briefly (e.g. BLE GATT MTU renegotiation).
- For combined BLE+LAN setups, both transports flapping in opposite directions still end up briefly showing the lower transport, exposing the user to flicker.

## Suggested Fix
Make the first retry attempt immediate, e.g.:

```kotlin
if (retryCount > 0) {
    delay(getExponentialDelay(retryCount - 1))
}
retryCount++
```

Or change `getExponentialDelay` semantics so retryCount=0 returns `Duration.ZERO`. Add a unit test to lock in "first reconnect is immediate".
