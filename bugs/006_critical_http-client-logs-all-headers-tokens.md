# `getHttpClient` enables `LogLevel.ALL` which logs Authorization headers and request bodies

## Type
broken-feature

**Severity:** critical

**Files:**
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktor/src/commonMain/kotlin/net/flipper/core/ktor/HttpClient.kt` lines 59–67
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/core/ktor/src/commonMain/kotlin/net/flipper/core/ktor/util/KtorLoggerKtx.kt` (`minimizeBodyLogMessage` — does not strip headers)

## Summary
`install(Logging) { level = LogLevel.ALL }` causes Ktor to emit every request/response **including all headers and the full body** through the `Logger` callback. The minimizer (`minimizeBodyLogMessage`) only redacts `application/octet-stream` and `image/bmp` bodies — JSON bodies (auth tokens, refresh tokens, OAuth payloads) and ALL headers (`Authorization: Bearer …`, `Cookie:`, `X-Api-Key:`, server-issued `Set-Cookie:`) flow straight to `TaggedLogger("Ktor")`.

```kotlin
install(Logging) {
    logger = object : Logger {
        override fun log(message: String) {
            val mappedMessage = minimizeBodyLogMessage(message)  // does NOT strip headers
            ktorTimber.info { mappedMessage }
        }
    }
    level = LogLevel.ALL                                        // includes headers + body
}
```

The Android `LogPlatform.android.kt` actual delegates `info` to Timber, which is normally wired to logcat / Crashlytics in production builds. Apple targets pipe to `NSLog` via `DefaultAppleLogger`, visible in Console.app and shipped logs.

`BuildKonfig.IS_LOG_ENABLED` does gate the `info` call, but build configurations leaving `IS_LOG_ENABLED = true` in release builds (the default for many KMP buildkonfig setups) will emit every secret to the platform log sink.

## Repro
1. In a release build of the consumer app with `IS_LOG_ENABLED = true`, perform a login request.
2. Inspect logcat / Console.app:
   ```
   I/Ktor: REQUEST: /api/login
   I/Ktor: HEADERS
   I/Ktor: -> Authorization: Bearer eyJhbGciOi…
   I/Ktor: BODY START
   I/Ktor: {"refreshToken":"…"}
   I/Ktor: BODY END
   ```

## Root Cause
- `LogLevel.ALL` is the most verbose Ktor level.
- The custom `Logger` does not implement any header redaction.
- `minimizeBodyLogMessage` only handles binary content types.
- No filtering is performed for `Authorization`, `Cookie`, `Set-Cookie`, `Proxy-Authorization`, or response bodies that may contain tokens.

## Impact
- Token / cookie / API-key leakage to platform-wide log sinks (logcat, Console, shipped CrashKit / log-archive features).
- Cloud transport authentication tokens for the BUSY backend will end up in user-visible logs.
- Credentials persist in log archives long after the request completes.

## Suggested Fix
1. Drop to `LogLevel.HEADERS` (or `LogLevel.INFO`) for non-debug builds, and use `LogLevel.ALL` only when `BuildKonfig.IS_VERBOSE_LOG_ENABLED == true`.
2. Add header redaction inside the custom `Logger` (or use the new `sanitizeHeader`/`filter` API in Ktor's Logging plugin):
   ```kotlin
   sanitizeHeader { it.equals("Authorization", true) || it.equals("Cookie", true) }
   ```
3. Extend `minimizeBodyLogMessage` to additionally redact bodies on URLs known to contain credentials (`/login`, `/refresh`, `/oauth/*`).
