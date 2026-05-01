# `recoverCatching` swallows `CancellationException` in suspend retry path

## Severity
medium

## Type
infrastructure

## Files
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/rest/impl/src/commonMain/kotlin/net/flipper/bsb/cloud/rest/utils/PrincipalApiKtx.kt` (lines 18-37)

## Summary
`BUSYLibUserPrincipal.Token.run` uses Kotlin stdlib `Result.recoverCatching`:

```kotlin
runSuspendCatching {
    with(BsbUserPrincipalScopeImpl(originalToken)) { block() }
}.recoverCatching { error ->
    if (error.isAuthError()) { ... } else { throw error }
}
```

`recoverCatching` is `Result<T>.recoverCatching(transform)` — internally it calls
`runCatching { transform(exceptionOrNull()) }`. That `runCatching` catches **all**
`Throwable`, including `CancellationException` originating from the retry block
(`block()`). The project rule explicitly bans this pattern.

When the calling coroutine is cancelled while the retry path is executing
`block()` again, the cancellation is captured into a `Result.Failure` and surfaced
as a regular failure to the caller — bypassing structured concurrency.

## Repro
1. Call any cloud REST API that triggers a 401 → retry path.
2. Cancel the calling coroutine while the second `block()` is running.
3. The cancellation is wrapped into `Result.failure(CancellationException(...))`
   rather than rethrown, so `getOrThrow()` upstream throws a regular exception, and
   `Job` cancellation is delayed past the suspending boundary.

## Root Cause
Kotlin stdlib's `recoverCatching` is not coroutine-aware. Same root cause as the
broader `runCatching` rule.

## Impact
- Structured cancellation is lost on the retry path of every cloud REST call.
- Callers that expect "cancel propagates immediately" instead see a wrapped failure.
- In high-concurrency cancel-on-leaving-screen patterns this can mask leaks.

## Suggested Fix
Replace with explicit suspend-aware logic:

```kotlin
val first = runSuspendCatching {
    with(BsbUserPrincipalScopeImpl(originalToken)) { block() }
}
when {
    first.isSuccess -> return@transform first
    first.exceptionOrNull()?.isAuthError() == true -> {
        val newToken = getToken(failedToken = originalToken)
        runSuspendCatching {
            with(BsbUserPrincipalScopeImpl(newToken)) { block() }
        }
    }
    else -> first
}
```

`runSuspendCatching` rethrows `CancellationException`.
