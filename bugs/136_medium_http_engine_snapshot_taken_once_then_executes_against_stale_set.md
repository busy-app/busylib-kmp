# `FCombinedHttpEngine.execute` fails over only against a stale snapshot

## Severity
medium

## Type
infrastructure

## Files
- `components/bridge/transport/combined/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/combined/impl/FCombinedHttpEngine.kt:36-91`

## Summary
`execute` reads the current set of delegates via `connectionPool.get().first()` once at the start of the request and iterates them. If a transport drops mid-request, no fresh transport from `connectionPool` is consulted — even if a previously-disconnected one came back online — and conversely, a transport that disappeared between snapshot and execute is still tried (returning a failure that the user could have skipped).

If `currentDelegates` is empty at snapshot time but a transport reconnects 1 ms later, the request fails immediately with `IllegalStateException("No connected devices")`.

Additionally, the `lastException` fallback uses `throw lastException ?: error("No delegates available")` after the loop. `lastException` is only set on `onFailure`, but if `runSuspendCatching` swallows a cancellation (it shouldn't but `runSuspendCatching` does propagate `CancellationException`), the loop's failure semantics are subtle.

## Repro
1. Single-transport combined config. Disconnect transient — happens after `connectionPool.get().first()` but before `delegate.getDeviceHttpEngine().execute(data)`.
2. The single delegate fails. `lastException` set. Loop ends with empty fallback set.
3. Throws `lastException`. The connection actually came back during the failure handler, but the engine never re-queries `connectionPool`.

Worse: imagine BLE drops just before snapshot; LAN is up. Snapshot returns `[LAN]`. Then LAN drops too while the request is in flight; BLE comes back. Snapshot is stale; the engine throws even though BLE is now usable.

## Root Cause
- `connectionPool.get().first()` is a one-shot read.
- No re-query inside the failover loop.

## Impact
- Spurious request failures during transient transport changes — exactly the scenario combined transports are supposed to fix.
- HTTP retries (e.g. via `exponentialRetry { ... }`) succeed on a later request, but only because the consumer calls `execute` again — within a single request, no re-resolution.

## Suggested Fix
Re-resolve delegates from `connectionPool.get().first()` between attempts:

```kotlin
private suspend fun executeWithRetry(data: HttpRequestData, requestedCapability: FHTTPTransportCapability?): HttpResponseData {
    var lastException: Throwable? = null
    repeat(MAX_HTTP_FAILOVER_ATTEMPTS) { attempt ->
        val current = currentDelegates(requestedCapability) // re-reads connectionPool
        for (delegate in current) {
            runSuspendCatching { return delegate.getDeviceHttpEngine().execute(data) }
                .onFailure { lastException = it }
        }
    }
    throw lastException ?: error("No delegates available")
}
```

Also, when `currentDelegates.isEmpty()` initially, `await` on the pool until at least one transport is present, with a configurable timeout, instead of failing fast.
