# high — Cloud engine serializes ALL requests behind a global `Mutex` — head-of-line blocking + cancellation hazard

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/transport/tcp/cloud/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/engine/BUSYCloudHttpEngine.kt:38,42-56`

## Summary
```kotlin
private val mutex = Mutex() // This is workaround for bug from server side
…
override suspend fun execute(data: HttpRequestData): HttpResponseData {
    return mutex.withLock {
        rwMutex.withReadLock {
            …
            var result = makeRequest(data, token)
            if (result.statusCode == HttpStatusCode.Forbidden) {
                result = makeRequest(data, tokenProvider.getToken(token))
            }
            result
        }
    }
}
```

Every cloud HTTP request is funnelled through a single `Mutex`, meaning **only one cloud request can be in flight at any moment globally per engine instance**. Two effects:

1. **Throughput tanks** — a slow streaming response (the typical case for BUSY device control) blocks every other request including unrelated short status pings, until completion. Latency-sensitive UI calls (e.g. "list capabilities") sit behind a long upload.
2. **Cancellation isn't safe** — `Mutex.withLock` correctly releases on cancellation, BUT `delegate.execute(...)` is the long-running suspend; if it is non-cooperative (engine internals don't observe cancellation), the mutex is held indefinitely while caller's coroutine is in `Cancelling` state. Real Ktor engines do observe cancellation, so this is mostly OK, but it's worth noting that any non-cancellable native engine would deadlock the queue.

The `// This is workaround for bug from server side` comment provides no detail — but a global serialization is a sledgehammer; if the server bug is "auth refresh races", the right fix is to serialize ONLY the token-refresh path, not all requests.

## Repro
1. Start a long upload via the cloud transport.
2. Concurrently issue any other request.
3. Second request blocks until upload finishes.

## Root Cause
Author serialized too aggressively to work around an unrelated server-side bug, and never went back to narrow the scope.

## Impact
- Severe latency multiplier under realistic mixed-traffic workloads.
- Hides any concurrency bugs in upper layers (because nothing is concurrent).
- LAN engine (`BUSYBarHttpEngine`) does NOT have the same `Mutex`, so the inconsistency between transports is also surprising.

## Suggested Fix
1. Remove the outer `mutex.withLock { ... }`; keep only the `rwMutex.withReadLock` (which is actually a reader, allowing many in flight).
2. Move the token-refresh-on-403 step to use a per-token guard if the server issue is "concurrent refresh" — e.g. inside `ProxyTokenProvider.getToken(failedToken)` already serialized by `mutex.withLock`.
3. Document the original server bug in code so future maintainers understand whether the workaround is still needed.
