# low — Mutable `currentConfig` field is not `@Volatile` / not under lock

## Severity
low

## Type
infrastructure

## Files
- `components/bridge/transport/tcp/lan/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/FLanApiImpl.kt:22`
- `components/bridge/transport/tcp/cloud/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/FCloudApiImpl.kt:25`

## Summary
`private var currentConfig: FLanDeviceConnectionConfig` is read by:
- `tryUpdateConnectionConfig(...)` (suspend, reads multiple times)
- `deviceName` getter
- `connectionMonitor` initialization, `_capabilities` initialization

Multi-thread access is possible (Swift / Android might call `deviceName` from a UI thread while `tryUpdateConnectionConfig` runs on another). Without `@Volatile` or a Mutex, JVM permits torn reads (technically safe for references on JVM but not portable across native targets) and on Native (Kotlin/Native memory model since 1.7+) the assumption is no longer "safe by default" — concurrent mutation may produce inconsistent reads on Apple targets.

## Repro
Hard to repro without race injection, but theoretically possible.

## Root Cause
Missing concurrency annotations.

## Impact
Low — most call sites are quick reads of a string. But violates portability.

## Suggested Fix
Either:
- `private val currentConfig = MutableStateFlow(initial)` and read via `.value`; OR
- Store under a Mutex and serialize all accesses; OR
- Use an `AtomicRef` from `kotlinx.atomicfu`.
