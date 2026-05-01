# Cloud REST `:api` interfaces return `Result<T>`, not `CResult<T>` (Swift interop break)

## Severity
high

## Type
infrastructure

## Files
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/rest/api/src/commonMain/kotlin/net/flipper/bsb/cloud/rest/api/BusyCloudAccessTokenApi.kt`
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/rest/api/src/commonMain/kotlin/net/flipper/bsb/cloud/rest/api/BusyCloudBarsApi.kt` (3 methods)
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/rest/api/src/commonMain/kotlin/net/flipper/bsb/cloud/rest/api/BusyCloudWebSocketTicketApi.kt`
- `/Users/lionzxy/flipper/busylib-kmp-fork/components/cloud/rest/api/src/commonMain/kotlin/net/flipper/bsb/cloud/rest/api/BusyFirmwareDirectoryApi.kt`

## Summary
Every cloud REST `:api` method declares `suspend fun ...: Result<T>` — Kotlin's stdlib
`Result`. Per `AGENTS.md` hard rule:
> Return `CResult<T>` from `suspend` functions, never Kotlin's inline `Result<T>`
> (it does not cross the Swift boundary).

`Result` is an inline value class; SKIE/Swift-interop cannot project it. Swift consumers
of the XCFramework cannot call these methods or cannot unwrap them safely.

## Repro
- Invoke any of these from Swift (e.g. via the published XCFramework). The function
  signature surfaces an opaque type, success/failure cannot be discriminated, and the
  function is effectively unusable from Swift without manual bridging.

## Root Cause
- These interfaces predate the project rule (or the rule was added after these
  modules), and `CResult<T>` was never substituted.

## Impact
- Public Swift consumers cannot use the cloud REST APIs through the XCFramework.
- These interfaces are also not exposed via `WrappedFlow`/CResult, so any change to
  these signatures is binary-/source-breaking.

## Suggested Fix
- Replace `Result<T>` with `CResult<T>` (see other recent commits like
  `Refactor BLE central manager initialization to use Provider pattern` and the
  `CResult` usage in `:bridge` modules for the standard pattern).
- Update the `:impl` modules to wrap `runSuspendCatching { ... }.toCResult()` (or
  whatever the project's adapter is).
- Ensure `:rest:api` is exported from `entrypoint/build.gradle.kts` `XCFramework("BusyLibKMP")`
  so the new `CResult` wrappers are visible from Swift.
