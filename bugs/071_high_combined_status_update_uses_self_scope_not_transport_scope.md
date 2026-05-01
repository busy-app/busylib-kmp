# Combined `Connected` status is emitted with the outer caller scope, hiding per-transport scope from consumers

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/FCombinedConnectionApiImpl.kt:80-92`

## Summary
`startCollectTransportStatusUpdateJob` rebuilds the `Connected` event with `scope = scope` (the outermost caller-supplied scope) and `deviceApi = this` (the combined api itself), discarding the actual `Connected.scope` the upstream merger produced. Anything tying its lifecycle to `Connected.scope` (e.g. flows started in a feature with `launchIn(connected.scope)`) will keep running until the *whole* combined transport tears down, instead of stopping when the underlying transport flips state.

## Repro
1. A consumer subscribes to events in `Connected.scope` — for example, `launchIn(connected.scope)` in a feature that starts whenever combined goes Connected.
2. The underlying transport disconnects, the combined snapshot loses one transport. `Connected.scope` should be cancelled so the feature's job stops.
3. Because the emitted `Connected.scope` is the *outer* scope (which is still alive — only the failing transport's individual scope died), the feature's job never stops. It now lives across multiple connect/disconnect cycles, even though it logically belongs to the prior connection.

## Root Cause
The status job replaces the inner scope and deviceApi:

```kotlin
listener.onStatusUpdate(
    status = FInternalTransportConnectionStatus.Connected(
        scope = scope, // outer scope, not the transport's
        deviceApi = this,
        connectionTypes = transportConnectionStatus.connectionTypes
    )
)
```

This was probably done so consumers can observe the combined api as the deviceApi, but it forfeits the lifecycle semantics that other transports rely on.

## Impact
- Subscribers of `Connected.scope` leak across connection cycles.
- Lifecycle-scoped resources (collectors, retries, etc.) outlive the transport they were created for.
- Inconsistent with how single-transport `Connected.scope` behaves elsewhere in the codebase, which can mask issues that surface only in combined mode.

## Suggested Fix
Maintain a child `SupervisorJob` that is cancelled and replaced whenever the merged status leaves `Connected`, and pass that scope as `Connected.scope`. For example:

```kotlin
private var connectedScope: CoroutineScope? = null
// in startCollectTransportStatusUpdateJob, on transition into Connected:
val newScope = CoroutineScope(SupervisorJob(scope.coroutineContext.job) + scope.coroutineContext)
connectedScope?.cancel()
connectedScope = newScope
listener.onStatusUpdate(Connected(newScope, this, ...))
// on transition out of Connected:
connectedScope?.cancel(); connectedScope = null
```
