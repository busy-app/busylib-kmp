# `SharedConnectionPool` keeps collecting forever in caller-supplied scope (`SharingStarted.Eagerly`)

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/connections/SharedConnectionPool.kt:44-50`

## Summary
`SharedConnectionPool` calls `shareIn(scope, SharingStarted.Eagerly, 1)` against the user-owned scope passed into `connect(...)`. The shared upstream is never stopped by `disconnect()`. As a side effect, each wrapped connection's `getCapabilities()` flow also stays subscribed.

`SharingStarted.Eagerly` is incompatible with the API contract — `disconnect()` should fully release transport resources, but this collector lives until the caller cancels the parent scope.

## Repro
1. `connect(scope, ...)` succeeds and pool collects from connections.
2. Call `disconnect()`.
3. Call `connect(scope, ...)` again on the **same** scope (simulating a "reconnect to a different device" flow).
4. The previous pool's shared upstream is still active and still holds references to the previously created `AutoReconnectConnection`s. Because `AutoReconnectConnection.disconnect()` does not reset its `stateFlow`, the old pool can still emit a `Connected` snapshot.

Repeated connect/disconnect cycles accumulate pool subscribers and live HTTP-capability collectors.

## Root Cause
- `SharingStarted.Eagerly` ignores subscriber count — the upstream runs as long as the scope is alive.
- `disconnect()` (`FCombinedConnectionApiImpl`) does not cancel the upstream.
- `connectionPool` is not tied to a sub-scope; it is rooted directly in the public `scope`.

## Impact
- **Memory leak** every connection lifecycle: each `connect()` adds new collectors that survive `disconnect()`.
- **Stale capability propagation**: `_capabilities` (also computed from `connectionPool.get()`) keeps reporting the old transport's capabilities.
- Multiple consecutive `connect()` calls on the same caller scope multiply the load on per-connection capability flows.

## Suggested Fix
Either:

1. Use `SharingStarted.WhileSubscribed(stopTimeoutMillis = 0L, replayExpirationMillis = 0L)` so the upstream stops when the last consumer disappears; or
2. Create an internal child `SupervisorJob`-backed scope inside `FCombinedConnectionApiImpl` and use it for `shareIn`, then cancel that scope from `disconnect()`.

Option 2 is the more correct fix, because `disconnect()` should be a hard release.
