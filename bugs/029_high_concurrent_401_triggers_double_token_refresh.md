# Two concurrent 401s call `getToken(failedToken)` twice — no de-dup on refresh

## Severity
high

## Type
infrastructure

## Files
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/rest/impl/src/commonMain/kotlin/net/flipper/bsb/cloud/rest/utils/PrincipalApiKtx.kt` (lines 14-44)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/principal/api/src/commonMain/kotlin/net/flipper/bsb/auth/principal/api/BUSYLibUserPrincipalToken.kt`

## Summary
`BUSYLibUserPrincipal.Token.run(...)` is the single retry-on-401 helper used by every
cloud REST endpoint:

```kotlin
runSuspendCatching {
    with(BsbUserPrincipalScopeImpl(originalToken)) { block() }
}.recoverCatching { error ->
    if (error.isAuthError()) {
        val newToken = getToken(failedToken = originalToken)
        with(BsbUserPrincipalScopeImpl(newToken)) { block() }
    } else { throw error }
}
```

If two REST calls run concurrently with the same expired token and both receive
HTTP 401, **both** invoke `getToken(failedToken = originalToken)`. The library makes
no attempt to coalesce these refreshes.

The interface contract (`BUSYLibUserPrincipal.Token.getToken`) does not require the
consumer to dedupe, and the contract docs are silent. A naïve consumer impl (e.g.
`SampleAuthClient`-style using `httpClient.post` to fetch a new token) will issue two
parallel refresh requests against the same identity provider, possibly invalidating
each other depending on backend implementation.

Worse: if `getToken(failedToken = originalToken)` succeeds on the first call but
returns the SAME token (because the refresh wasn't actually triggered or the IdP
didn't rotate), the recovery path retries with the same expired token. The second
call's recovery may succeed or also fail — but the library would then retry yet
another call against the still-failing endpoint, with no maximum retry bound.

## Repro
1. Wait for the cached access token in the consumer's `tokenProvider` to expire.
2. Issue two parallel REST calls (`getBarsList`, `getTicketToken`) on shared dispatcher.
3. Both 401, both call `getToken(failedToken = T0)`.
4. Observe two refresh round-trips against the IdP for one logical refresh.

## Root Cause
- No `Mutex` or `Deferred` cache around `getToken`.
- No retry/refresh contract in `BUSYLibUserPrincipal.Token`.
- The recovery path also doesn't bound retries; if `getToken(failedToken)` returns the
  same expired token, recovery will retry once and surface the auth error to the
  caller — but logs and metrics will be noisy.

## Impact
- Burst of refresh traffic on token expiry — costs IdP rate limit, may trigger anti-
  abuse throttling.
- If IdP refresh tokens are single-use, the second refresh fails, leaving the user
  stuck in a re-login loop until manual intervention.
- Tokens may be stomped between concurrent refreshes (write-write races on consumer-
  side caches).

## Suggested Fix
- Add a per-principal `Mutex` (or a coalescing `Deferred<String>`) inside
  `BsbUserPrincipalScopeImpl` (or wrap `getToken` at the cloud-rest layer) so that
  while a refresh is in flight, all concurrent waiters share the same `Deferred`.
- Document this expectation on `BUSYLibUserPrincipal.Token.getToken`.
- Bound the retry to ONE refresh per call site; if the second attempt also 401s,
  surface the auth error rather than silently looping.
