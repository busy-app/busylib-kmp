# low — No retry primitive used anywhere in this transport — violates project guideline

## Severity
low (the missing-retry impact is captured in the `critical_no-reconnect-...` bug; this entry tracks the project-rule violation)

## Type
lack-of-feature

## Files
- `components/bridge/transport/tcp/common/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/common/monitor/WSEventsDeviceMonitor.kt`
- `components/bridge/transport/tcp/lan/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/streaming/FLanStreamingApiImpl.kt`
- `components/bridge/transport/tcp/lan/impl/src/appleMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/monitor/FAppleLanConnectionMonitor.kt`

## Summary
AGENTS.md:
> "Retries: use `exponentialRetry { ... }` from `core:ktx` rather than hand-rolling loops."

Search of these directories yields zero usages of `exponentialRetry`. Each module either:
- has no retry at all (WSEventsDeviceMonitor, FLanStreamingApiImpl), or
- relies on Network.framework's opaque internal retry (FAppleLanConnectionMonitor — calls `restartMonitoring()` immediately, no backoff).

Without backoff on the Apple monitor:
1. Server unreachable.
2. State_failed → `restartMonitoring()` → `createConnection()` → state_failed → … tight loop with `SKIP_IF_RUNNING` providing the only friction.
3. CPU + log churn.

## Root Cause
Reusable retry primitive exists; not applied here.

## Impact
- CPU-burning reconnect loops on flapping networks.
- Inconsistent retry semantics across transports.

## Suggested Fix
Wrap the relevant connect/monitoring loops with `exponentialRetry(initial = 1.s, max = 30.s, factor = 2.0) { ... }`. For the Apple monitor, gate `restartMonitoring()` body with a backoff timestamp if `exponentialRetry`'s suspend semantics don't fit the callback-driven design.
