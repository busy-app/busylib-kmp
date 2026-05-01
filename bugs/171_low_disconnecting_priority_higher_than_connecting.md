# `getPriority` ranks `Disconnecting` (2) above `Connecting` (1), making the combined status report `Disconnecting` while another transport is actively `Connecting`

## Severity
low

## Type
broken-feature

## Files
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/ConnectionSnapshotMerger.kt:42-50`

## Summary
The priority order is `Disconnected (0) < Connecting (1) < Disconnecting (2) < Connected (3)`. With this order, when one transport is `Connecting` and another is `Disconnecting`, the combined status reports `Disconnecting`. A user-facing UI bound to this status will say "Disconnecting" even though we are simultaneously trying to bring up a different transport.

## Repro
1. BLE drops; AutoReconnectConnection on BLE briefly publishes `Disconnecting`.
2. LAN starts Connecting in parallel.
3. The merged snapshot reports `Disconnecting`.

## Root Cause
The semantics of "max priority status wins" assumed that the higher numeric priority is always "more useful" to the user. `Disconnecting` is a transient teardown state and is usually less actionable than `Connecting`.

## Impact
- UI flicker on transport handoff (BLE→LAN failover): "Disconnecting" briefly shown instead of "Connecting".
- Confusing for end users.

## Suggested Fix
Swap priorities so `Connecting > Disconnecting`:

```kotlin
return when (status) {
    Disconnected -> 0
    Disconnecting -> 1
    is Connecting -> 2
    is Connected -> 3
}
```

And update `ConnectionSnapshotMergerTest.getPriority returns correct ordering` accordingly. Also extend that test to lock in the BLE-disconnecting-while-LAN-connecting case at `mergeSnapshots`/`getCurrentConnectionSnapshotFlow` level.
