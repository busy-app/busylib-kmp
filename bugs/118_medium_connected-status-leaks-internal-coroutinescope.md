# medium — `FInternalTransportConnectionStatus.Connected` leaks the SDK's internal `CoroutineScope` to listeners

## Severity
medium

## Type
infrastructure

## Files
- `components/bridge/transport/common/api/src/commonMain/kotlin/net/flipper/bridge/connection/transport/common/api/FInternalTransportConnectionStatus.kt:16-26`

## Summary
The `Connected` data class carries a public `val scope: CoroutineScope` field. Every listener that receives a `Connected` status thus gets a handle to the SDK's connection-bound scope. There is no clear contract about what the listener is allowed to do with it — and any listener that launches work in that scope (a tempting "do my work as long as the device is connected" idiom) creates two real problems:

1. **Lifecycle bleed.** When the connection ends, the SDK cancels the scope; user-launched coroutines die abruptly and unexpectedly. When the connection re-establishes, the *new* connection rebuilds with a *different* scope, so the listener's previously-launched work is gone but the listener still holds a reference to the dead scope.
2. **Cancellation bleed in the other direction.** A user who launches a job that throws an unhandled exception in this scope (e.g. `scope.launch { error("oops") }`) propagates the throw to the connection scope's parent Job and brings down the entire connection — see also the related listener bug.

In short, the SDK is exporting an internal lifecycle primitive across its API boundary.

## Reproduction / scenario
1. Listener receives `Connected(scope, deviceApi, types)` and does
   ```kotlin
   scope.launch { collectTelemetry() }
   ```
2. Connection drops; SDK cancels `scope`; `collectTelemetry()` is killed mid-write.
3. Listener gets a *new* `Connected` with a *different* scope (re-connect path through `AutoReconnectConnection` produces fresh wrapper objects). Listener doesn't realise and re-launches; eventually a stale reference to the old scope is held while a new one is active.

## Why it happens
The status type was defined to mirror the SDK's internal model. There was no separate "public" view, so the internal scope is part of the public payload.

## Impact
- Encourages callers to attach work to a scope they don't own.
- Makes lifecycle reasoning across SDK/host boundary undefined.
- In conjunction with the listener-cancellation bug, makes every listener a potential source of "the connection silently died" outages.

## Suggested fix
- Remove `scope` from the public `Connected` payload. If listeners truly need a connection-scoped lifecycle, expose a `connectionLifecycle: WrappedFlow<Lifecycle>` (started/stopped tokens) instead of a raw `CoroutineScope`.
- Alternatively, split `FInternalTransportConnectionStatus` into an internal-only model and a sanitised public model. Even the type's name (`FInternal...`) hints that it shouldn't have been the listener payload.
