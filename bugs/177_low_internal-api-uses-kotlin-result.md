# Internal firmware APIs use `kotlin.Result<T>` rather than `runSuspendCatching`-friendly types

## Type
infrastructure

**Severity:** low

**Files:**
- `components/bridge/device/firmware-update/impl/src/commonMain/kotlin/net/flipper/bridge/device/firmwareupdate/uploader/api/FirmwareUploaderApi.kt`
- `components/bridge/device/firmware-update/impl/src/commonMain/kotlin/net/flipper/bridge/device/firmwareupdate/downloader/api/FirmwareDownloaderApi.kt`

## Summary

The `internal` interfaces `FirmwareUploaderApi.uploadAndInstall` and `FirmwareDownloaderApi.download`
return `kotlin.Result<T>` from suspending functions. While these are not exposed across the Swift boundary
(they are internal and therefore exempt from the `CResult<T>` rule in `AGENTS.md`), the use of `kotlin.Result`
inside `suspend` code is itself a footgun:

- `Result` swallows `CancellationException` if the producer uses `runCatching`. The implementations here use
  `runSuspendCatching`, which is correct, but anyone refactoring later may be tempted by the symmetric-looking
  Java-style `Result` and reintroduce a `runCatching` (which `AGENTS.md` already bans).
- The wrapping `Result` adds nothing — the only consumer (`FirmwareUpdaterApiImpl`) immediately calls
  `.getOrThrow()` or `.onFailure { ... }` and doesn't propagate the `Result` further.

## Suggested fix

- Change the return types to `T` and let exceptions propagate (with `runSuspendCatching` at the orchestration
  layer in `FirmwareUpdaterApiImpl`), or
- Use the project's `CResult<T>` even internally for consistency with the public API.

This is low-severity because it's not a bug today — it's a documented invitation for future bugs.
