# Authenticated WebSocket can be reused across user-switch within the 30-second `WhileSubscribed` grace window

## Severity
high

## Type
broken-feature

## Note on intended behavior
Per the project owner, **the 30-second `WhileSubscribed(stopTimeout = 30.seconds)` grace window is intentional** (avoids tearing down + recreating the WS on transient resubscribes). This bug is therefore **not** about the grace window itself — it is about the fact that during that 30 s window the cached `BSBWebSocket` is authenticated to whichever user logged in earlier, and is reused for the next principal without re-handshake.

## Files
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/barsws/impl/src/commonMain/kotlin/net/flipper/bsb/cloud/barsws/api/utils/CloudWebSocketApiImpl.kt` (lines 52-83)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/barsws/impl/src/commonMain/kotlin/net/flipper/bsb/cloud/barsws/api/utils/BSBWebSocket.kt` (lines 80-112)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/barsws/impl/src/commonMain/kotlin/net/flipper/bsb/cloud/barsws/api/orchestrator/CloudWebSocketOrchestratorApiImpl.kt` (lines 98-124)

## Summary
`wsStateFlow` is built with `shareIn(scope, SharingStarted.WhileSubscribed(stopTimeout = 30.seconds), replay = 1)`. The grace window is intentional. The bug is that the `BSBWebSocket` instance held in the `replay = 1` cache is bound to a specific principal's auth ticket, and there is no invalidation when the principal changes. If the user logs out and a different user logs in within 30 s (or the same user re-logs after a token rotation), the orchestrator can reuse the previously-authenticated socket — sending subscribe/unsubscribe frames for user B's bars over a session authenticated as user A.

## Repro
1. Log in user A, subscribe to `getEventsFlow(barIdA)`. WS is live, authenticated as A.
2. Log out → `principalApi` → `Empty`. The shared upstream's last subscriber leaves; the cached WS is held for 30 s by `WhileSubscribed`.
3. Within 30 s, log in as user B → `principalApi` → `Token(B)`.
4. `combine(...)` re-evaluates. The shared upstream is still alive (still inside the 30 s grace), so the new subscriber receives the **cached user-A `BSBWebSocket`** from `replay = 1` before a new socket is established.
5. The orchestrator issues subscribe RPCs for user B's bars over user A's session.

## Root Cause
- `wsStateFlow` is keyed on subscriber presence, not on the principal identity.
- `BSBWebSocket` has no public `close()` method, so the orchestrator cannot force teardown when the principal flips.
- There is no `distinctUntilChangedBy { principalIdOf(it) }` upstream of the shared flow that would discard the cached socket when the user changes.

## Impact
- Cross-user session reuse during a fast logout→login. Server-side billing / activity attribution remains under user A. Auth boundary is conceptually broken even if backend authorization checks happen to catch most calls.
- Compounds with the recent `Remove cloud-linked devices when user logs out` work: the device list is purged client-side, but the session streaming their state is not.

## Suggested Fix
- Keep the 30 s grace window for the *transport* but invalidate the cached value when the principal changes:
  - Wrap the cached `BSBWebSocket` with the principal that authenticated it, then in the consumer-side `flatMapLatest`/`map`, drop and rebuild whenever the current principal differs from the one cached.
  - Or expose `BSBWebSocket.close()` and have a side-effect collector on `principalApi.getPrincipalFlow()` that calls `close()` whenever the principal changes (`Empty` → `Token` or `Token(A)` → `Token(B)`), forcing the upstream to reproduce a fresh socket on next subscribe.
- Either way, the 30 s `stopTimeout` itself stays as designed — only the *identity* of the cached socket needs guarding.
