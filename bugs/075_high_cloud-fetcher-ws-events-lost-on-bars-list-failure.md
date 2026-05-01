# `CloudFetcher.getBarsFlow` silently drops all WebSocket events when initial REST list fetch fails

## Type
broken-feature

**Severity:** high

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/watchers/provisioning/src/commonMain/kotlin/net/flipper/bsb/watchers/provisioning/utils/CloudFetcher.kt` lines 58-89

## Summary

The inner `getBarsFlow(principal, webSocketFlow)` builder fetches the initial
bars list from the REST API and `return@flow` on any failure:

```kotlin
private fun getBarsFlow(
    principal: BUSYLibUserPrincipal.Token,
    webSocketFlow: Flow<WebSocketEvent>
): Flow<List<BusyCloudBar>> = flow {
    val bars = busyCloudBarsApi.getBarsList(principal).onFailure {
        error(it) { "Failed to get bars list" }
    }.getOrNull() ?: return@flow
    ...
    webSocketFlow.collect { ... emit(barsList) }
}
```

When the REST call fails (server 5xx, transient TLS error, token still warm),
the flow completes immediately *without ever subscribing* to the WebSocket
event stream. There is no retry loop and no fallback. From `CloudFetcherWatcher`'s
point of view, the user's cloud devices are now permanently un-syncable on the
current `(principal, network)` tuple — the only way to recover is to re-emit
on `principalApi.getPrincipalFlow()` or `networkStateApi.isNetworkAvailableFlow`,
which doesn't happen unless the user logs out and back in or toggles wifi.

Because the upstream `flatMapLatest { ws -> ... }` only re-runs when the WS
flow re-emits a non-null instance, and `wsApi.getWSFlow()` is upstream of a
1-second `debounce`, no organic retry occurs after this initial failure either.

## Repro

1. User logs in. `principalApi.getPrincipalFlow()` emits `Token(...)`.
2. Network is online. WebSocket connects. After 1s debounce, the inner
   `getBarsFlow` collector starts.
3. `busyCloudBarsApi.getBarsList(principal)` fails (HTTP 502 from the cloud
   gateway, or backend cold-start).
4. Flow returns. `CloudFetcherWatcher.collectLatest` never re-collects because
   the upstream did not re-emit.
5. The user links a new device on the web UI. The WebSocket event arrives,
   but `getBarsFlow` is no longer subscribed. Local storage never learns about
   the new device.
6. Until the principal flow or network flow re-emits (re-login or
   wifi toggle), the user's local device list stays out of sync indefinitely.

## Root Cause

`CloudFetcher` treats the REST seed as a hard prerequisite for the WebSocket
loop. It should either (a) retry the seed with `exponentialRetry { ... }`
(the project-standard retry helper, see AGENTS.md), or (b) start with an
empty seed and let the WS events backfill state.

The AGENTS.md guideline "Retries: use `exponentialRetry { ... }` from
`core:ktx` rather than hand-rolling loops" is violated here — the function
hand-rolls a single attempt with no retry.

## Impact

- New cloud-side device additions and renames are not reflected locally
  whenever the initial REST seed fails.
- Users lose visibility into freshly provisioned devices until they manually
  reset the auth or network state.

## Suggested Fix

1. Wrap the REST seed in `exponentialRetry { busyCloudBarsApi.getBarsList(principal) }` and only `return@flow` after exceeding a max attempt budget.
2. Consider downgrading to: emit an empty list, then enter the WS collect loop
   so subsequent events can build state even without a seed.
3. Add tests covering: REST seed failure → eventual recovery; REST seed
   failure → WS LinkEvent should still surface a device.
