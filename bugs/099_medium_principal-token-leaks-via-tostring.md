# `BUSYLibUserPrincipalToken` and friends do not redact secrets in `toString` / logs

## Type
broken-feature

**Severity:** medium

**Files:**
- `components/principal/api/src/commonMain/kotlin/net/flipper/bsb/auth/principal/api/BUSYLibUserPrincipalToken.kt`
- `components/principal/api/src/commonMain/kotlin/net/flipper/bsb/auth/principal/api/BUSYLibUserPrincipal.kt`

## Summary

`BUSYLibUserPrincipalToken` is a regular class holding a `tokenProvider: suspend (failedToken: String?) -> String`
lambda. The class does **not** override `toString`. If any caller does `info { "principal=$principal" }` (a
common thing to do during debugging), Kotlin's default `toString` prints the lambda's identity — not the
token, but **the failed token argument is already a String** that the caller is expected to log somewhere.

More directly: the public API of `BUSYLibUserPrincipal.Token` exposes `suspend fun getToken(failedToken:
String?): String`. The token return value is a raw `String` and shows up as a return value in any logger that
auto-logs return values (Kotlin's coroutine debugger, MockK verification logs, etc.). There is no
`@SensitiveString` marker, no `Redacted<String>` wrapper, and no logging guard.

`AGENTS.md` explicitly mentions a `sensitive { }` log facility that is gated by `BuildKonfig.IS_SENSITIVE_LOG_ENABLED`,
suggesting this project is aware of secret-in-log hazards. The principal API does not use any of that.

## Repro

1. Audit the codebase for `info { "$principal" }` or `info { "$token" }`.
2. Or: turn on the Kotlin coroutine debug agent — it logs return values on suspending functions, including
   the token returned by `getToken`.

## Root cause

Tokens are exposed as raw `String`s with no opaque-wrapper type. `tokenProvider` is invoked at the boundary
where the value will be plugged into HTTP `Authorization` headers, so it has to be a `String` somewhere — but
that does not mean every call site needs to see the raw form.

## Impact

- Risk of token / refresh-token leakage in production logs and crash reports if a caller does the obvious-
  looking thing.
- Violates the spirit of the `sensitive { }` facility provided elsewhere in the project.

## Suggested fix

- Wrap the token in an opaque value class with a redacted `toString`:

  ```kotlin
  @JvmInline
  value class BearerToken(val raw: String) {
      override fun toString() = "BearerToken(***)"
  }
  ```
- Have `BUSYLibUserPrincipal.Token.getToken` return `BearerToken` instead of `String`.
- Make `BUSYLibUserPrincipalToken` override `toString` and `equals`/`hashCode` to never expose the lambda or
  the user id (or at least not in a way that `info { "$it" }` will print secrets).
