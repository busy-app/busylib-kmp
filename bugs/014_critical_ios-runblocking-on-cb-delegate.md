# `runBlocking` inside `CBPeripheral` delegate callback can deadlock the BLE thread

## Type
infrastructure

**Severity:** critical

**Files (lines):**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/iosMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/ios/peripheral/FPeripheralValueRouter.kt` (lines 51-96)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/bridge/transport/ble/impl/src/iosMain/kotlin/net/flipper/bridge/connection/transport/ble/impl/ios/peripheral/FPeripheral.kt` (line 239 — caller)

## Summary

`FPeripheralValueRouter.didUpdateValue` is invoked synchronously from
`FPeripheralDelegate.peripheral(_:didUpdateValueForCharacteristic:error:)`,
which CoreBluetooth dispatches on the queue passed to `CBCentralManager.init`
(here it is `null`, i.e. the **main queue**). The router wraps its body in
`runBlocking { … }`, where it then calls `Channel.send` on `_rxDataChannel`
and `_streamingDataChannel` (capacity 2048 each, suspending on overflow) and
`gattIO.completeRead(...)` (which schedules a coroutine on `scope`).

Anything that requires the main queue to make progress while the router is
blocked — another CoreBluetooth callback, a UIKit hop, a `dispatch_async`
back to main, even Kotlin/Native's `WorkerBoundReference` machinery on the
main thread — will deadlock. As soon as `_rxDataChannel` fills up (consumers
slower than producer; e.g. `ByteEndlessReadChannel` paused on `awaitContent`
while the HTTP engine reset spins), `channel.send` suspends inside
`runBlocking`, parking the BLE callback queue indefinitely.

## Reproduction

1. Connect over BLE to a busybar that floods the RX characteristic faster than
   the HTTP engine can drain it (e.g. during a long body read with the engine
   stalled on `parseRawHttpResponse`).
2. RX channel hits 2048 buffered chunks.
3. The next `peripheral:didUpdateValueForCharacteristic:` runs `runBlocking`,
   which suspends inside `_rxDataChannel.send(payload)`.
4. CoreBluetooth's main-queue delegate is now blocked. No further BLE
   callbacks (write ack, disconnect, state update) can be delivered. Pending
   `withTimeout(WRITE_ACK_TIMEOUT_MS)` writes time-out instead of completing,
   the user-visible app freezes.

## Root cause

Calling `runBlocking` from within a callback that is owned by an
event-driven, single-threaded queue (CoreBluetooth's delegate queue) is
unsafe. The body suspends on a bounded `Channel.send` while the queue that
would normally drain the channel is exactly the one that is blocked.

## Impact

- Hard hang of CoreBluetooth: no further BLE traffic in or out.
- Spurious `WRITE_ACK_TIMEOUT_MS` write timeouts.
- App-level freeze if app shares the main queue.
- Recovery requires app restart or BT-toggle (peripheral disconnect cannot
  even be processed because `didDisconnect` would also be queued on the
  blocked main queue).

## Suggested fix

Do not block in the delegate callback. Replace `runBlocking { … }` with
either:

- `Channel.trySend(...)` plus a sane drop / overflow policy (e.g. `Channel(
  Channel.UNLIMITED)` already used elsewhere, or `Channel(BUFFERED,
  BufferOverflow.DROP_OLDEST)`), or
- forward the callback into an internal `Channel<DelegateEvent>` and process
  it on a dedicated `CoroutineScope`, mirroring `FCentralManagerDelegate`'s
  approach (`Channel.UNLIMITED` + `for(event in delegate.events)` consumer).

While here, consider also passing a non-main `dispatch_queue_t` to the
`CBCentralManager` initializer (`centralManagerProvider`) so callbacks never
contend with UI work even on the consumer side.
