# `linkBusyBar` / `unlinkBusyBar` status checks are dead code under `expectSuccess = true`

## Severity
medium

## Type
broken-feature

## Files
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/rest/impl/src/commonMain/kotlin/net/flipper/bsb/cloud/rest/api/BusyCloudBarsApiImpl.kt` (lines 39-76)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/rest/impl/src/commonMain/kotlin/net/flipper/bsb/cloud/rest/utils/PrincipalApiKtx.kt` (line 48 — `expectSuccess = true`)

## Summary
`BsbUserPrincipalScopeImpl.addAuth` sets `expectSuccess = true` on every request.
That makes Ktor throw `ResponseException` for any non-2xx response. Yet
`linkBusyBar` and `unlinkBusyBar` then perform redundant manual checks:

```kotlin
// unlink
when (response.status) {
    HttpStatusCode.OK, HttpStatusCode.NoContent -> Unit
    else -> error("Failed to delete bar ${response.bodyAsText()}")
}
// link
check(response.status.isSuccess()) { "Failed link busy bar" }
```

The `else` / `check` branches are unreachable while `expectSuccess = true` holds —
the response would already have thrown a `ResponseException` before the assignment.

This is benign today, but:
- It hides the actual error path. `ResponseException.message` is opaque to callers; if
  someone removes `expectSuccess = true` (or adds a Ktor call that doesn't go through
  `addAuth()`), the manual check would be reached but `error("Failed to delete bar
  ${response.bodyAsText()}")` reads the body **after** Ktor has already consumed it
  for `expectSuccess`, producing an empty string.
- The `error("...")` path masks the actual server error message because
  `bodyAsText()` may have already been read inside Ktor's exception path.

## Repro
- Simulate a 500 response from `/api/v0/bars/{id}` (DELETE).
- Observe: caller receives `Result.failure(ResponseException(...))`, NOT the manual
  `IllegalStateException` from `error(...)`. Caller-side error parsing has no
  visibility into the response body.

## Root Cause
- `expectSuccess = true` was added later (or `addAuth()` factored that in) without
  removing the manual checks, leading to duplicate error paths that don't agree.

## Impact
- Confusing error messages.
- If `expectSuccess` is ever flipped, the manual checks resurface and the body is
  unavailable.
- Slight code-bloat / detekt false-positive opportunity.

## Suggested Fix
Drop the manual checks; rely on `expectSuccess = true`. If a richer error message is
desired, install a custom `HttpResponseValidator` that parses the body before throwing.
