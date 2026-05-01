# high — `BUSYCloudHttpEngine` only refreshes token on 403, never on 401, and `ProxyTokenProvider` keeps cached token

## Severity
high

## Type
infrastructure

## Files
- `components/bridge/transport/tcp/cloud/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/engine/BUSYCloudHttpEngine.kt:42-56`
- `components/bridge/transport/tcp/cloud/impl/src/commonMain/kotlin/net/flipper/bridge/connection/transport/tcp/lan/impl/engine/token/ProxyTokenProvider.kt:30-64`

## Summary
The cloud engine retries token refresh only on `HttpStatusCode.Forbidden` (403). RFC 7235 says expired/invalid bearer tokens return **401 Unauthorized**. Many gateways (including AWS API Gateway and many Spring-based stacks) return 401 in this case. Result: a stale cached token returns 401 forever, no refresh ever happens, and the request fails permanently.

Additionally, `ProxyTokenProvider.shouldUpdateToken()` only checks the cached `tokenExpireTime`. If the server invalidates the token early (revoked, rotated, principal changed), the cache will keep serving the dead token until either:
- `tokenExpireTime` is reached, OR
- Caller passes `failedToken == cachedToken` (only happens via the 403 path above).

So a 401 + cache-still-fresh scenario becomes a permanent silent failure.

Secondary issues in the same code path:
- `getToken(failedToken: String? = null)` accepts the failed token to force a refresh, but only forces if `failedToken == cachedToken`. If two concurrent callers race, caller A gets the new token, caller B's `failedToken` no longer equals the now-fresh `cachedToken`, so caller B is rebuffed and may re-issue the request with a different token than expected — harmless here but fragile.
- `cachedToken ?: error("Token should not be null")` — under cancellation `generateToken()` could be cancelled mid-write leaving `cachedToken` non-null but stale, or null but `tokenExpireTime` set. Untested invariants.

## Repro
1. Deploy the BUSY cloud HTTP API behind a gateway that returns 401 (not 403) for expired tokens.
2. Wait until the token expires server-side BEFORE `tokenExpireTime` triggers locally (clock skew, server-side revocation).
3. Issue a request → 401 → `result.statusCode == 401`, the `if (result.statusCode == HttpStatusCode.Forbidden)` is false, the engine returns 401 to the caller.
4. Every subsequent request hits the same cached token → permanent 401.

## Root Cause
The 403/401 confusion is a common bug. The token provider also misses an "invalidate cache" entry point.

## Impact
- "Stuck-on-bad-token" mode requires user to log out / log back in to reach the `BUSYLibUserPrincipal` flow change.
- Cloud transport effectively bricked for affected users.

## Suggested Fix
```kotlin
val isAuthFail = result.statusCode == HttpStatusCode.Forbidden ||
                 result.statusCode == HttpStatusCode.Unauthorized
if (isAuthFail) {
    info { "Auth failure ${result.statusCode}, refreshing token" }
    result = makeRequest(data, tokenProvider.getToken(failedToken = token))
}
```

Also expose `ProxyTokenProvider.invalidate()` for upper layers to call when they detect a forced re-auth.
